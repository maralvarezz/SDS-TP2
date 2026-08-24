package ar.edu.itba.sds.tp2.observable;

import java.util.List;
import java.util.OptionalInt;

/**
 * Detecta a partir de que paso una serie temporal (tipicamente va(t) o S(t)) entro en estado
 * estacionario. Una ventana de tamaño windowSize se considera "estacionaria" cuando se cumplen
 * TRES condiciones a la vez, durante minConsecutiveWindows ventanas seguidas:
 * <ol>
 *     <li>pendiente (cuadrados minimos) con valor absoluto por debajo de slopeThreshold;</li>
 *     <li>desvio estandar de la ventana por debajo de maxWindowStdDev;</li>
 *     <li>el promedio de la ventana no se aleja mas de tailTolerance del promedio de la cola de
 *     la serie (el ultimo tailFraction de los pasos).</li>
 * </ol>
 * <p>
 * Las primeras dos condiciones (agregadas el 24/08) no alcanzan solas para el modelo de
 * votante con eta=0: encontramos con un caso real (rho=8, eta=0) que ese modelo puede quedarse
 * "pausado" durante muchos pasos seguidos en un nivel que todavia esta lejos del consenso final
 * (ej. va oscilando sin tendencia neta y con poco ruido alrededor de 0.36, entre t=99 y t~350),
 * antes de retomar la deriva hacia consenso -- son mesetas locales genuinas, no ruido de
 * medicion, asi que ni agrandar la ventana ni subir minConsecutiveWindows las filtra (se
 * probo hasta windowSize=300 sobre datos reales y seguian marcando falso positivo). La unica
 * forma de distinguir "meseta local" de "estado estacionario real" es compararla contra donde
 * termina la serie: por eso la tercera condicion, que ancla la ventana candidata al nivel final
 * en vez de mirar solo su forma local. Esto es valido porque el detector corre offline sobre la
 * serie ya completa (no en tiempo real), asi que conocer la cola no es trampa.
 * <p>
 * Calibracion (24/08, con datos reales): para VOTER rho=8 eta=0 la meseta falsa (nivel ~0.36,
 * cola final en 1.0) queda rechazada con cualquier tailTolerance razonable (la diferencia es
 * ~0.64); la convergencia real se detecta ahi cuando el promedio de la ventana ya esta a menos
 * de tailTolerance del nivel final (con tailTolerance=0.1, en t~488, con va ya en el orden de
 * 0.9-0.98 subiendo hacia 1.0). Para VICSEK rho=4 eta=2 (caso con ruido real, sin consenso
 * absoluto) el desvio de ventanas de 50 pasos dentro de la meseta genuina fue entre 0.009 y
 * 0.039 (media 0.018), de ahi que maxWindowStdDev tenga que ser bastante mayor a esos valores
 * (0.01 se probo primero y quedaba por debajo de casi todo ese rango, de ahi el aviso de "no
 * llego a estado estacionario" en casi todas las combinaciones con eta&gt;0 en la corrida
 * completa antes de este ajuste).
 * <p>
 * Es el criterio pensado para el runner de experimentos, que corre muchas combinaciones sin que
 * haya alguien mirando cada grafico -- se valida a ojo aparte con viz/plot_observables.py en un
 * puñado de casos caracteristicos, que es justo lo que pide mostrar el punto (b) del enunciado.
 */
public final class SteadyStateDetector {

    private final int windowSize;
    private final double slopeThreshold;
    private final int minConsecutiveWindows;
    private final double maxWindowStdDev;
    private final double tailTolerance;
    private final double tailFraction;

    public SteadyStateDetector(int windowSize, double slopeThreshold, int minConsecutiveWindows,
                                double maxWindowStdDev, double tailTolerance) {
        this(windowSize, slopeThreshold, minConsecutiveWindows, maxWindowStdDev, tailTolerance, 0.1);
    }

    public SteadyStateDetector(int windowSize, double slopeThreshold, int minConsecutiveWindows,
                                double maxWindowStdDev, double tailTolerance, double tailFraction) {
        if (windowSize < 2) {
            throw new IllegalArgumentException("windowSize debe ser al menos 2: " + windowSize);
        }
        if (slopeThreshold < 0) {
            throw new IllegalArgumentException("slopeThreshold no puede ser negativo: " + slopeThreshold);
        }
        if (minConsecutiveWindows < 1) {
            throw new IllegalArgumentException("minConsecutiveWindows debe ser al menos 1: " + minConsecutiveWindows);
        }
        if (maxWindowStdDev < 0) {
            throw new IllegalArgumentException("maxWindowStdDev no puede ser negativo: " + maxWindowStdDev);
        }
        if (tailTolerance < 0) {
            throw new IllegalArgumentException("tailTolerance no puede ser negativo: " + tailTolerance);
        }
        if (tailFraction <= 0 || tailFraction > 1) {
            throw new IllegalArgumentException("tailFraction debe estar en (0, 1]: " + tailFraction);
        }
        this.windowSize = windowSize;
        this.slopeThreshold = slopeThreshold;
        this.minConsecutiveWindows = minConsecutiveWindows;
        this.maxWindowStdDev = maxWindowStdDev;
        this.tailTolerance = tailTolerance;
        this.tailFraction = tailFraction;
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

        int tailWindow = Math.max(windowSize, (int) Math.round(n * tailFraction));
        tailWindow = Math.min(tailWindow, n);
        double tailMean = windowMean(series, n - tailWindow, tailWindow);

        int consecutive = 0;
        int candidateStart = -1;
        for (int start = 0; start + windowSize <= n; start++) {
            double slope = windowSlope(series, start);
            double stdDev = windowStdDev(series, start);
            double mean = windowMean(series, start, windowSize);
            boolean closeToTail = Math.abs(mean - tailMean) <= tailTolerance;
            if (Math.abs(slope) <= slopeThreshold && stdDev <= maxWindowStdDev && closeToTail) {
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

    private double windowMean(List<Double> series, int start, int window) {
        double sum = 0;
        for (int i = 0; i < window; i++) {
            sum += series.get(start + i);
        }
        return sum / window;
    }

    /**
     * Desvio estandar (poblacional) de series[start .. start+windowSize). Filtra el caso de una
     * ventana ruidosa sin tendencia neta (pendiente ~0) pero que en realidad todavia esta lejos
     * de haber convergido.
     */
    private double windowStdDev(List<Double> series, int start) {
        double mean = windowMean(series, start, windowSize);
        double sumSquaredDiff = 0;
        for (int i = 0; i < windowSize; i++) {
            double diff = series.get(start + i) - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / windowSize);
    }
}
