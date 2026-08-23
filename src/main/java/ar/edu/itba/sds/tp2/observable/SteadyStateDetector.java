package ar.edu.itba.sds.tp2.observable;

import java.util.List;
import java.util.OptionalInt;

/**
 * Detecta a partir de que paso una serie temporal (tipicamente va(t) o S(t)) entro en estado
 * estacionario, con el criterio de pendiente en ventana movil charlado con el equipo: se ajusta
 * una recta de cuadrados minimos a cada ventana de tamaño windowSize, y se considera
 * "estacionario" el primer t donde la pendiente absoluta se mantiene por debajo de
 * slopeThreshold durante minConsecutiveWindows ventanas seguidas (para no cortar en un cruce por
 * cero casual de la derivada en pleno transitorio).
 * <p>
 * Es el criterio pensado para el runner de experimentos, que corre muchas combinaciones sin que
 * haya alguien mirando cada grafico -- se valida a ojo aparte con viz/plot_observables.py en un
 * puñado de casos caracteristicos, que es justo lo que pide mostrar el punto (b) del enunciado.
 */
public final class SteadyStateDetector {

    private final int windowSize;
    private final double slopeThreshold;
    private final int minConsecutiveWindows;

    public SteadyStateDetector(int windowSize, double slopeThreshold, int minConsecutiveWindows) {
        if (windowSize < 2) {
            throw new IllegalArgumentException("windowSize debe ser al menos 2: " + windowSize);
        }
        if (slopeThreshold < 0) {
            throw new IllegalArgumentException("slopeThreshold no puede ser negativo: " + slopeThreshold);
        }
        if (minConsecutiveWindows < 1) {
            throw new IllegalArgumentException("minConsecutiveWindows debe ser al menos 1: " + minConsecutiveWindows);
        }
        this.windowSize = windowSize;
        this.slopeThreshold = slopeThreshold;
        this.minConsecutiveWindows = minConsecutiveWindows;
    }

    /**
     * Indice de la serie desde el cual se considera estacionaria (en este TP coincide con el t,
     * porque las series siempre arrancan en t=0), o vacio si nunca se cumplio el criterio dentro
     * de la corrida -- en ese caso el que llama tiene que decidir un fallback (ver
     * ExperimentRunner) y OJO porque probablemente signifique que faltan steps para esa
     * combinacion.
     */
    public OptionalInt detect(List<Double> series) {
        int n = series.size();
        if (n < windowSize) {
            return OptionalInt.empty();
        }

        int consecutive = 0;
        int candidateStart = -1;
        for (int start = 0; start + windowSize <= n; start++) {
            double slope = windowSlope(series, start);
            if (Math.abs(slope) <= slopeThreshold) {
                if (consecutive == 0) {
                    candidateStart = start;
                }
                consecutive++;
                if (consecutive >= minConsecutiveWindows) {
                    return OptionalInt.of(candidateStart);
                }
            } else {
                consecutive = 0;
                candidateStart = -1;
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Pendiente de la recta de cuadrados minimos ajustada a series[start .. start+windowSize).
     */
    private double windowSlope(List<Double> series, int start) {
        double sumT = 0;
        double sumV = 0;
        double sumTV = 0;
        double sumTT = 0;
        for (int i = 0; i < windowSize; i++) {
            double t = i;
            double v = series.get(start + i);
            sumT += t;
            sumV += v;
            sumTV += t * v;
            sumTT += t * t;
        }
        double denominator = windowSize * sumTT - sumT * sumT;
        if (denominator == 0) {
            return 0.0;
        }
        return (windowSize * sumTV - sumT * sumV) / denominator;
    }
}
