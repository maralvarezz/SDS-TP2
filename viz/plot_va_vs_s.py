#!/usr/bin/env python3
"""Punto e/f: polarización vs fracción gigante para las densidades pedidas.

Mismo criterio de formato que plot_va_vs_eta.py / plot_s_vs_eta.py: TODAS las densidades
pedidas van juntas en un unico panel, sea con uno o con los dos modelos cargados. Con UN
modelo se colorea por rho; con AMBOS (punto f) tambien se colorea por rho, distinguiendo el
modelo por trazo (solido Vicsek / punteado Votante, via MODEL_STYLE) en vez de separar en
subplots por rho. Nota: se dejó de colorear por eta con una barra de colores continua (lo
teniamos asi antes, pero el otro grupo -- cuyo formato es el validado por la catedra -- no lo
usa): cada curva es de un solo color por rho, conectando los puntos en orden de eta creciente,
con barras de error en ambos ejes. Mas simple de leer y consistente con como se presenta el
resto de la comparacion.
"""

import argparse
import sys
from pathlib import Path
import matplotlib.pyplot as plt
from data_io import ALL_CLUSTER_DENSITIES, BASE_DENSITIES, CLUSTER_DENSITIES, MODELS, read_experiments, require_densities, rows_for_density
from plot_common import MODEL_STYLE, density_color, density_label, finish_figure, parse_rho_list, style_axis


def parse_models(value):
    models = [m.strip().upper() for m in value.split(",") if m.strip()]
    unknown = [m for m in models if m not in MODELS]
    if unknown:
        raise ValueError(f"Modelo(s) desconocido(s): {unknown}, validos: {list(MODELS)}")
    if not models:
        raise ValueError("--models no puede quedar vacio")
    return models


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("polarization", type=Path, help="experiments_polarization.csv")
    parser.add_argument("clusters", type=Path, nargs="?", default=None,
                         help="experiments_clusters.csv (no hace falta si --densities=polarization)")
    parser.add_argument("--out", type=Path)
    parser.add_argument("--models", type=str, default=",".join(MODELS),
                         help="modelos separados por coma a graficar/exigir (default: todos)")
    parser.add_argument("--densities", choices=["polarization", "cluster", "all"], default="all",
                         help="polarization: solo rho=2,4,8 (no hace falta el CSV de clusters). "
                              "cluster: solo las 3 densidades de cluster (hace falta el CSV de "
                              "clusters como segundo argumento). all (default): los dos estudios "
                              "juntos (hacen falta los dos CSV).")
    parser.add_argument("--rho", type=str, default=None,
                         help="lista de densidades separadas por coma para quedarse con un "
                              "subconjunto puntual, ej. --rho=2,8,1/pi,1/3pi -- se aplica DESPUES "
                              "de --densities (que sigue determinando que CSV hacen falta), solo "
                              "filtra cuales de esas densidades ya cargadas se terminan graficando.")
    args = parser.parse_args()
    try:
        selected_models = parse_models(args.models)
        if args.densities == "polarization":
            groups = [(None, BASE_DENSITIES)]
            paths = [args.polarization]
        elif args.densities == "cluster":
            if args.clusters is None:
                sys.exit("--densities=cluster necesita el CSV de clusters como segundo argumento")
            groups = [(None, CLUSTER_DENSITIES)]
            paths = [args.clusters]
        else:
            if args.clusters is None:
                sys.exit("--densities=all necesita ambos CSV (polarization y clusters)")
            groups = [("Polarización", BASE_DENSITIES), ("Clusters", CLUSTER_DENSITIES)]
            paths = [args.polarization, args.clusters]
        rows = read_experiments(paths, required_models=tuple(selected_models))
        require_densities(rows, ALL_CLUSTER_DENSITIES if args.densities == "all" else groups[0][1])
    except ValueError as error:
        sys.exit(str(error))

    multi_model = len(selected_models) > 1
    all_densities = [rho for _, densities in groups for rho in densities]

    if args.rho:
        try:
            requested = parse_rho_list(args.rho)
        except ValueError as error:
            sys.exit(str(error))
        filtered = []
        for target in requested:
            match = next((rho for rho in all_densities if abs(rho - target) < 1e-6), None)
            if match is None:
                hint = " -- probar con --densities=all" if args.densities != "all" else ""
                sys.exit(f"--rho pide {target:.6f} pero no esta entre las densidades cargadas "
                          f"con --densities={args.densities}{hint}")
            filtered.append(match)
        all_densities = filtered

    fig, ax = plt.subplots(figsize=(8.5, 6.6) if multi_model else (7.5, 6.2))
    for rho in all_densities:
        density_rows = rows_for_density(rows, rho)
        for model in selected_models:
            selected = sorted((row for row in density_rows if row["model"] == model), key=lambda row: row["eta"])
            if not selected:
                sys.exit(f"Faltan filas de {model} para rho={rho:.6f}")
            # Color por indice dentro de TODAS las densidades posibles (no solo las que van en
            # este panel) -- mismo criterio que plot_va_vs_eta.py / plot_s_vs_eta.py, asi rho=2
            # es siempre azul, rho=8 siempre verde, etc. sea que se filtren densidades con --rho.
            color = density_color(rho, ALL_CLUSTER_DENSITIES)
            if multi_model:
                style = MODEL_STYLE[model]
                marker, linestyle = style["marker"], style["linestyle"]
                label = rf"{style['label']}, $\rho={density_label(rho)}$"
            else:
                marker, linestyle = "o", "-"
                label = rf"$\rho={density_label(rho)}$"
            xs, ys = [row["mean_va"] for row in selected], [row["mean_S"] for row in selected]
            # Ejes invertidos: va en x, S en y. Solo barras de error en S (ahora verticales, yerr)
            # -- con las dos a la vez el grafico se llenaba de cruces superpuestas entre curvas y
            # quedaba dificil de leer; el eje de va ya se puede leer sin barra propia gracias a la
            # linea que conecta los puntos en orden de eta creciente.
            ax.errorbar(xs, ys, yerr=[row["std_S"] for row in selected], color=color, marker=marker,
                        linestyle=linestyle, capsize=3, linewidth=1.3, markersize=5.5, label=label)
    ax.set_xlabel(r"Polarización $v_a$")
    ax.set_ylabel(r"Fracción gigante $S$")
    ax.set_xlim(-0.02, 1.05)
    style_axis(ax)
    ax.legend(frameon=False, ncol=2 if (multi_model or len(all_densities) > 3) else 1)
    fig.suptitle("Polarización y componente gigante")
    finish_figure(fig, args.out)


if __name__ == "__main__":
    main()
