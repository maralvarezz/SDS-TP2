"""Estilo compartido por los gráficos finales."""

from __future__ import annotations

from pathlib import Path
import math
import matplotlib.pyplot as plt

plt.rcParams.update({"font.size": 13, "axes.titlesize": 15, "axes.labelsize": 13,
                      "legend.fontsize": 12, "xtick.labelsize": 11, "ytick.labelsize": 11,
                      "figure.titlesize": 17})

MODEL_STYLE = {
    "VICSEK": {"color": "#1769aa", "marker": "o", "linestyle": "-", "label": "Vicsek"},
    "VOTER": {"color": "#d1495b", "marker": "s", "linestyle": "--", "label": "Votante"},
}

# Paleta para cuando una sola corrida de modelo mezcla varias densidades en un mismo panel
# (formato validado por la catedra: una sola curva por rho, sin separar en subplots).
# Mismos colores que usa el grupo de referencia: azul/naranja/verde en orden creciente de rho.
_DENSITY_PALETTE = ["#1f77b4", "#ff7f0e", "#2ca02c", "#9467bd", "#8c564b", "#17becf"]


def density_color(rho, densities):
    """Color estable para `rho` dentro de la lista ordenada `densities` (por indice, no por
    valor), para que ρ=2 sea siempre azul, ρ=4 naranja, ρ=8 verde, etc., sin importar en que
    orden se hayan cargado las filas."""
    densities = list(densities)
    for index, value in enumerate(densities):
        if abs(rho - value) < 1e-6:
            return _DENSITY_PALETTE[index % len(_DENSITY_PALETTE)]
    return _DENSITY_PALETTE[0]


def density_label(rho):
    for value in (2.0, 4.0, 8.0):
        if abs(rho - value) < 5e-6:
            return f"{value:g}"
    for divisor in (1, 2, 3):
        if abs(rho - 1 / (divisor * math.pi)) < 5e-6:
            return r"1/\pi" if divisor == 1 else rf"1/({divisor}\pi)"
    return f"{rho:.4g}"


def style_axis(ax):
    ax.grid(alpha=0.25)
    ax.spines[["top", "right"]].set_visible(False)


def finish_figure(fig, out: Path | None):
    fig.tight_layout()
    if out:
        out.parent.mkdir(parents=True, exist_ok=True)
        fig.savefig(out, dpi=180, bbox_inches="tight")
        plt.close(fig)
        print(f"Figura guardada en {out}")
    else:
        plt.show()
