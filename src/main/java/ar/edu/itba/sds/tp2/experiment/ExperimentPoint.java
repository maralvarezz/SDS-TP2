package ar.edu.itba.sds.tp2.experiment;

import ar.edu.itba.sds.tp2.config.FlockingModel;

public record ExperimentPoint(
        FlockingModel model,
        double rho,
        double eta,
        int n,
        int reps,
        double meanVa,
        double stdVa,
        double meanS,
        double stdS
) {
}
