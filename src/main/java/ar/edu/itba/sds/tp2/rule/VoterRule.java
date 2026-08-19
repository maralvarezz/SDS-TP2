package ar.edu.itba.sds.tp2.rule;

import ar.edu.itba.sds.model.Particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Modelo de votante [2]: a diferencia de Vicsek, la particula NO promedia -- elige al azar a un
 * solo vecino dentro de rc y copia directamente su direccion, mas el termino de ruido. Si no
 * tiene vecinos, no hay a quien copiar y conserva su propia direccion.
 */
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
