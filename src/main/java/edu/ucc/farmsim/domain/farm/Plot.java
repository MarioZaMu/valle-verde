package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.crop.Crop;

/**
 * Un cuadro del mapa de la granja: su terreno, el cultivo que crece en él (sobre tierra) o el edificio que lo cubre
 * (sobre pasto).
 *
 * <p>Stories: HU-01, HU-25, HU-28.
 */
public final class Plot {

    private final Terrain terrain;
    private Crop crop;
    private Building building;

    /**
     * Crea una parcela.
     *
     * @param terrain tipo de suelo
     */
    public Plot(Terrain terrain) {
        this.terrain = terrain;
    }

    /** Tipo de suelo. */
    public Terrain terrain() {
        return terrain;
    }

    /** El cultivo que crece aquí, o {@code null}. */
    public Crop crop() {
        return crop;
    }

    /** Verdadero si un cultivo (vivo o marchito) ocupa la parcela. */
    public boolean hasCrop() {
        return crop != null;
    }

    /**
     * Planta un cultivo.
     *
     * @throws IllegalStateException si la parcela no es tierra o ya tiene un cultivo
     */
    public void plant(Crop newCrop) {
        if (!canBeSown()) {
            throw new IllegalStateException("Only soil or a greenhouse plot can be sown: " + terrain);
        }
        if (crop != null) {
            throw new IllegalStateException("The plot already has a crop");
        }
        this.crop = newCrop;
    }

    /** Verdadero si un invernadero cubre la parcela: su cultivo es un cultivo protegido (RN-22). */
    public boolean isSheltered() {
        return building != null && building.type() == BuildingType.GREENHOUSE;
    }

    /** Verdadero si aquí se puede sembrar: tierra, o una parcela de un invernadero (RN-06, RN-22). */
    public boolean canBeSown() {
        return terrain == Terrain.SOIL || isSheltered();
    }

    /** Quita el cultivo (cosecha o limpieza). */
    public void removeCrop() {
        this.crop = null;
    }

    /** El edificio que cubre esta parcela, o {@code null}. */
    public Building building() {
        return building;
    }

    void setBuilding(Building covering) {
        this.building = covering;
    }

    void restoreCrop(Crop restored) {
        this.crop = restored;
    }
}
