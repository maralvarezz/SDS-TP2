package ar.edu.itba.sds.tp2.generator;

import ar.edu.itba.sds.config.SimulationConfig;
import ar.edu.itba.sds.distance.DistanceCalculator;
import ar.edu.itba.sds.generator.ParticleGenerator;
import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.model.StaticParticle;
import ar.edu.itba.sds.model.StaticSystem;
import ar.edu.itba.sds.tp2.config.FlockingConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class FlockingParticleGenerator {
    private static final int MAX_ATTEMPTS_PER_PARTICLE = 100_000;

    private FlockingParticleGenerator() {
    }

    public static List<Particle> generateInitialState(FlockingConfig config) {
        StaticSystem staticSystem = ParticleGenerator.generateStaticSystem(toStaticConfig(config));
        return generateDynamicState(staticSystem, config);
    }

    private static List<Particle> generateDynamicState(StaticSystem staticSystem, FlockingConfig config) {
        Random random = config.seed().isPresent() ? new Random(config.seed().getAsLong()) : new Random();
        List<Particle> particles = new ArrayList<>(staticSystem.n());

        for (StaticParticle staticParticle : staticSystem.particles()) {
            Particle candidate = null;
            for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_PARTICLE; attempt++) {
                double bound = staticSystem.l() - 2 * staticParticle.radius();
                double x = staticParticle.radius() + random.nextDouble(bound);
                double y = staticParticle.radius() + random.nextDouble(bound);
                double theta = (random.nextDouble() * 2 - 1) * Math.PI;
                Particle next = new Particle(staticParticle.id(), x, y, staticParticle.radius(), theta);
                if (!overlaps(next, particles, true, staticSystem.l())) {
                    candidate = next;
                    break;
                }
            }
            if (candidate == null) {
                throw new IllegalStateException("No se pudo ubicar la particula " + staticParticle.id() + " sin superposicion");
            }
            particles.add(candidate);
        }

        return List.copyOf(particles);
    }

    private static boolean overlaps(Particle candidate, List<Particle> particles, boolean periodic, double l) {
        for (Particle particle : particles) {
            double distance = DistanceCalculator.centerDistance(candidate, particle, periodic, l);
            if (distance < candidate.radius() + particle.radius()) {
                return true;
            }
        }
        return false;
    }

    private static SimulationConfig toStaticConfig(FlockingConfig config) {
        return new SimulationConfig(
                config.n(),
                config.l(),
                config.cellsPerSide(),
                config.rc(),
                0.0,
                0.0,
                true,
                config.seed(),
                "random",
                Path.of("unused"),
                Path.of("unused"),
                Path.of("unused"),
                Path.of("unused"),
                1,
                false,
                "python3",
                Path.of("unused"),
                Path.of("unused"),
                Path.of("unused"),
                false,
                0,
                0,
                false,
                "n",
                List.of(),
                1,
                Path.of("unused"),
                Path.of("unused")
        );
    }
    public static List<Particle> generateCollidingClusters(FlockingConfig config) {
        Random random = config.seed().isPresent() ? new Random(config.seed().getAsLong()) : new Random();
        double l = config.l();
        int n = config.n();
        int leftCount = n / 2;
        int rightCount = n - leftCount;

        List<Particle> particles = new ArrayList<>(n);
        int id = 0;
        for (int i = 0; i < leftCount; i++) {
            particles.add(randomParticleInBand(random, id++, l, 0.05, 0.35, 0.0));
        }
        for (int i = 0; i < rightCount; i++) {
            particles.add(randomParticleInBand(random, id++, l, 0.65, 0.95, Math.PI));
        }
        return List.copyOf(particles);
    }

    private static Particle randomParticleInBand(Random random, int id, double l, double xFracMin, double xFracMax, double baseAngle) {
        double x = l * (xFracMin + random.nextDouble() * (xFracMax - xFracMin));
        double y = random.nextDouble() * l;
        double theta = baseAngle + (random.nextDouble() * 2 - 1) * 0.4;
        return new Particle(id, x, y, 0.0, theta);
    }
}
