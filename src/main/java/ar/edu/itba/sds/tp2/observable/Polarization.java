package ar.edu.itba.sds.tp2.observable;

import ar.edu.itba.sds.model.Particle;

import java.util.List;

/**
 * Polarizacion va: el observable primario del TP (parametro de orden de Vicsek). Mide que tan
 * alineadas estan las velocidades de todas las particulas -- 1 significa perfectamente alineadas
 * (bandada ordenada), valores cercanos a 0 significan direcciones aleatorias (desorden, tipico
 * de eta alto).
 * <p>
 * va = | mean_i (cos(theta_i), sin(theta_i)) |, el modulo del promedio de los vectores unitarios
 * de velocidad. Como todas las particulas comparten la misma rapidez v0, el resultado no depende
 * de v0 -- por eso esta clase solo necesita los angulos (particle.property()).
 */
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
