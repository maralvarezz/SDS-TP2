package ar.edu.itba.sds.tp2.experiment;

import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.tp2.config.FlockingConfig;
import ar.edu.itba.sds.tp2.config.FlockingModel;
import ar.edu.itba.sds.tp2.engine.SimulationEngine;
import ar.edu.itba.sds.tp2.generator.FlockingParticleGenerator;
import ar.edu.itba.sds.tp2.observable.ClusterAnalyzer;
import ar.edu.itba.sds.tp2.observable.Polarization;
import ar.edu.itba.sds.tp2.observable.SteadyStateDetector;
import ar.edu.itba.sds.tp2.rule.DirectionRule;
import ar.edu.itba.sds.tp2.rule.VicsekAverageRule;
import ar.edu.itba.sds.tp2.rule.VoterRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Set;

/**
 * Herramienta temporal de verificacion (no forma parte de la entrega): repite EXACTAMENTE la
 * misma logica que ExperimentRunner.runCombination (mismos parametros, misma formula de semilla,
 * mismo SteadyStateDetector) para UNA sola combinacion, pero imprime el resultado de cada
 * repeticion individualmente en vez de solo el promedio. Sirve para confirmar que un std_va
 * grande en el CSV agregado viene de verdad de que las repeticiones cayeron en niveles distintos
 * (fluctuacion real entre corridas independientes, ej. cerca de una transicion) y no de un bug.
 * <p>
 * Uso: --model=VICSEK|VOTER --rho=R --eta=E (mismos L/rc/v0/dt/steps/detector que ExperimentMain).
 */
public final class SpotCheckMain {

    private static final double L = 10.0;
    private static final double RC = 1.0;
    private static final double V0 = 0.03;
    private static final double DT = 1.0;
    private static final int STEPS = 1500;
    private static final int REPETITIONS = 5;

    private static final SteadyStateDetector STEADY_STATE_DETECTOR =
            new SteadyStateDetector(50, 1e-3, 5, 0.05, 0.1);

    private SpotCheckMain() {
    }

    public static void main(String[] args) {
        FlockingModel model = null;
        Double rho = null;
        Double eta = null;
        for (String arg : args) {
            if (arg.startsWith("--model=")) {
                model = FlockingModel.valueOf(arg.substring("--model=".length()));
            } else if (arg.startsWith("--rho=")) {
                rho = Double.parseDouble(arg.substring("--rho=".length()));
            } else if (arg.startsWith("--eta=")) {
                eta = Double.parseDouble(arg.substring("--eta=".length()));
            }
        }
        if (model == null || rho == null || eta == null) {
            throw new IllegalArgumentException("Usar --model=VICSEK|VOTER --rho=R --eta=E");
        }

        System.out.printf("=== SpotCheck modelo=%s rho=%.4f eta=%.3f (L=%.1f steps=%d repetitions=%d) ===%n",
                model, rho, eta, L, STEPS, REPETITIONS);

        List<Double> vaSamples = new ArrayList<>();
        List<Double> sSamples = new ArrayList<>();

        for (int rep = 0; rep < REPETITIONS; rep++) {
            long seed = Objects.hash(model.name(), rho, eta, rep);
            FlockingConfig config = FlockingConfig.ofDensity(
                    rho, L, RC, V0, DT, eta, STEPS, model, OptionalLong.of(seed));

            RunResult result = runSingle(config);
            OptionalInt stationaryStart = STEADY_STATE_DETECTOR.detect(result.va());
            int start;
            String how;
            if (stationaryStart.isPresent()) {
                start = stationaryStart.getAsInt();
                how = "detectado en t=" + start;
            } else {
                start = result.va().size() / 2;
                how = "FALLBACK (mitad final, t=" + start + ")";
            }

            double va = average(result.va(), start);
            double s = average(result.s(), start);
            vaSamples.add(va);
            sSamples.add(s);

            System.out.printf("  rep=%d seed=%d n=%d -> va=%.4f S=%.4f  [%s]%n",
                    rep, seed, config.n(), va, s, how);
        }

        System.out.printf("Resumen: va=%.4f±%.4f  S=%.4f±%.4f%n",
                mean(vaSamples), stdDev(vaSamples), mean(sSamples), stdDev(sSamples));
    }

    private static RunResult runSingle(FlockingConfig config) {
        Random random = new Random(config.seed().getAsLong());
        DirectionRule rule = switch (config.model()) {
            case VICSEK -> new VicsekAverageRule();
            case VOTER -> new VoterRule();
        };
        SimulationEngine engine = new SimulationEngine(config, rule, random);
        List<Particle> state = FlockingParticleGenerator.generateInitialState(config);

        List<Double> va = new ArrayList<>(config.steps() + 1);
        List<Double> s = new ArrayList<>(config.steps() + 1);
        for (int t = 0; t <= config.steps(); t++) {
            Map<Integer, Set<Integer>> neighbours = engine.findNeighbours(state);
            va.add(Polarization.compute(state));
            s.add(ClusterAnalyzer.giantComponentFraction(neighbours));
            if (t < config.steps()) {
                state = engine.step(state, neighbours);
            }
        }
        return new RunResult(va, s);
    }

    private record RunResult(List<Double> va, List<Double> s) {
    }

    private static double average(List<Double> series, int fromIndexInclusive) {
        double sum = 0;
        int count = 0;
        for (int i = fromIndexInclusive; i < series.size(); i++) {
            sum += series.get(i);
            count++;
        }
        return count == 0 ? Double.NaN : sum / count;
    }

    private static double mean(List<Double> values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private static double stdDev(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }
        double m = mean(values);
        double sumSq = 0;
        for (double value : values) {
            sumSq += (value - m) * (value - m);
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }
}
