import math
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from data_io import (  # noqa: E402
    ALL_CLUSTER_DENSITIES,
    EXPERIMENT_COLUMNS,
    read_experiments,
    read_observables,
    read_tp2_timings,
    read_trajectory,
    require_densities,
)


class DataIoTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)

    def tearDown(self):
        self.directory.cleanup()

    def write(self, name, contents):
        path = self.root / name
        path.write_text(contents, encoding="utf-8")
        return path

    def test_observables_requires_exact_header(self):
        path = self.write("observables.txt", "# model=VICSEK\nt,polarization,S\n0,0.1,0.2\n")
        with self.assertRaisesRegex(ValueError, "Encabezado inválido"):
            read_observables(path)

    def test_trajectory_rejects_incomplete_frame(self):
        path = self.write(
            "trajectory.txt",
            "# model=VICSEK\n# n=2 l=10\nt,id,x,y,vx,vy\n0,1,1,2,0.1,0\n",
        )
        with self.assertRaisesRegex(ValueError, "incompleto"):
            read_trajectory(path)

    def test_experiments_reject_duplicate_combination(self):
        header = ",".join(EXPERIMENT_COLUMNS)
        row = "VICSEK,2,0,200,5,0.9,0.1,0.8,0.1"
        first = self.write("first.csv", f"{header}\n{row}\nVOTER,2,0,200,5,0.8,0.1,0.7,0.1\n")
        second = self.write("second.csv", f"{header}\n{row}\n")
        with self.assertRaisesRegex(ValueError, "duplicada"):
            read_experiments([first, second])

    def test_density_validation_accepts_rounded_inverse_pi(self):
        rows = [{"rho": rho} for rho in (2, 4, 8, 0.318310, 0.159155, 0.106103)]
        require_densities(rows, ALL_CLUSTER_DENSITIES)
        self.assertTrue(math.isclose(rows[-1]["rho"], 1 / (3 * math.pi), abs_tol=5e-6))

    def test_tp2_timing_header_is_exact(self):
        path = self.write("timing.csv", "n,run,elapsed_ns,extra\n20,0,100,1\n")
        with self.assertRaisesRegex(ValueError, "Encabezado inválido"):
            read_tp2_timings(path)


if __name__ == "__main__":
    unittest.main()
