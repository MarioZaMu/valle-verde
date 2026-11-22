package edu.ucc.farmsim.domain.farm;

/**
 * One square of the farm map. For now it only knows its terrain; crops and buildings are added by later
 * stories.
 *
 * <p>Stories: HU-28.
 */
public final class Plot {

    private final Terrain terrain;

    /**
     * Creates a plot.
     *
     * @param terrain kind of ground
     */
    public Plot(Terrain terrain) {
        this.terrain = terrain;
    }

    /** Kind of ground. */
    public Terrain terrain() {
        return terrain;
    }
}
