package ar.edu.itba.sds.tp2.rule;

import ar.edu.itba.sds.model.Particle;

import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class VicsekAverageRule implements DirectionRule {

    @Override
    public double nextAngle(Particle self, Set<Integer> neighbourIds, Map<Integer, Particle> particlesById, double eta, Random random) {
        double sumSin = Math.sin(self.property());
        double sumCos = Math.cos(self.property());

        for (int neighbourId : neighbourIds) {
            double angle = particlesById.get(neighbourId).property();
            sumSin += Math.sin(angle);
            sumCos += Math.cos(angle);
        }

        double meanAngle = Math.atan2(sumSin, sumCos);
        return AngleUtils.normalize(meanAngle + AngleUtils.noise(eta, random));
    }
}
