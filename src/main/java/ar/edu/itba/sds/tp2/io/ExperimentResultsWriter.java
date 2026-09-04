package ar.edu.itba.sds.tp2.io;

import ar.edu.itba.sds.tp2.experiment.ExperimentPoint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExperimentResultsWriter {

    private ExperimentResultsWriter() {
    }

    public static void write(Path path, List<ExperimentPoint> points) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>(points.size() + 1);
        lines.add("model,rho,eta,n,n_reps,mean_va,std_va,mean_S,std_S");
        for (ExperimentPoint point : points) {
            lines.add(String.format(Locale.US, "%s,%.6f,%.6f,%d,%d,%.6f,%.6f,%.6f,%.6f",
                    point.model(), point.rho(), point.eta(), point.n(), point.reps(),
                    point.meanVa(), point.stdVa(), point.meanS(), point.stdS()));
        }
        Files.write(path, lines);
    }
}
