package ar.edu.itba.sds.tp2;

import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.tp2.config.FlockingConfig;
import ar.edu.itba.sds.tp2.config.FlockingConfigLoader;
import ar.edu.itba.sds.tp2.engine.SimulationEngine;
import ar.edu.itba.sds.tp2.generator.FlockingParticleGenerator;
import ar.edu.itba.sds.tp2.io.ObservablesWriter;
import ar.edu.itba.sds.tp2.io.TrajectoryWriter;
import ar.edu.itba.sds.tp2.observable.ClusterAnalyzer;
import ar.edu.itba.sds.tp2.observable.Polarization;
import ar.edu.itba.sds.tp2.rule.DirectionRule;
import ar.edu.itba.sds.tp2.rule.VicsekAverageRule;
import ar.edu.itba.sds.tp2.rule.VoterRule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Corrida de punta a punta: arma estado(0), corre `steps` pasos con la regla configurada
 * (Vicsek o votante), y en cada paso mide va y S sobre estado(t) antes de avanzar a estado(t+1).
 * Al final escribe todo a output/observables.txt.
 * <p>
 * El objetivo por ahora es puramente exploratorio: graficar va(t) y S(t) con
 * viz/plot_observables.py para elegir a ojo, mirando el grafico, a partir de que t el sistema
 * entra en estado estacionario (punto b del enunciado). Todavia no es el runner que barre
 * modelo x densidad x eta -- eso es la proxima capa.
 * <p>
 * Acepta un flag extra, --initial-state=random|colliding (o --generator=random|clusters,
 * por compatibilidad; default random), que NO pasa por
 * FlockingConfigLoader (se saca de los args antes) porque no es un parametro fisico del
 * enunciado -- es solo para elegir el estado inicial:
 * - random (default): el de siempre, N particulas dispersas al azar en toda la caja. Es lo que
 *   usan ExperimentRunner y CimTimingMain (sin pasar por este flag, asi que no se ven afectados).
 * - clusters: SOLO para demos/animaciones -- arma dos masas compactas que arrancan viajando una
 *   hacia la otra (ver FlockingParticleGenerator.generateCollidingClusters), para que
 *   viz/animate_trajectory.py muestre un choque/fusion de bandadas bien visible en vez de
 *   particulas dispersas desde el arranque.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws IOException {
        String generator = "random";
        List<String> remainingArgs = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("--generator=")) {
                generator = normalizeGenerator(arg.substring("--generator=".length()));
            } else if (arg.startsWith("--initial-state=")) {
                generator = normalizeGenerator(arg.substring("--initial-state=".length()));
            } else {
                remainingArgs.add(arg);
            }
        }

        FlockingConfig config = FlockingConfigLoader.load(remainingArgs.toArray(new String[0]));
        DirectionRule rule = ruleFor(config);
        Random random = config.seed().isPresent() ? new Random(config.seed().getAsLong()) : new Random();

        SimulationEngine engine = new SimulationEngine(config, rule, random);
        List<Particle> state = generator.equals("clusters")
                ? FlockingParticleGenerator.generateCollidingClusters(config)
                : FlockingParticleGenerator.generateInitialState(config);

        Path trajectoryFile = Path.of("output/trajectory.txt");
        List<ObservablesWriter.Row> rows = new ArrayList<>(config.steps() + 1);
        try (TrajectoryWriter trajectoryWriter = new TrajectoryWriter(trajectoryFile, config)) {
            for (int t = 0; t <= config.steps(); t++) {
                Map<Integer, Set<Integer>> neighbours = engine.findNeighbours(state);
                double va = Polarization.compute(state);
                double s = ClusterAnalyzer.giantComponentFraction(neighbours);
                rows.add(new ObservablesWriter.Row(t, va, s));
                trajectoryWriter.writeFrame(t, state);

                if (t < config.steps()) {
                    state = engine.step(state, neighbours);
                }
            }
        }

        Path outputFile = Path.of("output/observables.txt");
        ObservablesWriter.write(outputFile, config, rows);

        double rho = config.n() / (config.l() * config.l());
        System.out.printf("Corrida terminada: modelo=%s rho=%.2f n=%d eta=%.3f steps=%d generador=%s%n",
                config.model(), rho, config.n(), config.eta(), config.steps(), generator);
        System.out.println("Observables escritos en " + outputFile.toAbsolutePath());
        System.out.println("Trayectoria escrita en " + trajectoryFile.toAbsolutePath());
        System.out.println("Graficar con: python3 viz/plot_observables.py " + outputFile);
        System.out.println("Animar con: python3 viz/animate_trajectory.py " + trajectoryFile);
    }

    private static DirectionRule ruleFor(FlockingConfig config) {
        return switch (config.model()) {
            case VICSEK -> new VicsekAverageRule();
            case VOTER -> new VoterRule();
        };
    }

    private static String normalizeGenerator(String value) {
        return switch (value.toLowerCase()) {
            case "random" -> "random";
            case "clusters", "colliding", "colliding-clusters", "two-flocks" -> "clusters";
            default -> throw new IllegalArgumentException(
                    "--initial-state debe ser 'random' o 'colliding', recibido: " + value);
        };
    }
}
