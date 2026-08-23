#!/usr/bin/env python3
"""Punto d/f: fracción gigante estacionaria vs ruido para las seis densidades."""

import argparse
import sys
from pathlib import Path
import matplotlib.pyplot as plt
from data_io import ALL_CLUSTER_DENSITIES, MODELS, read_experiments, require_densities, rows_for_density
from plot_common import MODEL_STYLE, density_label, finish_figure, style_axis


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("polarization", type=Path, help="experiments_polarization.csv")
    parser.add_argument("clusters", type=Path, help="experiments_clusters.csv")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    try:
        rows = read_experiments([args.polarization, args.clusters])
        require_densities(rows, ALL_CLUSTER_DENSITIES)
    except ValueError as error:
        sys.exit(str(error))
    fig, axes = plt.subplots(2, 3, sharex=True, sharey=True, figsize=(12, 7.2))
    for ax, rho in zip(axes.flat, ALL_CLUSTER_DENSITIES):
        density_rows = rows_for_density(rows, rho)
        for model in MODELS:
            selected = sorted((row for row in density_rows if row["model"] == model), key=lambda row: row["eta"])
            if not selected:
                sys.exit(f"Faltan filas de {model} para rho={rho:.6f}")
            style = MODEL_STYLE[model]
            ax.errorbar([row["eta"] for row in selected], [row["mean_S"] for row in selected],
                        yerr=[row["std_S"] for row in selected], color=style["color"],
                        marker=style["marker"], linestyle=style["linestyle"], capsize=3,
                        linewidth=1.1, markersize=3.5, label=style["label"])
        ax.set_title(rf"$\rho={density_label(rho)}$")
        ax.set_ylim(-0.02, 1.05)
        style_axis(ax)
    for ax in axes[-1, :]:
        ax.set_xlabel(r"Ruido $\eta$")
    for ax in axes[:, 0]:
        ax.set_ylabel(r"Fracción gigante $S$")
    axes[0, -1].legend(frameon=False)
    fig.suptitle("Componente gigante en función del ruido")
    finish_figure(fig, args.out)


if __name__ == "__main__":
    main()
