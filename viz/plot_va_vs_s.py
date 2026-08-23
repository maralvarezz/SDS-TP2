#!/usr/bin/env python3
"""Punto e/f: polarización vs fracción gigante para las seis densidades."""

import argparse
import sys
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.cm import ScalarMappable
from matplotlib.colors import Normalize
from data_io import ALL_CLUSTER_DENSITIES, MODELS, read_experiments, require_densities, rows_for_density
from plot_common import MODEL_STYLE, density_label, style_axis


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
    eta_values = [row["eta"] for row in rows]
    norm = Normalize(min(eta_values), max(eta_values))
    cmap = plt.get_cmap("viridis")
    fig, axes = plt.subplots(2, 3, sharex=True, sharey=True, figsize=(12, 7.5))
    for ax, rho in zip(axes.flat, ALL_CLUSTER_DENSITIES):
        density_rows = rows_for_density(rows, rho)
        for model in MODELS:
            selected = sorted((row for row in density_rows if row["model"] == model), key=lambda row: row["eta"])
            if not selected:
                sys.exit(f"Faltan filas de {model} para rho={rho:.6f}")
            style = MODEL_STYLE[model]
            xs, ys = [row["mean_S"] for row in selected], [row["mean_va"] for row in selected]
            ax.plot(xs, ys, color=style["color"], linestyle=style["linestyle"], alpha=0.65,
                    linewidth=1.1, label=style["label"])
            ax.errorbar(xs, ys, xerr=[row["std_S"] for row in selected],
                        yerr=[row["std_va"] for row in selected], fmt="none",
                        ecolor=style["color"], alpha=0.35, capsize=2)
            ax.scatter(xs, ys, c=[row["eta"] for row in selected], cmap=cmap, norm=norm,
                       marker=style["marker"], edgecolors=style["color"], linewidths=0.8, s=30, zorder=3)
        ax.set_title(rf"$\rho={density_label(rho)}$")
        ax.set_xlim(-0.02, 1.05)
        ax.set_ylim(-0.02, 1.05)
        style_axis(ax)
    for ax in axes[-1, :]:
        ax.set_xlabel(r"Fracción gigante $S$")
    for ax in axes[:, 0]:
        ax.set_ylabel(r"Polarización $v_a$")
    axes[0, -1].legend(frameon=False)
    fig.colorbar(ScalarMappable(norm=norm, cmap=cmap), ax=axes, label=r"Ruido $\eta$", fraction=0.025, pad=0.02)
    fig.suptitle("Polarización y componente gigante")
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        fig.savefig(args.out, dpi=180, bbox_inches="tight")
        plt.close(fig)
        print(f"Figura guardada en {args.out}")
    else:
        plt.show()


if __name__ == "__main__":
    main()
