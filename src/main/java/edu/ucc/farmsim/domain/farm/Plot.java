package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.crop.Crop;

/**
 * One square of the farm map: its terrain and, on soil, the crop growing in it. Buildings are added by the
 * construction stories.
 *
 * <p>Stories: HU-01, HU-28.
 */
public final class Plot {

    private final Terrain terrain;
    private Crop crop;

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

    /** The crop growing here, or {@code null}. */
    public Crop crop() {
        return crop;
    }

    /** True if a crop (alive or withered) occupies the plot. */
    public boolean hasCrop() {
        return crop != null;
    }

    /**
     * Plants a crop.
     *
     * @throws IllegalStateException if the plot is not soil or already has a crop
     */
    public void plant(Crop newCrop) {
        if (terrain != Terrain.SOIL) {
            throw new IllegalStateException("Only soil can be sown: " + terrain);
        }
        if (crop != null) {
            throw new IllegalStateException("The plot already has a crop");
        }
        this.crop = newCrop;
    }

    /** Removes the crop (harvest or clearing). */
    public void removeCrop() {
        this.crop = null;
    }

    void restoreCrop(Crop restored) {
        this.crop = restored;
    }
}
