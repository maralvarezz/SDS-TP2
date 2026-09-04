package ar.edu.itba.sds.tp2.experiment;

import ar.edu.itba.sds.algorithm.CellIndexMethod;
import ar.edu.itba.sds.config.SimulationConfig;
import ar.edu.itba.sds.generator.ParticleGenerator;
import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.model.StaticSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

public final class CimTimingMain {

    private static final double BASE_L = 20.0;
    private static final int BASE_M = 13;
    private static final int DENSITY_REFERENCE_N = 100;
    private static final double RC = 1.0;
    private static final double RADIUS_MIN = 0.23;
    private static final double RADIUS_MAX = 0.26;
    private static final List<Integer> N_VALUES = List.of(
            10, 20, 50, 100, 200,
            500, 646, 834, 1077, 1391, 1797, 2321, 2997, 3871, 5000);
    private static final int RUNS_PER_VALUE = 10;
    private static final int WARMUP_N = 200;
    private static final int WARMUP_ITERATIONS = 5000;

    private CimTimingMain() {
    }

    public static void main(String[] args) throws IOException {
        warmUp();

        List<String> lines = new ArrayList<>();
        lines.add("n,run,l,m,elapsed_ns");

        for (int n : N_VALUES) {
            double l = lFor(n);
            int m = mFor(l);

            for (int run = 0; run < RUNS_PER_VALUE; run++) {
                List<Particle> particles = randomParticles(n, l, m, run);

                long start = System.nanoTime();
                CellIndexMethod.findNeighbours(particles, l, m, RC, true);
                long elapsed = System.nanoTime() - start;

                lines.add(String.format(Locale.US, "%d,%d,%.4f,%d,%d", n, run, l, m, elapsed));
            }
            System.out.println("n=" + n + " (l=" + l + ", m=" + m + ") listo (" + RUNS_PER_VALUE + " corridas)");
        }

        Path outputFile = Path.of("output/cim_timing_tp2.csv");
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.write(outputFile, lines);
        System.out.println("Tiempos escritos en " + outputFile.toAbsolutePath());
        System.out.println("Comparar con TP1: correr viz/time_analysis.py una vez por cada N con L y M escalados "
                + "a la misma densidad (ver README/instrucciones), y despues viz/compare_cim_timing.py");
    }

    private static void warmUp() {
        double l = lFor(WARMUP_N);
        int m = mFor(l);
        List<Particle> particles = randomParticles(WARMUP_N, l, m, -1);

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            CellIndexMethod.findNeighbours(particles, l, m, RC, true);
        }
        System.out.println("Warm-up completo (" + WARMUP_ITERATIONS + " llamadas con n=" + WARMUP_N + ")");
    }

    private static double lFor(int n) {
        return round4(BASE_L * Math.sqrt(n / (double) DENSITY_REFERENCE_N));
    }

    private static int mFor(double l) {
        return Math.max(1, (int) Math.floor(BASE_M * l / BASE_L + 1e-12));
    }

    private static double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private static List<Particle> randomParticles(int n, double l, int m, int seedOffset) {
        SimulationConfig config = new SimulationConfig(
                n, l, m, RC, RADIUS_MIN, RADIUS_MAX, true, OptionalLong.of(1000L + seedOffset), "random",
                Path.of("unused"), Path.of("unused"), Path.of("unused"), Path.of("unused"),
                1, false, "python3", Path.of("unused"), Path.of("unused"), Path.of("unused"),
                true, WARMUP_N, WARMUP_ITERATIONS,
                false, "n", List.of(), 1, Path.of("unused"), Path.of("unused")
        );
        StaticSystem staticSystem = ParticleGenerator.generateStaticSystem(config);
        return ParticleGenerator.generateDynamicParticles(staticSystem, config);
    }
}
