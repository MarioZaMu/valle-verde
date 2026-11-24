package edu.ucc.farmsim.domain.farm;

/**
 * Tipo de suelo de una parcela (RN-04): solo la tierra se puede sembrar, solo el pasto admite construcciones, y el
 * avatar camina sobre pasto, tierra y camino.
 *
 * <p>Stories: HU-28, HU-50.
 */
public enum Terrain {
    SOIL,
    GRASS,
    WATER,
    OBSTACLE,
    /** El camino de tierra entre la granja y la aldea. */
    PATH,
    /** Una casa del valle. */
    HOUSE
}
