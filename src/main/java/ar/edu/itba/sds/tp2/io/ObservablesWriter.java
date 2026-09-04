package ar.edu.itba.sds.tp2.io;

import ar.edu.itba.sds.tp2.config.FlockingConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ObservablesWriter {

    private ObservablesWriter() {
    }

    public record Row(int t, double polarization, double clusterFraction) {
    }

    public static void write(Path path, FlockingConfig config, List<Row> rows) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        double rho = config.n() / (config.l() * config.l());
        List<String> lines = new ArrayList<>(rows.size() + 3);
        lines.add("# model=" + config.model());
        lines.add(String.format(Locale.US, "# n=%d rho=%.4f l=%.4f rc=%.4f v0=%.4f dt=%.4f eta=%.4f steps=%d",
                config.n(), rho, config.l(), config.rc(), config.v0(), config.dt(), config.eta(), config.steps()));
        lines.add("t,polarization,cluster_fraction");
        for (Row row : rows) {
            lines.add(String.format(Locale.US, "%d,%.6f,%.6f", row.t(), row.polarization(), row.clusterFraction()));
        }
        Files.write(path, lines);
    }
}
