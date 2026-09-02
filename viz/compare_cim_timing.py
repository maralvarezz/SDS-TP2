#!/usr/bin/env python3
"""
Compara los tiempos de ejecucion del CIM entre TP1 y TP2 (punto g del enunciado de TP2),
en escala log-log y a densidad fija (para que la pendiente sea comparable con el orden de
complejidad esperado del CIM).

Toma uno o mas *_runs.csv generados por viz/time_analysis.py de TP1 (columnas series, n,
elapsed_ns/elapsed_ms entre otras) y el output/cim_timing_tp2.csv que genera CimTimingMain de TP2
(columnas n, run, l, m, elapsed_ns), agrega por N y grafica ambas curvas juntas en escala log-log,
anotando en la leyenda la pendiente del ajuste de potencia (regresion lineal en log-log) de cada una.

Agregacion (--aggregate):
  mean (default historico): promedio + desvio estandar. Sensible a outliers.
  median: mediana + MAD escalado (median absolute deviation * 1.4826, comparable a un desvio
  estandar bajo normalidad). Robusto MIENTRAS la mayoria de las corridas esten limpias.
  besthalf (recomendado para el informe): ordena las corridas de cada N de menor a mayor tiempo,
  se queda solo con la mitad mas rapida, y calcula promedio + desvio de esa mitad. Se llego a esta
  opcion porque los datos reales de TP1 muestran algo mas grave que "la primera corrida sale mal
  por arranque de JVM": en varios N (chequeado con los CSV de N=500 a 1000) HASTA LA MITAD de las
  10 corridas salen contaminadas por pausas de GC o ruido del SO, dispersas en cualquier posicion,
  no solo la primera. Con esa fraccion de contaminacion la mediana queda al borde entre el cluster
  "limpio" y el "contaminado" y puede caer para cualquier lado segun el N (por eso el zigzag que se
  ve en el grafico con --aggregate median). Quedarse con la mitad mas rapida asume que el ruido
  SIEMPRE suma tiempo (nunca lo resta) -- por eso la mitad de corridas mas rapidas es la mejor
  estimacion disponible del tiempo "real" del algoritmo, sin descartar a mano cuantas corridas
  contaminadas hay en cada N (que varia). Es la misma logica que usan herramientas de benchmarking
  como hyperfine o JMH al reportar el mejor tiempo en vez del promedio crudo.

Descarte de corridas de arranque (--discard-first): en vez de (o ademas de) usar mediana, permite
descartar las primeras K corridas de cada N en el orden en que aparecen en el CSV (que es el orden
en que se ejecutaron). Se aplica por igual a TP1 y TP2 -- aunque TP2 no tiene el problema (un solo
proceso continuo, calentado una unica vez antes de medir), descartar del mismo modo en ambas series
evita que la comparacion se vea como "recorte a medida" de una sola serie.

Filtro de N minimo (--n-min): a N chico (<=200) el CIM tarda fracciones de milisegundo, asi que la
comparacion queda dominada por ruido de arranque de proceso/temporizador del SO en vez de por el
algoritmo en si -- ahi TP1 (JVM nueva por N) y TP2 (un solo proceso ya caliente) no estan midiendo
lo mismo, sea cual sea la agregacion usada. --n-min descarta esos puntos chicos de la comparacion
(aplica a TP1 y TP2 por igual).

Como el rango de N usado (10 a 5000) hace que la serie "free" de --compare-density sea inviable
(con L fijo, en N grandes la densidad de particulas con radio es geometricamente imposible de
empaquetar), el flujo recomendado es correr TP1 una vez POR CADA N con el L y M ya escalados a
densidad fija (sin --compare-density), lo que genera un _runs.csv por N. Este script acepta varios
archivos a la vez y los une antes de agregar.

Uso:
    python3 viz/compare_cim_timing.py \
        --tp1-runs output/cim_timing_n*_runs.csv \
        --tp2-runs output/cim_timing_tp2.csv \
        --aggregate median \
        --out output/figures/cim_timing_comparison_loglog.png

    (dejar que la shell expanda el glob, o listar los paths a mano separados por espacio)
"""
import argparse
import csv
import statistics
import sys
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

MAD_TO_STD = 1.4826  # factor que hace que MAD sea comparable a un desvio estandar bajo normalidad


def aggregate(paths, n_key, ms_from_row, series_key=None, series_value=None, aggregate_mode="mean",
              discard_first=0, n_min=None, near_min_factor=1.5):
    grouped = defaultdict(list)
    for path in paths:
        with open(path, newline="") as f:
            reader = csv.DictReader(f)
            for row in reader:
                if series_key is not None and series_value is not None:
                    if row.get(series_key, "standard") != series_value:
                        continue
                n = int(row[n_key])
                if n_min is not None and n < n_min:
                    continue
                grouped[n].append(ms_from_row(row))

    ns = sorted(grouped)
    if discard_first:
        for n in ns:
            if len(grouped[n]) > discard_first:
                grouped[n] = grouped[n][discard_first:]

    centers, spreads = [], []
    for n in ns:
        c, s = _reduce(grouped[n], aggregate_mode, near_min_factor)
        centers.append(c)
        spreads.append(s)
    return ns, centers, spreads


