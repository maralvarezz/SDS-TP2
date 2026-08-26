package ar.edu.itba.sds.tp2.experiment;

import java.io.IOException;

/**
 * Alias corto para correr los experimentos desde exec:java sin recordar el nombre largo.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        ExperimentMain.main(args);
    }
}
