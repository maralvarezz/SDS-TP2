package ar.edu.itba.sds.tp2.rule;

import ar.edu.itba.sds.model.Particle;

import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Pieza intercambiable del loop de simulacion: dado el estado de una particula y sus vecinos,
 * decide cual es su nuevo angulo de velocidad. Vicsek promedia entre todos los vecinos, el
 * votante copia a uno solo elegido al azar.
 */
public interface DirectionRule {

    /**
     * @param self          la particula cuyo angulo se va a actualizar
     * @param neighbourIds  ids de sus vecinos segun el CIM (no incluye a self; puede venir vacio)
     * @param particlesById estado completo indexado por id, para resolver el angulo de cada vecino
     * @param eta           parametro de ruido de la corrida
     * @param random        fuente de aleatoriedad (para poder reproducir corridas con seed)
     * @return el nuevo angulo ya con el ruido aplicado
     */
    double nextAngle(Particle self, Set<Integer> neighbourIds, Map<Integer, Particle> particlesById, double eta, Random random);
}
