package ar.edu.itba.sds.tp2.observable;

import java.util.List;
import java.util.OptionalInt;

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
