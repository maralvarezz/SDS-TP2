package ar.edu.itba.sds.tp2.observable;

import ar.edu.itba.sds.model.Particle;

import java.util.List;

public final class Polarization {

    private Polarization() {
    }

    public static double compute(List<Particle> state) {
        if (state.isEmpty()) {
            return 0.0;
        }
        double sumCos = 0.0;
        double sumSin = 0.0;
        for (Particle particle : state) {
            sumCos += Math.cos(particle.property());
            sumSin += Math.sin(particle.property());
        }
        return Math.hypot(sumCos, sumSin) / state.size();
    }
}
