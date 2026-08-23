#!/usr/bin/env python3
"""
Compara los tiempos de ejecucion del CIM entre TP1 y TP2 (punto g del enunciado de TP2),
en escala log-log y a densidad fija (para que la pendiente sea comparable con el orden de
complejidad esperado del CIM).

Toma uno o mas *_runs.csv generados por viz/time_analysis.py de TP1 (columnas series, n,
elapsed_ns/elapsed_ms entre otras) y el output/cim_timing_tp2.csv que genera CimTimingMain de TP2
(columnas n, run, l, m, elapsed_ns), agrega por N (media y desvio) y grafica ambas curvas juntas
en escala log-log, anotando en la leyenda la pendiente del ajuste de potencia (regresion lineal en
log-log) de cada una.

Como el rango de N usado (10 a 5000) hace que la serie "free" de --compare-density sea inviable
(con L fijo, en N grandes la densidad de particulas con radio es geometricamente imposible de
empaquetar), el flujo recomendado es correr TP1 una vez POR CADA N con el L y M ya escalados a
densidad fija (sin --compare-density), lo que genera un _runs.csv por N. Este script acepta varios
archivos a la vez y los une antes de agregar.

Uso:
    python3 viz/compare_cim_timing.py \
        --tp1-runs ~/Documents/GitHub/SDS-TP1/output/figures/time_N_*/time_N_*_runs.csv \
        --tp2-runs output/cim_timing_tp2.csv \
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


def aggregate(paths, n_key, ms_from_row, series_key=None, series_value=None):
    grouped = defaultdict(list)
    for path in paths:
        with open(path, newline="") as f:
            reader = csv.DictReader(f)
            for row in reader:
                if series_key is not None and series_value is not None:
                    if row.get(series_key, "standard") != series_value:
                        continue
                n = int(row[n_key])
                grouped[n].append(ms_from_row(row))

    ns = sorted(grouped)
    means = [statistics.fmean(grouped[n]) for n in ns]
    stdevs = [statistics.stdev(grouped[n]) if len(grouped[n]) > 1 else 0.0 for n in ns]
    return ns, means, stdevs


def power_law_slope(ns, means):
    """Pendiente de la recta que mejor ajusta log10(mean) vs log10(n) (regresion lineal)."""
    log_n = np.log10(ns)
    log_mean = np.log10(means)
    slope, _ = np.polyfit(log_n, log_mean, 1)
    return slope


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--tp1-runs", type=Path, required=True, nargs="+",
                         help="uno o mas CSV *_runs.csv generados por viz/time_analysis.py de TP1")
    parser.add_argument("--tp2-runs", type=Path, required=True,
                         help="CSV generado por CimTimingMain de TP2 (output/cim_timing_tp2.csv)")
    parser.add_argument("--tp1-series", default="standard",
                         help="Serie a usar de los CSV de TP1 (default: standard, que es lo que se "
                              "genera corriendo TP1 SIN --compare-density; usar 'fixed_density' si "
                              "en cambio corriste TP1 con --compare-density)")
    parser.add_argument("--out", type=Path, default=None, help="si se pasa, guarda la figura en vez de mostrarla")
    args = parser.parse_args()

    for path in (*args.tp1_runs, args.tp2_runs):
        if not path.exists():
            sys.exit(f"No existe el archivo: {path}")

    n1, mean1, std1 = aggregate(
        args.tp1_runs, "n", lambda row: float(row["elapsed_ms"]),
        series_key="series", series_value=args.tp1_series,
    )
    if not n1:
        sys.exit(
            f"Los CSV de TP1 no tienen filas con series={args.tp1_series!r}. "
            "Si corriste TP1 con --compare-density, pasa --tp1-series fixed_density."
        )
    n2, mean2, std2 = aggregate([args.tp2_runs], "n", lambda row: float(row["elapsed_ns"]) / 1_000_000)

    slope1 = power_law_slope(n1, mean1)
    slope2 = power_law_slope(n2, mean2)

    fig, ax = plt.subplots(figsize=(7.5, 5))
    ax.errorbar(n1, mean1, yerr=std1, marker="o", label=f"TP1 (CIM standalone), pendiente≈{slope1:.2f}",
                color="tab:blue", capsize=3, linewidth=1.5)
    ax.errorbar(n2, mean2, yerr=std2, marker="s", label=f"TP2 (CIM dentro de SimulationEngine), pendiente≈{slope2:.2f}",
                color="tab:orange", capsize=3, linewidth=1.5)
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlabel("N (escala log)")
    ax.set_ylabel("Tiempo CIM promedio (ms, escala log)")
    ax.set_title("Tiempos de ejecucion del CIM: TP1 vs TP2 (densidad fija, rc=1, periodico)")
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
