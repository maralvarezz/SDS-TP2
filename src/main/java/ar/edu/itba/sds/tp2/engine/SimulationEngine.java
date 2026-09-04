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

public final class SimulationEngine {

    private final FlockingConfig config;
    private final DirectionRule rule;
    private final Random random;

    public SimulationEngine(FlockingConfig config, DirectionRule rule, Random random) {
        this.config = config;
        this.rule = rule;
        this.random = random;
    }

    public Map<Integer, Set<Integer>> findNeighbours(List<Particle> state) {
        return CellIndexMethod.findNeighbours(state, config.l(), config.cellsPerSide(), config.rc(), true);
    }

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
