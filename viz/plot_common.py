"""Estilo compartido por los gráficos finales."""

from pathlib import Path
import math
import matplotlib.pyplot as plt

MODEL_STYLE = {
    "VICSEK": {"color": "#1769aa", "marker": "o", "linestyle": "-", "label": "Vicsek"},
    "VOTER": {"color": "#d1495b", "marker": "s", "linestyle": "--", "label": "Votante"},
}


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
