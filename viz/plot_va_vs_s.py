#!/usr/bin/env python3
"""Punto e/f: polarización vs fracción gigante para las densidades pedidas.

Mismo criterio de formato que plot_va_vs_eta.py / plot_s_vs_eta.py. Nota: se dejó de colorear
por eta con una barra de colores continua (lo teniamos asi antes, pero el otro grupo -- cuyo
formato es el validado por la catedra -- no lo usa: cada curva es de un solo color por rho (o
por modelo en el punto f), conectando los puntos en orden de eta creciente, con barras de error
en ambos ejes. Mas simple de leer y consistente con como se presenta el resto de la comparacion.
"""

import argparse
import sys
from pathlib import Path
import matplotlib.pyplot as plt
from data_io import ALL_CLUSTER_DENSITIES, BASE_DENSITIES, CLUSTER_DENSITIES, MODELS, read_experiments, require_densities, rows_for_density
from plot_common import MODEL_STYLE, density_color, density_label, finish_figure, style_axis


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

    if multi_model:
        all_densities = [rho for _, densities in groups for rho in densities]
        two_rows = len(all_densities) > 3
        if two_rows:
            fig, axes = plt.subplots(2, 3, sharex=True, sharey=True, figsize=(12, 7.5))
            axes_flat = list(axes.flat)
        else:
            fig, axes = plt.subplots(1, len(all_densities), sharex=True, sharey=True,
                                      figsize=(4 * len(all_densities), 4.2))
            axes_flat = list(axes) if len(all_densities) > 1 else [axes]

        for ax, rho in zip(axes_flat, all_densities):
            density_rows = rows_for_density(rows, rho)
            for model in selected_models:
                selected = sorted((row for row in density_rows if row["model"] == model), key=lambda row: row["eta"])
                if not selected:
                    sys.exit(f"Faltan filas de {model} para rho={rho:.6f}")
                style = MODEL_STYLE[model]
                xs, ys = [row["mean_S"] for row in selected], [row["mean_va"] for row in selected]
                ax.errorbar(xs, ys, xerr=[row["std_S"] for row in selected],
                            yerr=[row["std_va"] for row in selected], color=style["color"],
                            marker=style["marker"], linestyle=style["linestyle"], capsize=3,
                            linewidth=1.2, markersize=5, label=style["label"])
            ax.set_title(rf"$\rho={density_label(rho)}$")
            ax.set_ylim(-0.02, 1.05)
            style_axis(ax)

        if two_rows:
            for ax in axes[-1, :]:
                ax.set_xlabel(r"Fracción gigante $S$")
            for ax in axes[:, 0]:
                ax.set_ylabel(r"Polarización $v_a$")
            axes[0, -1].legend(frameon=False)
        else:
            for ax in axes_flat:
                ax.set_xlabel(r"Fracción gigante $S$")
            axes_flat[0].set_ylabel(r"Polarización $v_a$")
            axes_flat[-1].legend(frameon=False)
    else:
        model = selected_models[0]
        fig, axes = plt.subplots(1, len(groups), figsize=(6.5 * len(groups), 5.8), squeeze=False)
        axes = axes[0]
        for ax, (title, densities) in zip(axes, groups):
            for rho in densities:
                density_rows = rows_for_density(rows, rho)
                selected = sorted((row for row in density_rows if row["model"] == model), key=lambda row: row["eta"])
                if not selected:
                    sys.exit(f"Faltan filas de {model} para rho={rho:.6f}")
                color = density_color(rho, densities)
                xs, ys = [row["mean_S"] for row in selected], [row["mean_va"] for row in selected]
                ax.errorbar(xs, ys, xerr=[row["std_S"] for row in selected],
                            yerr=[row["std_va"] for row in selected], color=color, marker="o",
                            linestyle="-", capsize=3, linewidth=1.3, markersize=5.5,
                            label=rf"$\rho={density_label(rho)}$")
            ax.set_xlabel(r"Fracción gigante $S$")
            ax.set_ylim(-0.02, 1.05)
            style_axis(ax)
            ax.legend(frameon=False)
            if title:
                ax.set_title(title)
        axes[0].set_ylabel(r"Polarización $v_a$")

    fig.suptitle("Polarización y componente gigante")
    finish_figure(fig, args.out)


if __name__ == "__main__":
    main()
