#!/bin/zsh
# Corre el modelo VOTER para rho=2,4,8 con unos pocos ruidos entre 0.2 y 1, guardando cada
# resultado con nombre descriptivo (Main.java sobreescribe output/observables.txt en cada
# corrida, asi que hay que moverlo antes de la siguiente). Correr desde la raiz del repo
# SDS-TP2 (donde esta el pom.xml).

set -e

mvn -q compile

ETA_CASES=(
  "0.2:eta02"
  "0.4:eta04"
  "0.5:eta05"
  "0.7:eta07"
  "1.0:eta1"
)
RHOS=(2 4 8)

for entry in "${ETA_CASES[@]}"; do
  eta="${entry%%:*}"
  label="${entry#*:}"
  for rho in "${RHOS[@]}"; do
    mvn -q exec:java -Dexec.mainClass="ar.edu.itba.sds.tp2.Main" \
      -Dexec.args="--rho=${rho} --model=VOTER --eta=${eta} --steps=5000"
    mv output/observables.txt "output/observables_rho${rho}_${label}_voter.txt"
    echo "listo rho=${rho} eta=${eta} -> output/observables_rho${rho}_${label}_voter.txt"
  done
done

echo "Sweep completo."
