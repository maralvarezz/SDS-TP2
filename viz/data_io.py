"""Lectura y validación de los contratos de salida reales del TP2."""

from __future__ import annotations

import csv
import math
from collections import defaultdict
from pathlib import Path

OBSERVABLE_COLUMNS = ["t", "polarization", "cluster_fraction"]
TRAJECTORY_COLUMNS = ["t", "id", "x", "y", "vx", "vy"]
EXPERIMENT_COLUMNS = ["model", "rho", "eta", "n", "n_reps", "mean_va", "std_va", "mean_S", "std_S"]
TP2_TIMING_COLUMNS = ["n", "run", "elapsed_ns"]
MODELS = ("VICSEK", "VOTER")
BASE_DENSITIES = (2.0, 4.0, 8.0)
CLUSTER_DENSITIES = (1 / math.pi, 1 / (2 * math.pi), 1 / (3 * math.pi))
ALL_CLUSTER_DENSITIES = BASE_DENSITIES + CLUSTER_DENSITIES


def read_metadata(path: Path) -> dict[str, str]:
    metadata = {}
    with path.open(encoding="utf-8") as source:
        for line in source:
            if not line.startswith("#"):
                break
            for token in line.lstrip("#").split():
                if "=" in token:
                    key, value = token.split("=", 1)
                    metadata[key] = value
    return metadata


def _plain_rows(path: Path, expected: list[str], allow_extra: bool = False):
    if not path.exists():
        raise ValueError(f"No existe el archivo: {path}")
    with path.open(newline="", encoding="utf-8") as source:
        reader = csv.DictReader(source)
        actual = reader.fieldnames or []
        valid = all(column in actual for column in expected) if allow_extra else actual == expected
        if not valid:
            qualifier = "que incluya" if allow_extra else "exacto"
            raise ValueError(f"Encabezado inválido en {path}; se esperaba {qualifier} {expected}, se recibió {actual}")
        return list(reader)


def _commented_rows(path: Path, expected: list[str]):
    if not path.exists():
        raise ValueError(f"No existe el archivo: {path}")
    with path.open(newline="", encoding="utf-8") as source:
        reader = csv.DictReader(line for line in source if not line.startswith("#"))
        actual = reader.fieldnames or []
        if actual != expected:
            raise ValueError(f"Encabezado inválido en {path}; se esperaba {expected}, se recibió {actual}")
        return list(reader)


def read_observables(path: Path):
    metadata = read_metadata(path)
    parsed = []
    for row in _commented_rows(path, OBSERVABLE_COLUMNS):
        try:
            parsed.append({"t": int(row["t"]), "polarization": float(row["polarization"]),
                           "cluster_fraction": float(row["cluster_fraction"])})
        except (TypeError, ValueError) as error:
            raise ValueError(f"Fila inválida en {path}: {row}") from error
    if not parsed:
        raise ValueError(f"El archivo no contiene observables: {path}")
    if any(parsed[i]["t"] <= parsed[i - 1]["t"] for i in range(1, len(parsed))):
        raise ValueError(f"Los tiempos deben ser estrictamente crecientes en {path}")
    return metadata, parsed


def read_trajectory(path: Path):
    metadata = read_metadata(path)
    frames = defaultdict(list)
    for row in _commented_rows(path, TRAJECTORY_COLUMNS):
        try:
            frames[int(row["t"])].append({"id": int(row["id"]), "x": float(row["x"]),
                                           "y": float(row["y"]), "vx": float(row["vx"]),
                                           "vy": float(row["vy"])})
        except (TypeError, ValueError) as error:
            raise ValueError(f"Fila inválida en {path}: {row}") from error
    if not frames:
        raise ValueError(f"El archivo no contiene frames: {path}")
    times = sorted(frames)
    expected_n = int(metadata["n"]) if "n" in metadata else len(frames[times[0]])
    reference_ids = {row["id"] for row in frames[times[0]]}
    for t in times:
        ids = [row["id"] for row in frames[t]]
        if len(ids) != expected_n or len(set(ids)) != expected_n:
            raise ValueError(f"Frame t={t} incompleto o con ids duplicados en {path}")
        if set(ids) != reference_ids:
            raise ValueError(f"El conjunto de partículas cambia en t={t} en {path}")
        frames[t].sort(key=lambda row: row["id"])
    return metadata, [(t, frames[t]) for t in times]


