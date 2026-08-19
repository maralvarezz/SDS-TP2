package ar.edu.itba.sds.tp2.rule;

import java.util.Random;

/**
 * Utilidades compartidas por las reglas de actualizacion de direccion.
 */
final class AngleUtils {

    private AngleUtils() {
    }

    /**
     * Ruido uniforme en [-eta/2, eta/2), como en el algoritmo original de Vicsek.
     */
    static double noise(double eta, Random random) {
        return (random.nextDouble() - 0.5) * eta;
    }

    /**
     * Normaliza un angulo a (-pi, pi] para que no crezca sin limite a lo largo de la simulacion.
     */
    static double normalize(double angle) {
        double twoPi = 2 * Math.PI;
        double wrapped = angle % twoPi;
        if (wrapped <= -Math.PI) {
            wrapped += twoPi;
        } else if (wrapped > Math.PI) {
            wrapped -= twoPi;
        }
        return wrapped;
    }
}
