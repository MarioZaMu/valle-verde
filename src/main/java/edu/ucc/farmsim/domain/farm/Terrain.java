package edu.ucc.farmsim.domain.farm;

/**
 * Kind of ground of a plot (RN-04): only soil can be sown, only grass can be built on.
 *
 * <p>Stories: HU-28.
 */
public enum Terrain {
    SOIL,
    GRASS,
    WATER,
    OBSTACLE
}
