#!/usr/bin/env python3
"""Grafica va(t) y S(t) para una o más corridas con el contrato observables.txt."""
import argparse
import sys
from pathlib import Path
import matplotlib.pyplot as plt
from data_io import read_observables
from plot_common import MODEL_STYLE, finish_figure, style_axis


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inputs", nargs="+", type=Path)
    parser.add_argument("--stationary-t", nargs="*", type=float, default=[],
                        help="un inicio estacionario por archivo, en el mismo orden")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    if args.stationary_t and len(args.stationary_t) != len(args.inputs):
        sys.exit("--stationary-t debe recibir exactamente un valor por archivo")
    try:
        series = [(path, *read_observables(path)) for path in args.inputs]
    except ValueError as error:
        sys.exit(str(error))
    if len(series) > 1 and len({(m.get("rho"), m.get("eta")) for _, m, _ in series}) != 1:
        sys.exit("Para superponer evoluciones, todos los archivos deben tener igual rho y eta")

    fig, (ax_va, ax_s) = plt.subplots(2, 1, sharex=True, figsize=(9, 6.4))
    fallback_colors = plt.get_cmap("tab10")
    for index, (path, metadata, rows) in enumerate(series):
        model = metadata.get("model", path.stem).upper()
        style = MODEL_STYLE.get(model, {"color": fallback_colors(index % 10), "linestyle": "-", "label": path.stem})
        ts = [row["t"] for row in rows]
        label = style["label"] if len(series) > 1 else f"{style['label']} ({path.name})"
        ax_va.plot(ts, [row["polarization"] for row in rows], color=style["color"],
                   linestyle=style["linestyle"], linewidth=1.25, label=label)
        ax_s.plot(ts, [row["cluster_fraction"] for row in rows], color=style["color"],
                  linestyle=style["linestyle"], linewidth=1.25, label=label)
        if args.stationary_t:
            for ax in (ax_va, ax_s):
                ax.axvline(args.stationary_t[index], color=style["color"], linestyle=":", linewidth=1.2)
    ax_va.set_ylabel(r"Polarización $v_a$")
    ax_s.set_ylabel(r"Fracción gigante $S$")
    ax_s.set_xlabel("Tiempo t")
    for ax in (ax_va, ax_s):
        ax.set_ylim(-0.02, 1.02)
        style_axis(ax)
        ax.legend(frameon=False)
    metadata = series[0][1]
    fig.suptitle(f"Evolución temporal · ρ={metadata.get('rho', '?')} · η={metadata.get('eta', '?')}")
    finish_figure(fig, args.out)


if __name__ == "__main__":
    main()
