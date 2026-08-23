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

Para el punto g se usa la serie `free` del CSV crudo de TP1, que corresponde a `L=20` y `M=10`,
junto con el contrato exacto `n,run,elapsed_ns` de TP2:

```bash
python3 viz/plot_cim_comparison.py /ruta/al/time_N_runs.csv \
  output/cim_timing_tp2.csv --out output/figures/cim_tp1_vs_tp2.png
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
