package ar.edu.itba.sds.tp2.experiment;

import ar.edu.itba.sds.tp2.config.FlockingModel;
import ar.edu.itba.sds.tp2.io.ExperimentResultsWriter;
import ar.edu.itba.sds.tp2.observable.SteadyStateDetector;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Punto de entrada del barrido masivo de experimentos (puntos c, d, e, f del enunciado). A
 * diferencia de Main (que corre UNA combinacion para explorar/debuggear a mano con
 * viz/plot_observables.py), esto corre TODAS las combinaciones necesarias para armar los
 * graficos finales y escribe el CSV agregado que consume viz/.
 * <p>
 * Las constantes DEFAULT_* son la definicion "de verdad" del experimento -- se editan directo
 * aca si el equipo decide cambiar la grilla de eta, rc, etc. Para probar rapido sin editar
 * codigo hay 3 flags opcionales por linea de comandos:
 * <pre>
 *   --quick               steps=300, repetitions=1, eta en 6 puntos en vez de 11 (prueba end-to-end rapida)
 *   --steps=N             pisa DEFAULT_STEPS
 *   --repetitions=N       pisa DEFAULT_REPETITIONS
 * </pre>
 * Las dos listas de densidad son intencionalmente distintas: {2,4,8} para el estudio general
 * (polarizacion, puntos b/c), {1/pi, 1/(2pi), 1/(3pi)} para el estudio de clusters (punto d y el
 * scatter va-vs-S del punto e), segun la aclaracion de la catedra -- rc es el MISMO en los dos
 * casos, no hace falta cambiarlo entre estudios.
 */
public final class ExperimentMain {

    private static final double L = 10.0;
    private static final double RC = 1.0;
    private static final double V0 = 0.03;
    private static final double DT = 1.0;
    private static final int DEFAULT_STEPS = 1500;
    private static final int DEFAULT_REPETITIONS = 5;

    private static final List<Double> POLARIZATION_DENSITIES = List.of(2.0, 4.0, 8.0);
    private static final List<Double> CLUSTER_DENSITIES = List.of(1 / Math.PI, 1 / (2 * Math.PI), 1 / (3 * Math.PI));
    private static final List<Double> DEFAULT_ETAS = List.of(0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0);
    private static final List<Double> QUICK_ETAS = List.of(0.0, 1.0, 2.0, 3.0, 4.0, 5.0);
    private static final List<FlockingModel> MODELS = List.of(FlockingModel.VICSEK, FlockingModel.VOTER);

    // Ventana de 50 pasos, pendiente < 0.001, 5 ventanas seguidas por debajo del umbral,
    // desvio de ventana < 0.05 y promedio de ventana a menos de 0.1 del promedio de la cola
    // final de la corrida (ver SteadyStateDetector para el porque de cada numero: calibrado
    // el 24/08 con dos casos reales, votante rho=8/eta=0 -- que revelo que pendiente+desvio
    // solos no alcanzan por las mesetas locales del voter model -- y vicsek rho=4/eta=2 -- que
    // dio el rango real de ruido a esperar en un caso con eta>0 genuino).
    private static final SteadyStateDetector STEADY_STATE_DETECTOR =
            new SteadyStateDetector(50, 1e-3, 5, 0.05, 0.1);

    private ExperimentMain() {
    }

    public static void main(String[] args) throws IOException {
        int steps = DEFAULT_STEPS;
        int repetitions = DEFAULT_REPETITIONS;
        List<Double> etas = DEFAULT_ETAS;

        for (String arg : args) {
            if (arg.equals("--quick")) {
                steps = 300;
                repetitions = 1;
                etas = QUICK_ETAS;
            } else if (arg.startsWith("--steps=")) {
                steps = Integer.parseInt(arg.substring("--steps=".length()));
            } else if (arg.startsWith("--repetitions=")) {
                repetitions = Integer.parseInt(arg.substring("--repetitions=".length()));
            } else {
                throw new IllegalArgumentException(
                        "Argumento desconocido: " + arg + " (usar --quick, --steps=N, --repetitions=N)");
            }
        }

        System.out.printf("Config: steps=%d repetitions=%d etas=%d valores%n", steps, repetitions, etas.size());
        ExperimentRunner runner = new ExperimentRunner(L, RC, V0, DT, steps, repetitions, STEADY_STATE_DETECTOR);

        System.out.println("=== Barrido de polarizacion (rho=2,4,8) ===");
        List<ExperimentPoint> polarizationResults = runner.run(MODELS, POLARIZATION_DENSITIES, etas);
        Path polarizationOut = Path.of("output/experiments_polarization.csv");
        ExperimentResultsWriter.write(polarizationOut, polarizationResults);
        System.out.println(polarizationResults.size() + " combinaciones -> " + polarizationOut);

        System.out.println("=== Barrido de clusters (rho=1/pi, 1/2pi, 1/3pi) ===");
        List<ExperimentPoint> clusterResults = runner.run(MODELS, CLUSTER_DENSITIES, etas);
        Path clusterOut = Path.of("output/experiments_clusters.csv");
        ExperimentResultsWriter.write(clusterOut, clusterResults);
        System.out.println(clusterResults.size() + " combinaciones -> " + clusterOut);
    }
}
