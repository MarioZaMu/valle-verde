package edu.ucc.farmsim.dominio.cultivo;

/**
 * Etapa de crecimiento de un cultivo (RN-08), más la etapa final de un cultivo muerto.
 *
 * <p>Stories: HU-01.
 */
public enum EtapaCultivo {
    SEED,
    SPROUT,
    GROWING,
    MATURE,
    WITHERED
}
