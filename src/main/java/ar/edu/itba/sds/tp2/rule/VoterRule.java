package ar.edu.itba.sds.tp2.rule;

import ar.edu.itba.sds.model.Particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class VoterRule implements DirectionRule {

    @Override
    public double nextAngle(Particle self, Set<Integer> neighbourIds, Map<Integer, Particle> particlesById, double eta, Random random) {
        double baseAngle;
        if (neighbourIds.isEmpty()) {
            baseAngle = self.property();
        } else {
            List<Integer> ids = new ArrayList<>(neighbourIds);
            int chosenId = ids.get(random.nextInt(ids.size()));
            baseAngle = particlesById.get(chosenId).property();
        }
        return AngleUtils.normalize(baseAngle + AngleUtils.noise(eta, random));
    }
}
