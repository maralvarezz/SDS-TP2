#!/usr/bin/env python3
"""Punto g: compara tiempos crudos del CIM de TP1 y TP2 para valores comunes de N."""

import argparse
import statistics
import sys
from collections import defaultdict
from pathlib import Path
import matplotlib.pyplot as plt
from data_io import read_tp1_timings, read_tp2_timings
from plot_common import finish_figure, style_axis


def aggregate(rows):
    grouped = defaultdict(list)
    for n, elapsed_ns in rows:
        grouped[n].append(elapsed_ns / 1_000_000)
    return {n: (statistics.fmean(values), statistics.stdev(values) if len(values) > 1 else 0.0)
            for n, values in grouped.items()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tp1", type=Path, help="CSV crudo de corridas de TP1")
    parser.add_argument("tp2", type=Path, help="output/cim_timing_tp2.csv")
    parser.add_argument("--tp1-series", default="free",
                        help="serie del CSV de TP1 comparable con L=20 y M=10 (default: free)")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    try:
        tp1 = aggregate(read_tp1_timings(args.tp1, args.tp1_series))
        tp2 = aggregate(read_tp2_timings(args.tp2))
    except ValueError as error:
        sys.exit(str(error))
    common_n = sorted(set(tp1) & set(tp2))
    if not common_n:
        sys.exit("TP1 y TP2 no tienen valores de N en común")
    if set(tp1) != set(tp2):
        print(f"Aviso: sólo se grafican N comunes: {common_n}", file=sys.stderr)
    fig, ax = plt.subplots(figsize=(8, 5))
    for label, data, color, marker in (("TP1", tp1, "#5b5f97", "o"), ("TP2", tp2, "#d1495b", "s")):
        ax.errorbar(common_n, [data[n][0] for n in common_n], yerr=[data[n][1] for n in common_n],
                    color=color, marker=marker, capsize=3, linewidth=1.2, label=label)
    ax.set_xlabel("Cantidad de partículas N")
    ax.set_ylabel("Tiempo del CIM [ms]")
    ax.set_title("Comparación de tiempos del Cell Index Method", loc="left")
    ax.legend(frameon=False)
    style_axis(ax)
    finish_figure(fig, args.out)


if __name__ == "__main__":
    main()
