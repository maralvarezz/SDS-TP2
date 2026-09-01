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
 * Punto (g) del enunciado: medir tiempos de ejecucion del CIM para compararlos con TP1.
 * <p>
 * Para que la comparacion en escala log-log tenga sentido, se barre N en un rango amplio
 * (10 a 5000, espaciado aprox. logaritmico) manteniendo la DENSIDAD fija en vez del lado L fijo:
 * si L quedara fijo, al aumentar N tambien aumentaria la densidad de particulas, y el tiempo del
 * CIM dejaria de reflejar solo el efecto de N (se sumaria el efecto de mas vecinos por particula).
 * Con densidad fija, L y M escalan junto con N (L = L0*sqrt(N/N0), M = floor(M0*L/L0)) y el largo
 * de celda se mantiene siempre por encima del minimo requerido.
 * <p>
 * El largo de celda minimo no es simplemente rc: como estas particulas tienen radio (no son
 * puntuales, a diferencia del escenario de bandadas de TP2), ConfigValidator.validateGeometry de
 * TP1 exige cellLength >= rc + 2*radiusMax = 1 + 2*0.26 = 1.52 (la interaccion se mide entre
 * superficies, no entre centros). Los valores base (L0=20, M0=13, N0=100, rc=1) dan un largo de
 * celda de ~1.54 en el punto de referencia, el M mas grande que TP1 acepta para L=20 con estos
 * radios (confirmado por el propio mensaje de error de TP1: "M maximo permitido: 13").
 * <p>
 * Llama a CellIndexMethod.findNeighbours() directo (lo mismo que envuelve
 * SimulationEngine.findNeighbours() puertas adentro) para no meter overhead de mas en la
 * medicion.
 * <p>
 * Antes de medir en serio se hace un WARM-UP: se llama a findNeighbours varios miles de veces
 * sobre un sistema descartable, sin registrar esos tiempos. Al correr las mediciones dentro de
 * un unico proceso Java (a diferencia de TP1, que lanza una JVM nueva por corrida), las primeras
 * llamadas caen en modo interpretado -- el JIT de HotSpot todavia no compilo el codigo caliente --
 * lo que antes se vio como tiempos anomalos (incluso no monotonos) justo en los N mas chicos. El
 * warm-up hace que esa transicion ya haya pasado antes de que arranque la medicion real, para que
 * la curva completa (desde N=10) sea representativa.
 * <p>
 * N_VALUES se densifico (10 a 5000, 22 puntos) para que la comparacion log-log con TP1 tenga mas
 * puntos donde importa (N&gt;=500, la zona donde TP1 vs TP2 son comparables -- ver
 * viz/compare_cim_timing.py y su flag --n-min). Los puntos chicos (10-200) se mantienen para no
 * perder esa parte de la curva, aunque en la comparacion con TP1 se filtren con --n-min.
 * <p>
 * Los 10 puntos de N&gt;=500 estan espaciados LOGARITMICAMENTE: son N0*r^k con razon
 * r=10^(1/9) (~1.2915), o sea 0.111 decadas parejas entre cada uno en el eje log-log del grafico
 * de comparacion (ver viz/compare_cim_timing.py).
 */
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
                1, false, "python3", Path.of("unused"), Path.of("unused"), Path.of("unused")
        );
        StaticSystem staticSystem = ParticleGenerator.generateStaticSystem(config);
        return ParticleGenerator.generateDynamicParticles(staticSystem, config);
    }
}
