package ar.edu.itba.sds.tp2.rule;

import ar.edu.itba.sds.model.Particle;

import java.util.Map;
import java.util.Random;
import java.util.Set;

public interface DirectionRule {

    double nextAngle(Particle self, Set<Integer> neighbourIds, Map<Integer, Particle> particlesById, double eta, Random random);
}
