package ar.edu.itba.sds.tp2.io;

import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.tp2.config.FlockingConfig;
import ar.edu.itba.sds.tp2.config.FlockingModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrajectoryWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesMetadataHeaderAndFramesIncrementally() throws Exception {
        FlockingConfig config = new FlockingConfig(
                2, 10, 1, 0.5, 1, 0.2, 1, FlockingModel.VICSEK, OptionalLong.of(7));
        Path output = tempDir.resolve("nested/trajectory.txt");

        try (TrajectoryWriter writer = new TrajectoryWriter(output, config)) {
            writer.writeFrame(0, List.of(
                    new Particle(1, 1, 2, 0, 0),
                    new Particle(2, 3, 4, 0, Math.PI / 2)));
            writer.writeFrame(1, List.of(
                    new Particle(1, 1.5, 2, 0, 0),
                    new Particle(2, 3, 4.5, 0, Math.PI / 2)));
        }

        List<String> lines = Files.readAllLines(output);
        assertEquals("# model=VICSEK", lines.get(0));
        assertTrue(lines.get(1).contains("n=2 rho=0.0200 l=10.0000"));
        assertEquals("t,id,x,y,vx,vy", lines.get(2));
        assertEquals("0,1,1.00000000,2.00000000,0.50000000,0.00000000", lines.get(3));
        assertEquals(7, lines.size());
    }

    @Test
    void rejectsMalformedFrames() throws Exception {
        FlockingConfig config = new FlockingConfig(
                2, 10, 1, 0.5, 1, 0.2, 1, FlockingModel.VOTER, OptionalLong.empty());
        try (TrajectoryWriter writer = new TrajectoryWriter(tempDir.resolve("trajectory.txt"), config)) {
            assertThrows(IllegalArgumentException.class,
                    () -> writer.writeFrame(0, List.of(new Particle(1, 1, 2, 0, 0))));
            assertThrows(IllegalArgumentException.class, () -> writer.writeFrame(0, List.of(
                    new Particle(1, 1, 2, 0, 0), new Particle(1, 3, 4, 0, 0))));
        }
    }
}
