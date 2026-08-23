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

/**
 * Arma estado(0) en dos etapas, igual que TP1:
 * <p>
 * 1) "Estatica": reusa tal cual ParticleGenerator.generateStaticSystem() de TP1 para decidir N
 * y el radio de cada particula (0.0 -- particulas puntuales; el corte de interaccion queda
 * definido solo por rc). No hacia falta reescribir esto, TP1 ya lo resuelve.
 * <p>
 * 2) "Dinamica": ahi TP1 solo genera posiciones sin superposicion y arrastra la property
 * estatica de cada particula tal cual. Ac&aacute; hace falta algo distinto -- cada particula
 * necesita ademas un ANGULO DE VELOCIDAD inicial, algo que no existe en el modelo de TP1 -- asi
 * que esta clase reimplementa el mismo esquema de muestreo de TP1 (posicion uniforme en la caja,
 * con rechazo por superposicion) pero suma el sorteo del angulo inicial en property.
 */
public final class FlockingParticleGenerator {
    private static final int MAX_ATTEMPTS_PER_PARTICLE = 100_000;

    private FlockingParticleGenerator() {
    }

    /**
     * Estado inicial completo: N particulas ubicadas al azar en la caja (condicion periodica,
     * fija segun el enunciado) con angulo de velocidad inicial uniforme en (-pi, pi].
     */
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
                // Con radio 0 (particulas puntuales) bound = l y el chequeo de superposicion de
                // abajo nunca rechaza en la practica; se mantiene la misma estructura que TP1
                // por si el dia de mañana hace falta un radio de exclusion > 0.
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

    /**
     * Adapta un FlockingConfig de TP2 al SimulationConfig que pide
     * ParticleGenerator.generateStaticSystem() de TP1. Solo n, l, radiusMin/Max y el seed
     * importan para ese metodo puntual; el resto de los campos de TP1 (paths de I/O, m, viz,
     * etc.) no se leen ahi y quedan con valores de relleno.
     */
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
                Path.of("unused")
        );
    }
}
