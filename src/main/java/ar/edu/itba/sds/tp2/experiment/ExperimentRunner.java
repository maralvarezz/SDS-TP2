package ar.edu.itba.sds.tp2.experiment;

import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.tp2.config.FlockingConfig;
import ar.edu.itba.sds.tp2.config.FlockingModel;
import ar.edu.itba.sds.tp2.engine.SimulationEngine;
import ar.edu.itba.sds.tp2.generator.FlockingParticleGenerator;
import ar.edu.itba.sds.tp2.observable.ClusterAnalyzer;
import ar.edu.itba.sds.tp2.observable.Polarization;
import ar.edu.itba.sds.tp2.observable.SteadyStateDetector;
import ar.edu.itba.sds.tp2.rule.DirectionRule;
import ar.edu.itba.sds.tp2.rule.VicsekAverageRule;
import ar.edu.itba.sds.tp2.rule.VoterRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Barre modelo x densidad x eta, corriendo `repetitions` simulaciones independientes por
 * combinacion (misma config, distinta semilla) y agregando va/S en estado estacionario. Es el
 * bloque que alimenta el CSV que consumen los graficos de los puntos (c), (d), (e) y (f).
 * <p>
 * L, rc, v0, dt, steps y la cantidad de repeticiones son los mismos para todas las combinaciones
 * de una corrida del runner -- lo que varia es modelo/densidad/eta, que se pasan en run().
 * <p>
 * fixedStationaryStart, si esta presente, bypassea el SteadyStateDetector y promedia siempre
 * desde ese paso fijo -- pensado unicamente para comparar contra otros grupos que usan un
 * cutoff fijo en vez de deteccion dinamica (misma logica que su --stationary-from + --no-dynamic).
 * El barrido "de verdad" del informe no debe usar esto, para eso queda vacio (OptionalInt.empty()).
 * <p>
 * run() paraleliza las combinaciones (model, rho, eta) entre los nucleos disponibles -- cada
 * combinacion es completamente independiente (semilla propia via seedFor(), motor de simulacion
 * propio, sin ningun estado mutable compartido: verificado que SimulationEngine, CellIndexMethod
 * de TP1 y SteadyStateDetector son todos statelesss/inmutables entre llamadas), asi que no hay
 * ningun riesgo de condicion de carrera ni de resultados no deterministicos por el paralelismo:
 * la semilla de cada combinacion depende solo de (model, rho, eta, rep), nunca del orden ni del
 * hilo en que se ejecuta. Antes esto corria secuencial en un solo hilo -- con 330 combinaciones
 * de 5000 steps cada una eso significaba varias horas en vez de los ~1h que le tomo a otro grupo
 * corriendo en paralelo con su propio runner.
 */
public final class ExperimentRunner {

    public enum InitialStateMode {
        RANDOM,
        COLLIDING
    }

    private final double l;
    private final double rc;
    private final double v0;
    private final double dt;
    private final int steps;
    private final int repetitions;
    private final SteadyStateDetector steadyStateDetector;
    private final InitialStateMode initialStateMode;
    private final OptionalInt fixedStationaryStart;

    public ExperimentRunner(
            double l, double rc, double v0, double dt, int steps, int repetitions,
            SteadyStateDetector steadyStateDetector
    ) {
        this(l, rc, v0, dt, steps, repetitions, steadyStateDetector, InitialStateMode.RANDOM, OptionalInt.empty());
    }

    public ExperimentRunner(
            double l, double rc, double v0, double dt, int steps, int repetitions,
            SteadyStateDetector steadyStateDetector, InitialStateMode initialStateMode
    ) {
        this(l, rc, v0, dt, steps, repetitions, steadyStateDetector, initialStateMode, OptionalInt.empty());
    }

    public ExperimentRunner(
            double l, double rc, double v0, double dt, int steps, int repetitions,
            SteadyStateDetector steadyStateDetector, InitialStateMode initialStateMode,
            OptionalInt fixedStationaryStart
    ) {
        this.l = l;
        this.rc = rc;
        this.v0 = v0;
        this.dt = dt;
        this.steps = steps;
        this.repetitions = repetitions;
        this.steadyStateDetector = steadyStateDetector;
        this.initialStateMode = initialStateMode;
        this.fixedStationaryStart = fixedStationaryStart;
    }

