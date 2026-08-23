package ar.edu.itba.sds.tp2.experiment;

import ar.edu.itba.sds.tp2.config.FlockingModel;

/**
 * Un punto agregado del barrido de experimentos: para un (modelo, rho, eta) fijo, el promedio y
 * desvio de va y S en estado estacionario, calculados sobre `reps` corridas independientes
 * (misma config, distinta semilla). Cada corrida aporta UN numero (su propio promedio en la
 * ventana estacionaria) -- el desvio de abajo es entre corridas independientes, no dentro de una
 * misma serie temporal, así que no hace falta lidiar con la autocorrelacion de va(t)/S(t) para
 * estimar el error.
 */
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
