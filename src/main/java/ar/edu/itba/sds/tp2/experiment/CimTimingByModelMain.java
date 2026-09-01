package ar.edu.itba.sds.tp2.experiment;

import ar.edu.itba.sds.algorithm.CellIndexMethod;
import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.tp2.config.FlockingConfig;
import ar.edu.itba.sds.tp2.config.FlockingModel;
import ar.edu.itba.sds.tp2.engine.SimulationEngine;
import ar.edu.itba.sds.tp2.generator.FlockingParticleGenerator;
import ar.edu.itba.sds.tp2.rule.DirectionRule;
import ar.edu.itba.sds.tp2.rule.VicsekAverageRule;
import ar.edu.itba.sds.tp2.rule.VoterRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Set;

/**
 * Version "por modelo" del punto (g): en vez de medir el CIM sobre particulas sinteticas
 * generadas aparte (como CimTimingMain, que ademas escala L/M a densidad fija para poder llegar
 * a N grandes con las particulas CON RADIO de TP1), esta clase mide el CIM DENTRO de una
 * simulacion de bandadas REAL -- misma L=10 fija, mismo rc, mismas rho=2,4,8 (N=200,400,800) que
 * se usaron en los puntos (a-f) del enunciado -- para los DOS modelos por separado. Es lo que
 * pide literal el punto (g): "Tomar algunas simulaciones que tengan un numero de particulas
 * similar a las estudiadas" -- ac&aacute; son LAS MISMAS simulaciones (mismas rho, mismo L),
 * no una densidad inventada aparte.
 * <p>
 * Arranca con un warm-up del CIM solo (igual que CimTimingMain), despues por cada (modelo, rho)
 * corre BURN_IN_STEPS pasos de simulacion real (sin medir, para llegar a un estado dinamico
 * representativo y terminar de calentar el JIT de la regla de direccion de ese modelo) y recien
 * ahi mide MEASURED_STEPS pasos, cronometrando SOLO engine.findNeighbours() (no step() completo)
 * en cada uno.
 */
public final class CimTimingByModelMain {

    private static final double L = 10.0;
    private static final double RC = 1.0;
    private static final double V0 = 0.03;
    private static final double DT = 1.0;
    private static final double ETA = 1.0;
    private static final List<Double> RHO_VALUES = List.of(2.0, 4.0, 8.0);
    private static final List<FlockingModel> MODELS = List.of(FlockingModel.VICSEK, FlockingModel.VOTER);
    private static final int BURN_IN_STEPS = 50;
    private static final int MEASURED_STEPS = 10;
    private static final int WARMUP_ITERATIONS = 5000;

    private CimTimingByModelMain() {
    }

    public static void main(String[] args) throws IOException {
        warmUpCim();

        List<String> lines = new ArrayList<>();
        lines.add("model,rho,n,run,l,m,elapsed_ns");

        for (FlockingModel model : MODELS) {
            for (double rho : RHO_VALUES) {
                FlockingConfig config = FlockingConfig.ofDensity(
                        rho, L, RC, V0, DT, ETA, BURN_IN_STEPS + MEASURED_STEPS, model,
                        OptionalLong.of(2000L + Math.round(rho * 100)));
                DirectionRule rule = ruleFor(model);
                Random random = new Random(config.seed().getAsLong());
                SimulationEngine engine = new SimulationEngine(config, rule, random);

                List<Particle> state = FlockingParticleGenerator.generateInitialState(config);
                for (int step = 0; step < BURN_IN_STEPS; step++) {
                    Map<Integer, Set<Integer>> neighbours = engine.findNeighbours(state);
                    state = engine.step(state, neighbours);
                }

                for (int run = 0; run < MEASURED_STEPS; run++) {
                    long start = System.nanoTime();
                    Map<Integer, Set<Integer>> neighbours = engine.findNeighbours(state);
                    long elapsed = System.nanoTime() - start;

                    lines.add(String.format(
                            Locale.US, "%s,%.4f,%d,%d,%.4f,%d,%d",
                            model, rho, config.n(), run, config.l(), config.cellsPerSide(), elapsed));

                    state = engine.step(state, neighbours);
                }
                System.out.println(model + " rho=" + rho + " (n=" + config.n() + ") listo ("
                        + MEASURED_STEPS + " pasos medidos, " + BURN_IN_STEPS + " de burn-in)");
            }
        }

        Path outputFile = Path.of("output/cim_timing_tp2_by_model.csv");
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.write(outputFile, lines);
        System.out.println("Tiempos escritos en " + outputFile.toAbsolutePath());
    }

    private static DirectionRule ruleFor(FlockingModel model) {
        return switch (model) {
            case VICSEK -> new VicsekAverageRule();
            case VOTER -> new VoterRule();
        };
    }

    private static void warmUpCim() {
        FlockingConfig config = FlockingConfig.ofDensity(
                4.0, L, RC, V0, DT, ETA, 1, FlockingModel.VICSEK, OptionalLong.of(1L));
        List<Particle> particles = FlockingParticleGenerator.generateInitialState(config);

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            CellIndexMethod.findNeighbours(particles, config.l(), config.cellsPerSide(), config.rc(), true);
        }
        System.out.println("Warm-up completo (" + WARMUP_ITERATIONS + " llamadas)");
    }
}
