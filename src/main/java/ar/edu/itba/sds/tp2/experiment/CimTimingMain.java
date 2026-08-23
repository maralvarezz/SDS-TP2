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

/**
 * Punto (g) del enunciado: medir tiempos de ejecucion del CIM para N similares a los usados en
 * TP1, para poder compararlos.
 * <p>
 * A proposito usa los MISMOS parametros que el estudio de tiempos de TP1
 * (viz/time_analysis.py --variable n --values 20..100 --m 10 --l 20 --rc 1, ver README de TP1) en
 * vez de la config de bandadas de TP2 (L=10) -- lo que se quiere medir es el rendimiento del CIM
 * en si mismo, aislado del escenario fisico de Vicsek, para que la comparacion contra los
 * numeros que ya tiene TP1 sea directa.
 * <p>
 * Llama a CellIndexMethod.findNeighbours() directo (lo mismo que envuelve
 * SimulationEngine.findNeighbours() puertas adentro) para no meter overhead de mas en la
 * medicion.
 */
public final class CimTimingMain {

    private static final double L = 20.0;
    private static final int M = 10;
    private static final double RC = 1.0;
    private static final List<Integer> N_VALUES = List.of(20, 30, 40, 50, 60, 70, 80, 90, 100);
    private static final int RUNS_PER_VALUE = 10;

    private CimTimingMain() {
    }

    public static void main(String[] args) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("n,run,elapsed_ns");

        for (int n : N_VALUES) {
            for (int run = 0; run < RUNS_PER_VALUE; run++) {
                List<Particle> particles = randomParticles(n, run);

                long start = System.nanoTime();
                CellIndexMethod.findNeighbours(particles, L, M, RC, true);
                long elapsed = System.nanoTime() - start;

                lines.add(String.format(Locale.US, "%d,%d,%d", n, run, elapsed));
            }
            System.out.println("n=" + n + " listo (" + RUNS_PER_VALUE + " corridas)");
        }

        Path outputFile = Path.of("output/cim_timing_tp2.csv");
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.write(outputFile, lines);
        System.out.println("Tiempos escritos en " + outputFile.toAbsolutePath());
        System.out.println("Comparar contra los tiempos de TP1 con los mismos parametros (l=20 m=10 rc=1)");
    }

    private static List<Particle> randomParticles(int n, int seedOffset) {
        SimulationConfig config = new SimulationConfig(
                n, L, M, RC, 0.0, 0.0, true, OptionalLong.of(1000L + seedOffset), "random",
                Path.of("unused"), Path.of("unused"), Path.of("unused"), Path.of("unused"),
                1, false, "python3", Path.of("unused"), Path.of("unused"), Path.of("unused")
        );
        StaticSystem staticSystem = ParticleGenerator.generateStaticSystem(config);
        return ParticleGenerator.generateDynamicParticles(staticSystem, config);
    }
}
