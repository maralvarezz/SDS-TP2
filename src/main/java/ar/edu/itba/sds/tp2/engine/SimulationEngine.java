package ar.edu.itba.sds.tp2.engine;

import ar.edu.itba.sds.algorithm.CellIndexMethod;
import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.tp2.config.FlockingConfig;
import ar.edu.itba.sds.tp2.rule.DirectionRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Cierra el ciclo del diagrama: estado(t) -> CIM del TP1 -> Map(id, vecinos) -> regla de
 * direccion (Vicsek/votante) -> estado(t+1). No hace I/O ni calcula observables (eso va en capas
 * arriba de esta) para poder testear la fisica de forma aislada.
 *
 * Reutiliza el record Particle(id, x, y, radius, property) de TP1 tal cual: radius = 0 (particulas
 * puntuales, el corte de interaccion queda en rc solo) y property se usa para guardar el angulo
 * theta de la velocidad.
 */
public final class SimulationEngine {

    private final FlockingConfig config;
    private final DirectionRule rule;
    private final Random random;

    public SimulationEngine(FlockingConfig config, DirectionRule rule, Random random) {
        this.config = config;
        this.rule = rule;
        this.random = random;
    }

    /**
     * Estado inicial: N particulas distribuidas uniformemente en la caja, con angulo inicial
     * uniforme en (-pi, pi].
     */
    public List<Particle> randomInitialState() {
        List<Particle> state = new ArrayList<>(config.n());
        for (int id = 0; id < config.n(); id++) {
            double x = random.nextDouble() * config.l();
            double y = random.nextDouble() * config.l();
            double theta = (random.nextDouble() * 2 - 1) * Math.PI;
            state.add(new Particle(id, x, y, 0.0, theta));
        }
        return state;
    }

    /**
     * Vecinos de cada particula segun el CIM de TP1, con contorno periodico (fijo por enunciado).
     * Se expone aparte de step() porque el calculo de clusters (punto d del TP) necesita el mismo
     * Map(id, vecinos) del mismo instante, y no tiene sentido correr el CIM dos veces por paso.
     */
    public Map<Integer, Set<Integer>> findNeighbours(List<Particle> state) {
        return CellIndexMethod.findNeighbours(state, config.l(), config.cellsPerSide(), config.rc(), true);
    }

    /**
     * Un paso de la simulacion: aplica la regla de direccion configurada (Vicsek o votante) y
     * despues mueve cada particula a rapidez constante v0, con wraparound periodico en la caja.
     */
    public List<Particle> step(List<Particle> state, Map<Integer, Set<Integer>> neighbours) {
        Map<Integer, Particle> particlesById = new HashMap<>();
        for (Particle particle : state) {
            particlesById.put(particle.id(), particle);
        }

        List<Particle> nextState = new ArrayList<>(state.size());
        for (Particle particle : state) {
            Set<Integer> neighbourIds = neighbours.get(particle.id());
            double newAngle = rule.nextAngle(particle, neighbourIds, particlesById, config.eta(), random);
            double newX = wrap(particle.x() + config.v0() * Math.cos(newAngle) * config.dt(), config.l());
            double newY = wrap(particle.y() + config.v0() * Math.sin(newAngle) * config.dt(), config.l());
            nextState.add(new Particle(particle.id(), newX, newY, particle.radius(), newAngle));
        }
        return nextState;
    }

    private static double wrap(double value, double l) {
        double wrapped = value % l;
        return wrapped < 0 ? wrapped + l : wrapped;
    }
}
