#!/usr/bin/env python3
"""
Grafica la evolucion temporal de va(t) y S(t) a partir del archivo que escribe Main.java
(output/observables.txt por defecto). Sirve para elegir a ojo, mirando el grafico, a partir de
que t el sistema entra en estado estacionario (punto b del enunciado).

Uso:
    python3 viz/plot_observables.py output/observables.txt
    python3 viz/plot_observables.py output/observables.txt --stationary-t 400
    python3 viz/plot_observables.py output/observables.txt --stationary-t 400 --out output/figures/va_s.png
"""
import argparse
import csv
import sys
from pathlib import Path

import matplotlib.pyplot as plt


def read_metadata(path):
    metadata = {}
    with open(path, "r") as f:
        for line in f:
            if not line.startswith("#"):
                break
            for token in line.lstrip("#").split():
                if "=" in token:
                    key, value = token.split("=", 1)
                    metadata[key] = value
    return metadata


def read_rows(path):
    ts, va, s = [], [], []
    with open(path, newline="") as f:
        data_lines = (line for line in f if not line.startswith("#"))
        reader = csv.DictReader(data_lines)
        for row in reader:
            ts.append(int(row["t"]))
            va.append(float(row["polarization"]))
            s.append(float(row["cluster_fraction"]))
    return ts, va, s


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("input", type=Path, help="txt generado por Main (t,polarization,cluster_fraction)")
    parser.add_argument("--stationary-t", type=float, default=None,
                         help="t donde arranca el estado estacionario; si se pasa, se marca con una linea vertical")
    parser.add_argument("--out", type=Path, default=None, help="si se pasa, guarda la figura en vez de mostrarla")
    args = parser.parse_args()

    if not args.input.exists():
        sys.exit(f"No existe el archivo: {args.input}")

    metadata = read_metadata(args.input)
    ts, va, s = read_rows(args.input)

    fig, (ax_va, ax_s) = plt.subplots(2, 1, sharex=True, figsize=(9, 6))

    ax_va.plot(ts, va, color="tab:blue", linewidth=1.2)
    ax_va.set_ylabel(r"$v_a$ (polarizacion)")
    ax_va.set_ylim(-0.02, 1.02)
    ax_va.grid(alpha=0.3)

    ax_s.plot(ts, s, color="tab:orange", linewidth=1.2)
    ax_s.set_ylabel("S (fraccion cluster mas grande)")
    ax_s.set_xlabel("t")
    ax_s.set_ylim(-0.02, 1.02)
    ax_s.grid(alpha=0.3)

    if args.stationary_t is not None:
        for ax in (ax_va, ax_s):
            ax.axvline(args.stationary_t, color="red", linestyle="--", linewidth=1,
                       label=f"inicio estacionario (t={args.stationary_t:g})")
        ax_va.legend(loc="lower right")

    title_bits = []
    for key in ("model", "rho", "eta", "n"):
        if key in metadata:
            title_bits.append(f"{key}={metadata[key]}")
    fig.suptitle(", ".join(title_bits))
    fig.tight_layout()

    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        fig.savefig(args.out, dpi=150)
        print(f"Figura guardada en {args.out}")
    else:
        plt.show()


if __name__ == "__main__":
    main()
