#!/usr/bin/env python3
"""Grafica va(t) y S(t) para una o más corridas con el contrato observables.txt."""
import argparse
import sys
from pathlib import Path
import matplotlib.pyplot as plt
from data_io import read_observables
from plot_common import MODEL_STYLE, density_label, finish_figure, style_axis


def fmt_num(value):
    """Numero 'limpio' para titulos: sin ceros de mas (3.0000 -> 3, 4.5000 -> 4.5)."""
    try:
        return f"{float(value):g}"
    except (TypeError, ValueError):
        return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inputs", nargs="+", type=Path)
    parser.add_argument("--stationary-t", nargs="*", type=float, default=[],
                        help="un inicio estacionario por archivo, en el mismo orden")
    parser.add_argument("--group-by", choices=["model", "rho", "pair"], default="model",
                         help="model (default): superpone corridas que comparan MODELO a igual "
                              "rho/eta (exige mismo rho y eta en todos los archivos, colorea por "
                              "modelo, como antes). rho: superpone corridas que comparan DENSIDAD "
                              "a igual modelo/eta (exige mismo modelo y eta, colorea por rho) -- "
                              "para el punto (b) tipo 'evolucion a eta fijo para varias rho'. pair: "
                              "sin ninguna restriccion de consistencia -- cada archivo puede tener "
                              "su propio rho Y su propio eta, coloreado por indice y etiquetado con "
                              "ambos valores -- para mostrar un par de casos caracteristicos "
                              "cruzados (ej. rho bajo/eta bajo vs rho alto/eta alto) en un mismo "
                              "grafico, correccion de catedra del punto (b).")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    if args.stationary_t and len(args.stationary_t) != len(args.inputs):
        sys.exit("--stationary-t debe recibir exactamente un valor por archivo")
    try:
        series = [(path, *read_observables(path)) for path in args.inputs]
    except ValueError as error:
        sys.exit(str(error))

    if args.group_by == "model":
        if len(series) > 1 and len({(m.get("rho"), m.get("eta")) for _, m, _ in series}) != 1:
            sys.exit("Con --group-by=model todos los archivos deben tener igual rho y eta")
    elif args.group_by == "rho":
        if len(series) > 1 and len({(m.get("model"), m.get("eta")) for _, m, _ in series}) != 1:
            sys.exit("Con --group-by=rho todos los archivos deben tener igual modelo y eta")
    # pair: sin restricciones -- cada archivo trae su propio (rho, eta).

    fig, (ax_va, ax_s) = plt.subplots(2, 1, sharex=True, figsize=(9, 6.4))
    fallback_colors = plt.get_cmap("tab10")
    for index, (path, metadata, rows) in enumerate(series):
        if args.group_by == "model":
            model = metadata.get("model", path.stem).upper()
            style = MODEL_STYLE.get(model, {"color": fallback_colors(index % 10), "linestyle": "-", "label": path.stem})
            label = style["label"] if len(series) > 1 else f"{style['label']} ({path.name})"
            color, linestyle = style["color"], style["linestyle"]
        elif args.group_by == "rho":
            rho = float(metadata["rho"]) if "rho" in metadata else None
            color = fallback_colors(index % 10)
            linestyle = "-"
            label = rf"$\rho={density_label(rho)}$" if rho is not None else path.stem
        else:
            rho = float(metadata["rho"]) if "rho" in metadata else None
            eta = float(metadata["eta"]) if "eta" in metadata else None
            color = fallback_colors(index % 10)
            linestyle = "-"
            if rho is not None and eta is not None:
                label = rf"$\rho={density_label(rho)}$, $\eta={eta:g}$"
            else:
                label = path.stem
        ts = [row["t"] for row in rows]
        ax_va.plot(ts, [row["polarization"] for row in rows], color=color,
                   linestyle=linestyle, linewidth=1.25, label=label)
        ax_s.plot(ts, [row["cluster_fraction"] for row in rows], color=color,
                  linestyle=linestyle, linewidth=1.25, label=label)
        if args.stationary_t:
            for ax in (ax_va, ax_s):
                ax.axvline(args.stationary_t[index], color=color, linestyle=":", linewidth=1.2)
    ax_va.set_ylabel(r"Polarización $v_a$")
    ax_s.set_ylabel(r"Fracción gigante $S$")
    ax_s.set_xlabel("Tiempo t")

    ax_va.set_ylim(-0.02, 1.02)
    # El panel de S casi siempre satura cerca de 1 -- con la escala completa 0-1 las caidas
    # (lo que realmente interesa mostrar) quedan aplastadas en una franja de pocos pixeles.
    # Recorte: arranca un poco por debajo del minimo observado y llega hasta 1 (correccion de
    # catedra), en vez de forzar 0-1 fijo como en el panel de va (que si usa el rango completo).
    all_s = [row["cluster_fraction"] for _, _, rows in series for row in rows]
    s_min = min(all_s)
    s_pad = max((1.0 - s_min) * 0.06, 0.005)
    ax_s.set_ylim(max(-0.02, s_min - s_pad), 1.02)

    for ax in (ax_va, ax_s):
        style_axis(ax)
        ax.legend(frameon=False)
    metadata = series[0][1]
    if args.group_by == "model":
        fig.suptitle(f"Evolución temporal · ρ={fmt_num(metadata.get('rho', '?'))} · η={fmt_num(metadata.get('eta', '?'))}")
    elif args.group_by == "rho":
        fig.suptitle(f"Evolución temporal · modelo={metadata.get('model', '?')} · η={fmt_num(metadata.get('eta', '?'))}")
    else:
        models = {m.get("model", "?") for _, m, _ in series}
        model_label = models.pop() if len(models) == 1 else "/".join(sorted(models))
        fig.suptitle(f"Evolución temporal · modelo={model_label} · casos característicos")
    finish_figure(fig, args.out)


if __name__ == "__main__":
    main()
