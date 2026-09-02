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

    # El panel lateral de datos (modelo + parametros fijos + t) es un SUBPLOT propio (no texto
    # flotante sobre la figura), con margenes FIJOS en el GridSpec (left/right/top/bottom) en vez
    # de dejar que fig.tight_layout() los calcule: con ax.set_aspect("equal") tight_layout tira el
    # warning "Axes that are not compatible with tight_layout" y el resultado terminaba con la
    # escala del eje y recortada. Con margenes fijos el layout es 100% predecible.
    fig = plt.figure(figsize=(9.9, 7.2))
    gs = fig.add_gridspec(1, 2, width_ratios=[4.3, 1.15], wspace=0.30,
                          left=0.07, right=0.965, top=0.95, bottom=0.09)
    ax = fig.add_subplot(gs[0, 0])
    ax_info = fig.add_subplot(gs[0, 1])
    ax_info.axis("off")

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
    model = metadata.get("model", "?")
    # rho/eta vienen como texto crudo del header de trajectory.txt (ej. "4.0000", "0.5000") --
    # formateados con :g para no arrastrar ceros decimales de mas (4.0000 -> 4, 0.5000 -> 0.5),
    # mismo criterio que las etiquetas de las demas figuras de viz/.
    def fmt_num(value):
        try:
            return f"{float(value):g}"
        except (TypeError, ValueError):
            return value
    n = fmt_num(metadata.get("n", "?"))
    l = fmt_num(metadata.get("l", "?"))
    rc = fmt_num(metadata.get("rc", "?"))
    v0 = fmt_num(metadata.get("v0", "?"))
    dtsim = fmt_num(metadata.get("dt", "?"))
    rho, eta = fmt_num(metadata.get("rho", "?")), fmt_num(metadata.get("eta", "?"))

    # Parametros de la corrida en un unico bloque de texto con recuadro, centrado en el panel
    # lateral -- ademas de rho/eta/t van TODOS los parametros fijos de la configuracion (N, L,
    # rc, v0, dt), tal como pide la guia de presentaciones (1.7): "la informacion correspondiente
    # a la configuracion del sistema (parametros fijos) deben estar descriptas al costado de la
    # figura. Es decir cuales fueron las condiciones particulares bajo las cuales se obtuvieron
    # esos resultados." t va aparte del bloque fijo porque es lo unico que cambia cuadro a cuadro.
    fixed_lines = "\n".join([
        model, "",
        f"N = {n}", rf"$L = {l}$", rf"$r_c = {rc}$", rf"$v_0 = {v0}$", rf"$\Delta t = {dtsim}$", "",
        rf"$\rho = {rho}$", rf"$\eta = {eta}$",
    ])
    info_box = dict(boxstyle="round,pad=0.7", facecolor="#f7f7f7", edgecolor="#999999", linewidth=1.0)
    info_text = ax_info.text(0.5, 0.5, "", transform=ax_info.transAxes, ha="center", va="center",
                             fontsize=11.5, linespacing=1.7, bbox=info_box)

    def update(index):
        t, rows = frames[index]
        x, y, u, v, angles = components(rows)
        quiver.set_offsets(list(zip(x, y)))
        quiver.set_UVC(u, v, angles)
        info_text.set_text(fixed_lines + "\n\n" + f"t = {t}")
        return (quiver,)

    update(0)
    animation = FuncAnimation(fig, update, frames=len(frames), interval=1000 / args.fps, blit=False)
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        if args.out.suffix.lower() == ".mp4":
            # libx264 exige ancho y alto en PIXELES pares (submuestreo de croma yuv420p) --
            # figsize (pulgadas) x dpi no siempre da un entero par (ej. a Olivia le dio 1483x1080,
            # ancho impar) y ffmpeg tira "width not divisible by 2" y aborta sin escribir nada.
            # Se ajusta el ancho/alto de la figura una fraccion minima de pulgada (menos de un
            # pixel) para que el redondeo final quede en un numero par, para CUALQUIER combinacion
            # de figsize/--dpi, no solo para el tamaño que probamos nosotros.
            width_px = round(fig.get_figwidth() * args.dpi)
            height_px = round(fig.get_figheight() * args.dpi)
            if width_px % 2:
                fig.set_figwidth(fig.get_figwidth() + 1.0 / args.dpi)
            if height_px % 2:
                fig.set_figheight(fig.get_figheight() + 1.0 / args.dpi)
            writer = FFMpegWriter(fps=args.fps, bitrate=2400)
        else:
            writer = PillowWriter(fps=args.fps)
        animation.save(args.out, writer=writer, dpi=args.dpi)
        plt.close(fig)
        print(f"Animación guardada en {args.out}")
    else:
        plt.show()


if __name__ == "__main__":
    main()