    private record Combo(FlockingModel model, double rho, double eta) {
    }

    public List<ExperimentPoint> run(List<FlockingModel> models, List<Double> densities, List<Double> etas) {
        List<Combo> combos = new ArrayList<>();
        for (FlockingModel model : models) {
            for (double rho : densities) {
                for (double eta : etas) {
                    combos.add(new Combo(model, rho, eta));
                }
            }
        }
        int total = combos.size();
        AtomicInteger done = new AtomicInteger(0);

        // Deja un nucleo libre para que la maquina siga usable mientras corre el barrido.
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        System.out.printf("Paralelizando %d combinaciones en %d hilos (de %d nucleos disponibles)%n",
                total, threads, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<ExperimentPoint>> futures = new ArrayList<>(combos.size());
            for (Combo combo : combos) {
                futures.add(executor.submit(() -> {
                    long start = System.nanoTime();
                    ExperimentPoint point = runCombination(combo.model(), combo.rho(), combo.eta());
                    double elapsedSeconds = (System.nanoTime() - start) / 1e9;
                    int index = done.incrementAndGet();
                    synchronized (System.out) {
                        System.out.printf(
                                "[%d/%d] modelo=%-6s rho=%.4f eta=%.3f n=%d -> va=%.3f±%.3f S=%.3f±%.3f (%.1fs)%n",
                                index, total, combo.model(), combo.rho(), combo.eta(), point.n(),
                                point.meanVa(), point.stdVa(), point.meanS(), point.stdS(), elapsedSeconds);
                    }
                    return point;
                }));
            }
            List<ExperimentPoint> points = new ArrayList<>(combos.size());
            for (Future<ExperimentPoint> future : futures) {
                points.add(future.get());
            }
            return points;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrumpido esperando resultados del barrido", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Fallo una combinacion del barrido", e.getCause());
        } finally {
            executor.shutdown();
        }
    }

    private ExperimentPoint runCombination(FlockingModel model, double rho, double eta) {
        List<Double> vaSamples = new ArrayList<>(repetitions);
        List<Double> sSamples = new ArrayList<>(repetitions);
        // Pool de TODOS los valores instantaneos va(t)/S(t) (t en la ventana estacionaria) de las
        // `repetitions` corridas, sin promediar cada repeticion primero. El desvio reportado sale
        // de aca (ver mas abajo), no del desvio entre las `repetitions` medias por repeticion:
        // promediar cada repeticion sobre toda la ventana estacionaria (potencialmente miles de
        // pasos) da una estimacion por repeticion carisima precisa, y el desvio ENTRE esas medias
        // ya-suavizadas colapsa a casi cero en regimenes ordenados/alta densidad (mucho
        // autopromediado), aunque el observable siga fluctuando de verdad paso a paso. El desvio
        // sobre el pool de valores instantaneos captura esa fluctuacion real, que es lo que se
        // espera ver como barra de error (asi lo tiene el grupo con el que comparamos).
        List<Double> vaPooled = new ArrayList<>();
        List<Double> sPooled = new ArrayList<>();
        int n = 0;
        int missedSteadyState = 0;

        for (int rep = 0; rep < repetitions; rep++) {
            FlockingConfig config = FlockingConfig.ofDensity(
                    rho, l, rc, v0, dt, eta, steps, model, OptionalLong.of(seedFor(model, rho, eta, rep)));
            n = config.n();

            RunResult result = runSingle(config);
            int start;
            if (fixedStationaryStart.isPresent()) {
                // Comparacion en igualdad de condiciones con otro grupo: sin deteccion, cutoff fijo.
                start = Math.min(fixedStationaryStart.getAsInt(), Math.max(0, result.va().size() - 1));
            } else {
                OptionalInt stationaryStart = steadyStateDetector.detect(result.va());
                if (stationaryStart.isPresent()) {
                    start = stationaryStart.getAsInt();
                } else {
                    // No se detecto estacionario dentro de los steps configurados -- probablemente
                    // falten pasos para esta combinacion (tipico en densidades bajas o eta cerca de
                    // la transicion). Fallback: promediar la segunda mitad de la corrida, pero avisar
                    // para que quede claro que ese punto necesita revision/mas steps.
                    start = result.va().size() / 2;
                    missedSteadyState++;
                }
            }

            vaSamples.add(average(result.va(), start));
            sSamples.add(average(result.s(), start));
            vaPooled.addAll(result.va().subList(start, result.va().size()));
            sPooled.addAll(result.s().subList(start, result.s().size()));
        }

        if (missedSteadyState > 0) {
            synchronized (System.err) {
                System.err.printf(
                        "  [aviso] modelo=%s rho=%.4f eta=%.3f: %d/%d corridas no llegaron a estado estacionario en %d steps (se uso la mitad final como fallback)%n",
                        model, rho, eta, missedSteadyState, repetitions, steps);
            }
        }

        // mean_va/mean_S: promedio de las medias por repeticion (estimador estandar del valor
        // estacionario). std_va/std_S: desvio sobre el pool de valores instantaneos (ver arriba),
        // NO sobre vaSamples/sSamples -- son dos cantidades distintas a proposito.
        return new ExperimentPoint(
                model, rho, eta, n, repetitions,
                mean(vaSamples), stdDev(vaPooled),
                mean(sSamples), stdDev(sPooled)
        );
    }

    private RunResult runSingle(FlockingConfig config) {
        Random random = new Random(config.seed().getAsLong());
        DirectionRule rule = ruleFor(config.model());
        SimulationEngine engine = new SimulationEngine(config, rule, random);
        List<Particle> state = initialStateFor(config);

        List<Double> va = new ArrayList<>(config.steps() + 1);
        List<Double> s = new ArrayList<>(config.steps() + 1);
        for (int t = 0; t <= config.steps(); t++) {
            Map<Integer, Set<Integer>> neighbours = engine.findNeighbours(state);
            va.add(Polarization.compute(state));
            s.add(ClusterAnalyzer.giantComponentFraction(neighbours));
            if (t < config.steps()) {
                state = engine.step(state, neighbours);
            }
        }
        return new RunResult(va, s);
    }

    private record RunResult(List<Double> va, List<Double> s) {
    }

    private List<Particle> initialStateFor(FlockingConfig config) {
        return switch (initialStateMode) {
            case RANDOM -> FlockingParticleGenerator.generateInitialState(config);
            case COLLIDING -> FlockingParticleGenerator.generateCollidingClusters(config);
        };
    }

    private static DirectionRule ruleFor(FlockingModel model) {
        return switch (model) {
            case VICSEK -> new VicsekAverageRule();
            case VOTER -> new VoterRule();
        };
    }

    /**
     * Semilla deterministica a partir de la combinacion -- reproducible entre corridas del
     * runner, pero distinta por repeticion (para que las repeticiones sean realizaciones
     * independientes de verdad). Depende SOLO de (model, rho, eta, rep), nunca del hilo ni del
     * orden de ejecucion -- por eso paralelizar run() no cambia ni un solo resultado.
     */
    private static long seedFor(FlockingModel model, double rho, double eta, int rep) {
        // OJO: usar model.name() y NO el enum model directamente. Enum no overridea hashCode(),
        // asi que Object.hashCode() por defecto es el identity hash, que la JVM NO garantiza
        // estable entre corridas -- encontrado el 24/08 corriendo el mismo comando dos veces
        // seguidas y viendo semillas distintas cada vez (por eso los resultados no eran
        // reproducibles corrida a corrida, mas notorio en los casos cerca de una transicion,
        // donde la dinamica es sensible a la condicion inicial). String.hashCode() si esta
        // definido por spec (deterministico), asi que con .name() la semilla queda reproducible.
        return Objects.hash(model.name(), rho, eta, rep);
    }

    private static double average(List<Double> series, int fromIndexInclusive) {
        double sum = 0;
        int count = 0;
        for (int i = fromIndexInclusive; i < series.size(); i++) {
            sum += series.get(i);
            count++;
        }
        return count == 0 ? Double.NaN : sum / count;
    }

    private static double mean(List<Double> values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private static double stdDev(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }
        double m = mean(values);
        double sumSq = 0;
        for (double value : values) {
            sumSq += (value - m) * (value - m);
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }
}