def _reduce(values, aggregate_mode, near_min_factor=1.5):
    """Centro + dispersion de un solo grupo de corridas, segun --aggregate."""
    if aggregate_mode == "nearmin":
        ordered = sorted(values)
        floor = ordered[0]
        kept = [v for v in ordered if v <= floor * near_min_factor]
        return statistics.fmean(kept), (statistics.stdev(kept) if len(kept) > 1 else 0.0)
    if aggregate_mode == "besthalf":
        ordered = sorted(values)
        kept = ordered[: max(1, -(-len(ordered) // 2))]  # ceil(len/2) mas rapidas
        return statistics.fmean(kept), (statistics.stdev(kept) if len(kept) > 1 else 0.0)
    if aggregate_mode == "median":
        center = statistics.median(values)
        mad = statistics.median(abs(v - center) for v in values) if len(values) > 1 else 0.0
        return center, mad * MAD_TO_STD
    return statistics.fmean(values), (statistics.stdev(values) if len(values) > 1 else 0.0)


def power_law_slope(ns, centers):
    """Pendiente de la recta que mejor ajusta log10(centro) vs log10(n) (regresion lineal)."""
    log_n = np.log10(ns)
    log_center = np.log10(centers)
    slope, _ = np.polyfit(log_n, log_center, 1)
    return slope


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--tp1-runs", type=Path, required=True, nargs="+",
                         help="uno o mas CSV *_runs.csv generados por viz/time_analysis.py de TP1")
    parser.add_argument("--tp2-runs", type=Path, default=None,
                         help="CSV generado por CimTimingMain de TP2 (output/cim_timing_tp2.csv), "
                              "una sola curva de TP2 agnostica al modelo. No hace falta si se pasa "
                              "--tp2-by-model.")
    parser.add_argument("--tp2-by-model", type=Path, default=None,
                         help="CSV generado por CimTimingByModelMain de TP2 "
                              "(output/cim_timing_tp2_by_model.csv, columnas model,rho,n,run,l,m,"
                              "elapsed_ns) -- grafica DOS curvas de TP2 (una por modelo) en vez de "
                              "una sola. Reemplaza a --tp2-runs si se pasan los dos.")
    parser.add_argument("--tp1-series", default="standard",
                         help="Serie a usar de los CSV de TP1 (default: standard; el modo nuevo de "
                              "TP1 --experiment-* no escribe columna series, en ese caso se ignora "
                              "sola porque no hace falta filtrar)")
    parser.add_argument("--aggregate", choices=["mean", "median", "besthalf", "nearmin"], default="mean",
                         help="mean (default, compatibilidad): promedio + desvio estandar. "
                              "median: mediana + MAD escalado. besthalf: promedio + desvio de la "
                              "mitad de corridas mas rapidas por N (asume que la contaminacion es "
                              "<=50%% de las corridas). nearmin (recomendado): se queda con TODAS "
                              "las corridas que estan a lo sumo --near-min-factor veces la corrida "
                              "mas rapida de ese N (no un cupo fijo), y promedia esas -- se adapta "
                              "sola a cuantas corridas salieron contaminadas en cada N en vez de "
                              "asumir siempre 50%%, que en algunos N (ej. N=900 en los datos reales) "
                              "no alcanzaba.")
    parser.add_argument("--near-min-factor", type=float, default=1.5,
                         help="con --aggregate nearmin, factor multiplicativo sobre la corrida mas "
                              "rapida de cada N para decidir que otras corridas se consideran "
                              "'limpias' (default 1.5x el minimo). Bajarlo filtra mas agresivo.")
    parser.add_argument("--discard-first", type=int, default=0,
                         help="descarta las primeras K corridas de cada N (en orden de ejecucion), "
                              "aplicado por igual a TP1 y TP2. Por default no descarta nada.")
    parser.add_argument("--n-min", type=int, default=None,
                         help="descarta del grafico y de la pendiente todo N menor a este valor "
                              "(aplicado a TP1 y TP2 por igual). Ej. --n-min=500 se queda solo con "
                              "N=500,1000,2000,5000 si esos son los N corridos.")
    parser.add_argument("--linear", action="store_true",
                         help="grafica en escala lineal (N y tiempo) en vez de log-log. El CIM a "
                              "densidad fija es O(N) esperado, asi que en escala lineal tambien da "
                              "una recta -- no hace falta log-log para verlo, y se evitan los "
                              "problemas de cuantos puntos entran bien en una decada.")
    parser.add_argument("--out", type=Path, default=None, help="si se pasa, guarda la figura en vez de mostrarla")
    args = parser.parse_args()

    if not args.tp2_runs and not args.tp2_by_model:
        sys.exit("Hace falta --tp2-runs o --tp2-by-model")
    tp2_path = args.tp2_by_model if args.tp2_by_model else args.tp2_runs
    for path in (*args.tp1_runs, tp2_path):
        if not path.exists():
            sys.exit(f"No existe el archivo: {path}")

    def ms_from_row(row):
        # Formato nuevo (CimTimingMain, un solo proceso): trae elapsed_ns, no elapsed_ms.
        # Formato viejo (Main --experiment-enabled, una JVM por N): trae elapsed_ms directo.
        if "elapsed_ns" in row and row["elapsed_ns"] not in (None, ""):
            return float(row["elapsed_ns"]) / 1_000_000
        return float(row["elapsed_ms"])

    n1, center1, spread1 = aggregate(
        args.tp1_runs, "n", ms_from_row,
        series_key="series", series_value=args.tp1_series,
        aggregate_mode=args.aggregate, discard_first=args.discard_first, n_min=args.n_min,
        near_min_factor=args.near_min_factor,
    )
    if not n1:
        sys.exit(
            f"Los CSV de TP1 no tienen filas con series={args.tp1_series!r} y N>={args.n_min}. "
            "Si corriste TP1 con --compare-density, pasa --tp1-series fixed_density; si el problema "
            "es --n-min, bajalo o sacalo."
        )
    center_label = {"median": "mediana", "besthalf": "promedio (mitad rapida)",
                     "nearmin": "promedio (cerca del minimo)"}.get(args.aggregate, "promedio")

    fig, ax = plt.subplots(figsize=(7.5, 5))
    slope1 = power_law_slope(n1, center1)
    # Con --tp2-by-model, L y M quedan fijos mientras la densidad SI cambia entre los N (son
    # rho=2,4,8 a L=10, no un barrido a densidad fija) -- ahi el CIM deja de ser O(N) puro (mas
    # particulas por celda = mas comparaciones por celda, tiende a O(N^2) con M fijo), asi que
    # mostrar "pendiente" invita a compararla con el O(N) esperado cuando no corresponde en este
    # diseño. Se omite en ese modo (tampoco la mostraba el grafico de referencia con el que se
    # comparo esto).
    show_slope = not args.tp2_by_model and not args.linear
    label1 = f"TP1 (CIM standalone), pendiente≈{slope1:.2f}" if show_slope else "TP1 (CIM standalone)"
    ax.errorbar(n1, center1, yerr=spread1, marker="o", linestyle="--", label=label1,
                color="black", capsize=3, linewidth=1.5)

    if args.tp2_by_model:
        # dos curvas de TP2, una por modelo (mismas rho=2,4,8 a L=10 que TP1 aca arriba).
        by_model = defaultdict(lambda: defaultdict(list))
        with open(args.tp2_by_model, newline="") as f:
            for row in csv.DictReader(f):
                n = int(row["n"])
                if args.n_min is not None and n < args.n_min:
                    continue
                by_model[row["model"]][n].append(float(row["elapsed_ns"]) / 1_000_000)

        colors = {"VICSEK": "tab:blue", "VOTER": "tab:orange"}
        markers = {"VICSEK": "s", "VOTER": "^"}
        labels_es = {"VICSEK": "vicsek", "VOTER": "votante"}
        for model in sorted(by_model):
            grouped = by_model[model]
            ns = sorted(grouped)
            if args.discard_first:
                for n in ns:
                    if len(grouped[n]) > args.discard_first:
                        grouped[n] = grouped[n][args.discard_first:]
            centers, spreads = [], []
            for n in ns:
                values = grouped[n]
                c, s = _reduce(values, args.aggregate, args.near_min_factor)
                centers.append(c)
                spreads.append(s)
            label = f"TP2 {labels_es.get(model, model)}"
            ax.errorbar(ns, centers, yerr=spreads, marker=markers.get(model, "o"), label=label,
                        color=colors.get(model, "tab:green"), capsize=3, linewidth=1.5)
    else:
        n2, center2, spread2 = aggregate(
            [args.tp2_runs], "n", lambda row: float(row["elapsed_ns"]) / 1_000_000,
            aggregate_mode=args.aggregate, discard_first=args.discard_first, n_min=args.n_min,
            near_min_factor=args.near_min_factor,
        )
        slope2 = power_law_slope(n2, center2)
        label2 = "TP2 (CIM dentro de SimulationEngine)" if args.linear else f"TP2 (CIM dentro de SimulationEngine), pendiente≈{slope2:.2f}"
        ax.errorbar(n2, center2, yerr=spread2, marker="s", label=label2,
                    color="tab:orange", capsize=3, linewidth=1.5)

    if args.linear:
        ax.set_xlabel("N")
        ax.set_ylabel(f"Tiempo CIM {center_label} (ms)")
    else:
        ax.set_xscale("log")
        ax.set_yscale("log")
        ax.set_xlabel("N (escala log)")
        ax.set_ylabel(f"Tiempo CIM {center_label} (ms, escala log)")
    # Sin titulo en el propio grafico -- va como caption/encabezado de seccion en la presentacion
    # e informe (ver Formato_Presentaciones.pdf / Formato_Informes.pdf), no duplicado adentro de
    # la figura.
    ax.grid(alpha=0.3, which="both")
    ax.legend()
    fig.tight_layout()

    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        fig.savefig(args.out, dpi=150)
        print(f"Figura guardada en {args.out}")
    else:
        plt.show()


if __name__ == "__main__":
    main()
