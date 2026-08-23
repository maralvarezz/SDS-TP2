package ar.edu.itba.sds.tp2.io;

import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.tp2.config.FlockingConfig;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Escribe la trayectoria completa sin acumularla en memoria. El archivo resultante es la entrada
 * independiente de viz/animate_trajectory.py.
 */
public final class TrajectoryWriter implements Closeable {

    private final BufferedWriter writer;
    private final FlockingConfig config;
    private int lastTime = -1;

    public TrajectoryWriter(Path path, FlockingConfig config) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        this.config = config;
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);

        double rho = config.n() / (config.l() * config.l());
        writer.write("# model=" + config.model());
        writer.newLine();
        writer.write(String.format(Locale.US,
                "# n=%d rho=%.4f l=%.4f rc=%.4f v0=%.4f dt=%.4f eta=%.4f steps=%d",
                config.n(), rho, config.l(), config.rc(), config.v0(), config.dt(),
                config.eta(), config.steps()));
        writer.newLine();
        writer.write("t,id,x,y,vx,vy");
        writer.newLine();
    }

    public void writeFrame(int t, List<Particle> state) throws IOException {
        if (t < 0 || t <= lastTime) {
            throw new IllegalArgumentException("Los tiempos deben ser no negativos y crecientes: " + t);
        }
        if (state.size() != config.n()) {
            throw new IllegalArgumentException(
                    "El frame t=" + t + " tiene " + state.size() + " particulas; se esperaban " + config.n());
        }

        Set<Integer> ids = new HashSet<>();
        for (Particle particle : state) {
            if (!ids.add(particle.id())) {
                throw new IllegalArgumentException("Id de particula repetido en t=" + t + ": " + particle.id());
            }
            double vx = config.v0() * Math.cos(particle.property());
            double vy = config.v0() * Math.sin(particle.property());
            writer.write(String.format(Locale.US, "%d,%d,%.8f,%.8f,%.8f,%.8f",
                    t, particle.id(), particle.x(), particle.y(), vx, vy));
            writer.newLine();
        }
        lastTime = t;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
