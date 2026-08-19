package ar.edu.itba.sds.tp2.config;

/**
 * Los dos escenarios que pide el TP2: el modelo estandar de Vicsek (promedia direcciones de
 * vecinos) y el modelo de votante (copia la direccion de un solo vecino elegido al azar).
 */
public enum FlockingModel {
    VICSEK,
    VOTER
}
