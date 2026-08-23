#!/usr/bin/env python3
"""Punto c/f: polarización estacionaria vs ruido para rho=2,4,8."""

import argparse
import sys
from pathlib import Path
import matplotlib.pyplot as plt
from data_io import BASE_DENSITIES, MODELS, read_experiments, require_densities, rows_for_density
from plot_common import MODEL_STYLE, density_label, finish_figure, style_axis


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="experiments_polarization.csv")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    try:
        rows = read_experiments([args.input])
        require_densities(rows, BASE_DENSITIES)
    except ValueError as error:
        sys.exit(str(error))
    fig, axes = plt.subplots(1, 3, sharex=True, sharey=True, figsize=(12, 4))
    for ax, rho in zip(axes, BASE_DENSITIES):
        density_rows = rows_for_density(rows, rho)
        for model in MODELS:
            selected = sorted((row for row in density_rows if row["model"] == model), key=lambda row: row["eta"])
            if not selected:
                sys.exit(f"Faltan filas de {model} para rho={rho}")
            style = MODEL_STYLE[model]
            ax.errorbar([row["eta"] for row in selected], [row["mean_va"] for row in selected],
                        yerr=[row["std_va"] for row in selected], color=style["color"],
                        marker=style["marker"], linestyle=style["linestyle"], capsize=3,
                        linewidth=1.2, markersize=4, label=style["label"])
        ax.set_title(rf"$\rho={density_label(rho)}$")
        ax.set_xlabel(r"Ruido $\eta$")
        ax.set_ylim(-0.02, 1.05)
        style_axis(ax)
    axes[0].set_ylabel(r"Polarización estacionaria $v_a$")
    axes[-1].legend(frameon=False)
    fig.suptitle("Polarización en función del ruido")
    finish_figure(fig, args.out)


if __name__ == "__main__":
    main()
