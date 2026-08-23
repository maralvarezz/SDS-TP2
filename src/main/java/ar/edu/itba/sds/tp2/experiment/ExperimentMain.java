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
 * Las constantes de abajo SON la definicion del experimento -- se editan directo aca si el
 * equipo decide cambiar la grilla de eta, la cantidad de repeticiones, rc, etc. Las dos listas
 * de densidad son intencionalmente distintas: {2,4,8} para el estudio general (polarizacion,
 * puntos b/c), {1/pi, 1/(2pi), 1/(3pi)} para el estudio de clusters (punto d y el scatter
 * va-vs-S del punto e), segun la aclaracion de la catedra -- rc es el MISMO en los dos casos, no
 * hace falta cambiarlo entre estudios.
 * <p>
 * OJO CON EL TIEMPO DE CORRIDA: esto son 2 modelos x 3 densidades x 11 etas x repetitions
 * corridas COMPLETAS por cada lista de densidades (osea el doble, una vez para polarizacion y
 * otra para clusters) -- con los defaults de aca abajo son bastantes simulaciones. Para una
 * primera prueba rapida, bajen REPETITIONS a 1-2 y STEPS a algo como 300-500 antes de lanzar la
 * corrida "de verdad" para los datos finales del informe.
 */
public final class ExperimentMain {

    private static final double L = 10.0;
    private static final double RC = 1.0;
    private static final double V0 = 0.03;
    private static final double DT = 1.0;
    private static final int STEPS = 1500;
    private static final int REPETITIONS = 5;

    private static final List<Double> POLARIZATION_DENSITIES = List.of(2.0, 4.0, 8.0);
    private static final List<Double> CLUSTER_DENSITIES = List.of(1 / Math.PI, 1 / (2 * Math.PI), 1 / (3 * Math.PI));
    private static final List<Double> ETAS = List.of(0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0);
    private static final List<FlockingModel> MODELS = List.of(FlockingModel.VICSEK, FlockingModel.VOTER);

    // Ventana de 50 pasos, pendiente < 0.001, 5 ventanas seguidas por debajo del umbral.
    private static final SteadyStateDetector STEADY_STATE_DETECTOR = new SteadyStateDetector(50, 1e-3, 5);

    private ExperimentMain() {
    }

    public static void main(String[] args) throws IOException {
        ExperimentRunner runner = new ExperimentRunner(L, RC, V0, DT, STEPS, REPETITIONS, STEADY_STATE_DETECTOR);

        System.out.println("Corriendo barrido de polarizacion (rho=2,4,8)...");
        List<ExperimentPoint> polarizationResults = runner.run(MODELS, POLARIZATION_DENSITIES, ETAS);
        Path polarizationOut = Path.of("output/experiments_polarization.csv");
        ExperimentResultsWriter.write(polarizationOut, polarizationResults);
        System.out.println(polarizationResults.size() + " combinaciones -> " + polarizationOut);

        System.out.println("Corriendo barrido de clusters (rho=1/pi, 1/2pi, 1/3pi)...");
        List<ExperimentPoint> clusterResults = runner.run(MODELS, CLUSTER_DENSITIES, ETAS);
        Path clusterOut = Path.of("output/experiments_clusters.csv");
        ExperimentResultsWriter.write(clusterOut, clusterResults);
        System.out.println(clusterResults.size() + " combinaciones -> " + clusterOut);
    }
}
