#!/usr/bin/env python3
"""Punto c/f: polarización estacionaria vs ruido para rho=2,4,8.

Formato validado por la cátedra (calcado del de otro grupo, según correcciones que le
hicieron a un amigo + capturas de sus gráficos):
  - Con UN solo modelo cargado (--models=VICSEK, típico de comparaciones contra otro grupo
    que solo corrió el modelo estándar): las 3 densidades van TODAS en un mismo panel,
    coloreadas por rho (azul/naranja/verde), sin separar en subplots.
  - Con AMBOS modelos cargados (default, para el informe / punto f): se mantiene el formato
    anterior de 1x3 subplots por rho, coloreando por modelo dentro de cada subplot -- así es
    como el otro grupo presenta también la comparación Vicsek-vs-Votante.
"""

import argparse
import sys
from pathlib import Path
import matplotlib.pyplot as plt
from data_io import BASE_DENSITIES, MODELS, read_experiments, require_densities, rows_for_density
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
    parser.add_argument("input", type=Path, help="experiments_polarization.csv")
    parser.add_argument("--out", type=Path)
    parser.add_argument("--models", type=str, default=",".join(MODELS),
                         help="modelos separados por coma a graficar/exigir (default: todos). "
                              "Usar --models=VICSEK para comparaciones que solo corrieron ese "
                              "modelo (ej. contra otro grupo que no manda VOTER) -- en ese caso "
                              "las 3 densidades se muestran juntas en un solo panel.")
    args = parser.parse_args()
    try:
        selected_models = parse_models(args.models)
        rows = read_experiments([args.input], required_models=tuple(selected_models))
        require_densities(rows, BASE_DENSITIES)
    except ValueError as error:
        sys.exit(str(error))

    if len(selected_models) == 1:
        model = selected_models[0]
        fig, ax = plt.subplots(figsize=(7, 5.5))
        for rho in BASE_DENSITIES:
            density_rows = rows_for_density(rows, rho)
            selected = sorted((row for row in density_rows if row["model"] == model), key=lambda row: row["eta"])
            if not selected:
                sys.exit(f"Faltan filas de {model} para rho={rho}")
            color = density_color(rho, BASE_DENSITIES)
            ax.errorbar([row["eta"] for row in selected], [row["mean_va"] for row in selected],
                        yerr=[row["std_va"] for row in selected], color=color, marker="o",
                        linestyle="-", capsize=3, linewidth=1.4, markersize=5,
                        label=rf"$\rho={density_label(rho)}$")
        ax.set_xlabel(r"Ruido $\eta$")
        ax.set_ylabel(r"Polarización estacionaria $v_a$")
        ax.set_ylim(-0.02, 1.05)
        style_axis(ax)
        ax.legend(frameon=False)
        fig.suptitle("Polarización en función del ruido")
        finish_figure(fig, args.out)
        return

    fig, axes = plt.subplots(1, 3, sharex=True, sharey=True, figsize=(12, 4))
    for ax, rho in zip(axes, BASE_DENSITIES):
        density_rows = rows_for_density(rows, rho)
        for model in selected_models:
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