def read_experiments(paths: list[Path], required_models: tuple[str, ...] | None = None):
    """Lee y valida uno o mas CSV de experimentos.

    required_models restringe que modelos deben estar presentes -- por default (None) exige
    los dos (MODELS = VICSEK y VOTER), como en el barrido real del informe. Para comparaciones
    puntuales que solo corrieron un modelo (ej. contra otro grupo que solo mandan "standard"),
    se puede pasar required_models=("VICSEK",) y no va a exigir filas de VOTER que no existen.
    """
    parsed_rows = []
    seen = set()
    for path in paths:
        for row in _plain_rows(path, EXPERIMENT_COLUMNS):
            try:
                parsed = {"model": row["model"].upper(), "rho": float(row["rho"]),
                          "eta": float(row["eta"]), "n": int(row["n"]),
                          "n_reps": int(row["n_reps"]), "mean_va": float(row["mean_va"]),
                          "std_va": float(row["std_va"]), "mean_S": float(row["mean_S"]),
                          "std_S": float(row["std_S"])}
            except (TypeError, ValueError) as error:
                raise ValueError(f"Fila inválida en {path}: {row}") from error
            if parsed["model"] not in MODELS:
                raise ValueError(f"Modelo desconocido en {path}: {parsed['model']}")
            key = (parsed["model"], parsed["rho"], parsed["eta"])
            if key in seen:
                raise ValueError(f"Combinación duplicada (model,rho,eta)={key}")
            seen.add(key)
            parsed_rows.append(parsed)
    if not parsed_rows:
        raise ValueError("Los archivos de experimentos no contienen filas")
    expected_models = MODELS if required_models is None else required_models
    missing_models = set(expected_models) - {row["model"] for row in parsed_rows}
    if missing_models:
        raise ValueError(f"Faltan modelos en los experimentos: {sorted(missing_models)}")
    return parsed_rows


def rows_for_density(rows, expected_density: float, tolerance: float = 5e-6):
    return [row for row in rows if math.isclose(row["rho"], expected_density, abs_tol=tolerance)]


def require_densities(rows, densities):
    missing = [rho for rho in densities if not rows_for_density(rows, rho)]
    if missing:
        raise ValueError("Faltan densidades requeridas: " + ", ".join(f"{rho:.6f}" for rho in missing))


def read_tp2_timings(path: Path):
    raw_rows = _plain_rows(path, TP2_TIMING_COLUMNS)
    try:
        rows = [(int(row["n"]), int(row["elapsed_ns"])) for row in raw_rows]
    except (TypeError, ValueError) as error:
        raise ValueError(f"Fila inválida en {path}") from error
    if not rows:
        raise ValueError(f"El archivo no contiene mediciones: {path}")
    return rows


def read_tp1_timings(path: Path, series: str | None = None):
    raw_rows = _plain_rows(path, ["n", "elapsed_ns"], allow_extra=True)
    if series is not None:
        if raw_rows and "series" not in raw_rows[0]:
            raise ValueError(f"El archivo TP1 no tiene columna series: {path}")
        raw_rows = [row for row in raw_rows if row["series"] == series]
    try:
        rows = [(int(row["n"]), int(row["elapsed_ns"])) for row in raw_rows]
    except (TypeError, ValueError) as error:
        raise ValueError(f"Fila inválida en {path}") from error
    if not rows:
        raise ValueError(f"El archivo no contiene mediciones: {path}")
    return rows
