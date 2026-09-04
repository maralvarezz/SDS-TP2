package ar.edu.itba.sds.tp2.rule;

import java.util.Random;

final class AngleUtils {

    private AngleUtils() {
    }

    static double noise(double eta, Random random) {
        return (random.nextDouble() - 0.5) * eta;
    }

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
