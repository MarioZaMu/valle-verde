package edu.ucc.farmsim.domain.farm;

/**
 * Kind of ground of a plot (RN-04): only soil can be sown, only grass can be built on, and the avatar walks on
 * grass, soil and the path.
 *
 * <p>Stories: HU-28, HU-50.
 */
public enum Terrain {
    SOIL,
    GRASS,
    WATER,
    OBSTACLE,
    /** The dirt path between the farm and the village. */
    PATH,
    /** A house of the valley. */
    HOUSE
}
