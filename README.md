# SDS-TP2

Simulación off-lattice de los modelos de Vicsek y votante. La simulación y la visualización se
ejecutan por separado: Java genera archivos de texto y los scripts de `viz/` los consumen.

## Salidas de una corrida

`Main` genera simultáneamente:

- `output/observables.txt`: `t,polarization,cluster_fraction` para los puntos b y d.
- `output/trajectory.txt`: `t,id,x,y,vx,vy` para las animaciones del punto a.

Ambos archivos incluyen metadata comentada con modelo, densidad, geometría, ruido y pasos. Cada
nueva corrida sobrescribe estas rutas; antes de correr otra configuración, copiar las salidas que
se usarán como casos característicos con nombres descriptivos.

```bash
python3 viz/plot_observables.py output/observables_vicsek.txt \
  output/observables_voter.txt --stationary-t 400 450 \
  --out output/figures/evolucion.png

python3 viz/animate_trajectory.py output/trajectory.txt \
  --out output/figures/vicsek_rho4_eta1.mp4 --fps 20 --stride 2

python3 viz/animate_trajectory.py output/trajectory.txt \
  --out output/figures/vicsek_rho4_eta1.gif --fps 15
```

Sin `--out`, los scripts abren una ventana interactiva.

## Gráficos agregados

Los scripts leen directamente los contratos de `ExperimentMain`; no generan CSV intermedios.

```bash
python3 viz/plot_va_vs_eta.py output/experiments_polarization.csv \
  --out output/figures/va_vs_eta.png

python3 viz/plot_s_vs_eta.py output/experiments_polarization.csv \
  output/experiments_clusters.csv --out output/figures/s_vs_eta.png

python3 viz/plot_va_vs_s.py output/experiments_polarization.csv \
  output/experiments_clusters.csv --out output/figures/va_vs_s.png
```

Para el punto g la comparación es a **densidad fija**, no a L fijo: con L constante, al crecer
N también crece la densidad y el tiempo del CIM deja de reflejar solo el efecto de N, y con
partículas de radio (0.23–0.26) se vuelve geométricamente imposible de empaquetar a N grande. Por
eso TP1 se corre una vez POR CADA N con L y M ya escalados a la misma densidad de referencia
(N0=100, L0=20 → ρ=0.25; los valores de L y M por N están en el javadoc de `CimTimingMain`), sin
`--compare-density`:

```bash
cd ../SDS-TP1
python3 viz/time_analysis.py --variable n --values 10   --runs-per-value 10 --m 4  --l 6.3246   --rc 1 --periodic --compile
python3 viz/time_analysis.py --variable n --values 20   --runs-per-value 10 --m 5  --l 8.9443   --rc 1 --periodic
python3 viz/time_analysis.py --variable n --values 50   --runs-per-value 10 --m 9  --l 14.1421  --rc 1 --periodic
python3 viz/time_analysis.py --variable n --values 100  --runs-per-value 10 --m 13 --l 20.0000  --rc 1 --periodic
python3 viz/time_analysis.py --variable n --values 200  --runs-per-value 10 --m 18 --l 28.2843  --rc 1 --periodic
python3 viz/time_analysis.py --variable n --values 500  --runs-per-value 10 --m 29 --l 44.7214  --rc 1 --periodic
python3 viz/time_analysis.py --variable n --values 1000 --runs-per-value 10 --m 41 --l 63.2456  --rc 1 --periodic
python3 viz/time_analysis.py --variable n --values 2000 --runs-per-value 10 --m 58 --l 89.4427  --rc 1 --periodic
python3 viz/time_analysis.py --variable n --values 5000 --runs-per-value 10 --m 91 --l 141.4214 --rc 1 --periodic
```

(solo el primer comando necesita `--compile`; genera 9 carpetas separadas en
`output/figures/time_N_*/`). Del lado de TP2, `CimTimingMain` corre el mismo barrido de N con un
warm-up previo para que el tiempo no arrastre ruido de compilación JIT:

```bash
cd ../SDS-TP2
mvn compile exec:java -Dexec.mainClass="ar.edu.itba.sds.tp2.experiment.CimTimingMain"
```

Y el gráfico, en escala log-log, con la pendiente del ajuste de potencia de cada curva anotada en
la leyenda (≈1 confirma la complejidad O(N) esperada del CIM):

```bash
python3 viz/compare_cim_timing.py \
  --tp1-runs ../SDS-TP1/output/figures/time_N_*/time_N_*_runs.csv \
  --tp2-runs output/cim_timing_tp2.csv \
  --out output/figures/cim_timing_comparison_loglog.png
```

## Dependencias y pruebas

```bash
python3 -m pip install -r viz/requirements.txt
mvn -f ../SDS-TP1/pom.xml clean install
mvn test
python3 -m unittest discover -s viz/tests
```

Los CSV, figuras y animaciones permanecen bajo `output/`, que está ignorado por Git y no debe
incluirse en el ZIP final del motor.

## Entrega

Grupo 08, comisión S2. Según el enunciado, los nombres base son:

- `SdS_TP2_2026Q2G08CS2_Presentación.pdf`
- `SdS_TP2_2026Q2G08CS2_Codigo.zip`
- `SdS_TP2_2026Q2G08CS2_Informe.pdf`

Antes de empaquetar, contrastar estos nombres y la estructura de la presentación/informe con
`Formato_Presentaciones.pdf`, `Formato_Informes.pdf` y `GuiaPresentaciones.pdf` de Campus.
