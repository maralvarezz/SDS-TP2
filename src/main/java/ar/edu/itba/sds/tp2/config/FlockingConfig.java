package ar.edu.itba.sds.tp2.config;

import java.util.OptionalLong;

public record FlockingConfig(
        int n,
        double l,
        double rc,
        double v0,
        double dt,
        double eta,
        int steps,
        FlockingModel model,
        OptionalLong seed
) {
    public FlockingConfig {
        if (n <= 0) {
            throw new IllegalArgumentException("n debe ser positivo: " + n);
        }
        if (l <= 0) {
            throw new IllegalArgumentException("l debe ser positivo: " + l);
        }
        if (rc <= 0) {
            throw new IllegalArgumentException("rc debe ser positivo: " + rc);
        }
        if (l < 3 * rc) {
            throw new IllegalArgumentException(
                    "l debe ser al menos 3*rc para que el CIM funcione bien: l=" + l + ", rc=" + rc);
        }
        if (v0 < 0) {
            throw new IllegalArgumentException("v0 no puede ser negativo: " + v0);
        }
        if (dt <= 0) {
            throw new IllegalArgumentException("dt debe ser positivo: " + dt);
        }
        if (eta < 0) {
            throw new IllegalArgumentException("eta no puede ser negativo: " + eta);
        }
        if (steps < 0) {
            throw new IllegalArgumentException("steps no puede ser negativo: " + steps);
        }
    }

    public static FlockingConfig ofDensity(
            double rho, double l, double rc, double v0, double dt, double eta, int steps,
            FlockingModel model, OptionalLong seed
    ) {
        int n = (int) Math.round(rho * l * l);
        return new FlockingConfig(n, l, rc, v0, dt, eta, steps, model, seed);
    }

    public int cellsPerSide() {
        return Math.max((int) Math.floor(l / rc), 1);
    }
}
