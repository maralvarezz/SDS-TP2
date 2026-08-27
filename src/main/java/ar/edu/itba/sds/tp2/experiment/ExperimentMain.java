package ar.edu.itba.sds.tp2.experiment;

import ar.edu.itba.sds.tp2.config.FlockingModel;
import ar.edu.itba.sds.tp2.io.ExperimentResultsWriter;
import ar.edu.itba.sds.tp2.observable.SteadyStateDetector;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;

/**
 * Punto de entrada del barrido masivo de experimentos (puntos c, d, e, f del enunciado). A
 * diferencia de Main (que corre UNA combinacion para explorar/debuggear a mano con
 * viz/plot_observables.py), esto corre TODAS las combinaciones necesarias para armar los
 * graficos finales y escribe el CSV agregado que consume viz/.
 * <p>
 * Las constantes DEFAULT_* son la definicion "de verdad" del experimento -- se editan directo
 * aca si el equipo decide cambiar la grilla de eta, rc, etc. Para probar rapido sin editar
 * codigo hay flags opcionales por linea de comandos:
 * <pre>
 *   --quick               steps=300, repetitions=1, eta en 6 puntos en vez de 11 (prueba end-to-end rapida)
 *   --steps=N             pisa DEFAULT_STEPS
 *   --repetitions=N       pisa DEFAULT_REPETITIONS
 *   --initial-state=MODE  random o colliding
 *   --model=MODEL         VICSEK, VOTER o ALL
 *   --densities=SCOPE     polarization, cluster o all (default all) -- para correr solo uno de
 *                         los dos barridos, ej. al comparar contra otro grupo que solo corre
 *                         rho=2,4,8 y no le interesan las densidades de cluster
 *   --stationary-from=N   en vez de usar el SteadyStateDetector, promedia siempre desde el paso
 *                         N fijo (equivalente al --stationary-from + --no-dynamic de otros
 *                         grupos que no usan deteccion dinamica) -- pensado para comparaciones
 *                         en igualdad de condiciones, NO para el barrido "de verdad" del informe
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

    private enum DensityScope {
        POLARIZATION, CLUSTER, ALL
    }

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
        List<FlockingModel> models = MODELS;
        ExperimentRunner.InitialStateMode initialStateMode = ExperimentRunner.InitialStateMode.RANDOM;
        DensityScope densityScope = DensityScope.ALL;
        OptionalInt stationaryFrom = OptionalInt.empty();

        for (String arg : args) {
            if (arg.equals("--quick")) {
                steps = 300;
                repetitions = 1;
                etas = QUICK_ETAS;
            } else if (arg.startsWith("--steps=")) {
                steps = Integer.parseInt(arg.substring("--steps=".length()));
            } else if (arg.startsWith("--repetitions=")) {
                repetitions = Integer.parseInt(arg.substring("--repetitions=".length()));
            } else if (arg.startsWith("--initial-state=")) {
                initialStateMode = parseInitialStateMode(arg.substring("--initial-state=".length()));
            } else if (arg.startsWith("--model=")) {
                models = parseModels(arg.substring("--model=".length()));
            } else if (arg.startsWith("--densities=")) {
                densityScope = parseDensityScope(arg.substring("--densities=".length()));
            } else if (arg.startsWith("--stationary-from=")) {
                stationaryFrom = OptionalInt.of(Integer.parseInt(arg.substring("--stationary-from=".length())));
            } else {
                throw new IllegalArgumentException(
                        "Argumento desconocido: " + arg
                                + " (usar --quick, --steps=N, --repetitions=N, --initial-state=random|colliding,"
                                + " --model=VICSEK|VOTER|ALL, --densities=polarization|cluster|all, --stationary-from=N)");
            }
        }

        System.out.printf(
                "Config: steps=%d repetitions=%d etas=%d valores initialState=%s models=%s densities=%s stationaryFrom=%s%n",
                steps, repetitions, etas.size(), initialStateMode, models, densityScope,
                stationaryFrom.isPresent() ? stationaryFrom.getAsInt() : "detector dinamico");
        ExperimentRunner runner = new ExperimentRunner(
                L, RC, V0, DT, steps, repetitions, STEADY_STATE_DETECTOR, initialStateMode, stationaryFrom);

        if (densityScope == DensityScope.POLARIZATION || densityScope == DensityScope.ALL) {
            System.out.println("=== Barrido de polarizacion (rho=2,4,8) ===");
            List<ExperimentPoint> polarizationResults = runner.run(models, POLARIZATION_DENSITIES, etas);
            Path polarizationOut = Path.of("output/experiments_polarization.csv");
            ExperimentResultsWriter.write(polarizationOut, polarizationResults);
            System.out.println(polarizationResults.size() + " combinaciones -> " + polarizationOut);
        }

        if (densityScope == DensityScope.CLUSTER || densityScope == DensityScope.ALL) {
            System.out.println("=== Barrido de clusters (rho=1/pi, 1/2pi, 1/3pi) ===");
            List<ExperimentPoint> clusterResults = runner.run(models, CLUSTER_DENSITIES, etas);
            Path clusterOut = Path.of("output/experiments_clusters.csv");
            ExperimentResultsWriter.write(clusterOut, clusterResults);
            System.out.println(clusterResults.size() + " combinaciones -> " + clusterOut);
        }
    }

    private static ExperimentRunner.InitialStateMode parseInitialStateMode(String value) {
        return switch (value.toLowerCase()) {
            case "random" -> ExperimentRunner.InitialStateMode.RANDOM;
            case "colliding", "colliding-clusters", "two-flocks" -> ExperimentRunner.InitialStateMode.COLLIDING;
            default -> throw new IllegalArgumentException(
                    "initial-state debe ser random o colliding, recibido: " + value);
        };
    }

    private static List<FlockingModel> parseModels(String value) {
        if (value.equalsIgnoreCase("all")) {
            return MODELS;
        }
        return List.of(FlockingModel.valueOf(value.toUpperCase()));
    }

    private static DensityScope parseDensityScope(String value) {
        return switch (value.toLowerCase()) {
            case "polarization", "polarizacion" -> DensityScope.POLARIZATION;
            case "cluster", "clusters" -> DensityScope.CLUSTER;
            case "all" -> DensityScope.ALL;
            default -> throw new IllegalArgumentException(
                    "densities debe ser polarization, cluster o all, recibido: " + value);
        };
    }
}
