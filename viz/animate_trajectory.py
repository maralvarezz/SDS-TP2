#!/usr/bin/env python3
"""Anima trajectory.txt y colorea cada velocidad según su ángulo."""

import argparse
import math
import sys
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.animation import FFMpegWriter, FuncAnimation, PillowWriter
from matplotlib.colors import Normalize
from data_io import read_trajectory


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("--out", type=Path, help="salida .mp4 o .gif; sin esta opción abre una ventana")
    parser.add_argument("--fps", type=int, default=20)
    parser.add_argument("--stride", type=int, default=1, help="usar uno de cada N frames")
    parser.add_argument("--arrow-length", type=float, help="largo visual; default 4%% de L")
    parser.add_argument("--dpi", type=int, default=150)
    args = parser.parse_args()
    if args.fps <= 0 or args.stride <= 0:
        sys.exit("--fps y --stride deben ser positivos")
    if args.out and args.out.suffix.lower() not in {".mp4", ".gif"}:
        sys.exit("--out debe terminar en .mp4 o .gif")
    try:
        metadata, frames = read_trajectory(args.input)
        box_size = float(metadata["l"])
    except (ValueError, KeyError) as error:
        sys.exit(f"Trayectoria inválida: {error}")
    frames = frames[::args.stride]
    arrow_length = args.arrow_length or box_size * 0.04

    fig, ax = plt.subplots(figsize=(7.2, 7.2))
    ax.set(xlim=(0, box_size), ylim=(0, box_size), xlabel="x", ylabel="y")
    ax.set_aspect("equal")
    ax.grid(alpha=0.15)
    norm = Normalize(-math.pi, math.pi)

    def components(rows):
        x = [row["x"] for row in rows]
        y = [row["y"] for row in rows]
        angles = [math.atan2(row["vy"], row["vx"]) for row in rows]
        u = [arrow_length * math.cos(angle) for angle in angles]
        v = [arrow_length * math.sin(angle) for angle in angles]
        return x, y, u, v, angles

    _, first_rows = frames[0]
    x, y, u, v, angles = components(first_rows)
    quiver = ax.quiver(x, y, u, v, angles, cmap="hsv", norm=norm, angles="xy",
                       scale_units="xy", scale=1, width=0.004, headwidth=3.5, headlength=4.5)
    colorbar = fig.colorbar(quiver, ax=ax, fraction=0.046, pad=0.04)
    colorbar.set_label(r"Ángulo $\theta$")
    colorbar.set_ticks([-math.pi, -math.pi / 2, 0, math.pi / 2, math.pi])
    colorbar.set_ticklabels([r"$-\pi$", r"$-\pi/2$", "0", r"$\pi/2$", r"$\pi$"])
    model, rho, eta = (metadata.get(key, "?") for key in ("model", "rho", "eta"))

    def update(index):
        t, rows = frames[index]
        x, y, u, v, angles = components(rows)
        quiver.set_offsets(list(zip(x, y)))
        quiver.set_UVC(u, v, angles)
        ax.set_title(f"{model} · ρ={rho} · η={eta} · t={t}", loc="left")
        return (quiver,)

    update(0)
    animation = FuncAnimation(fig, update, frames=len(frames), interval=1000 / args.fps, blit=False)
    fig.tight_layout()
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        writer = FFMpegWriter(fps=args.fps, bitrate=2400) if args.out.suffix.lower() == ".mp4" else PillowWriter(fps=args.fps)
        animation.save(args.out, writer=writer, dpi=args.dpi)
        plt.close(fig)
        print(f"Animación guardada en {args.out}")
    else:
        plt.show()


if __name__ == "__main__":
    main()
