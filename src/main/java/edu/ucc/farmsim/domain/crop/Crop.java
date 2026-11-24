package edu.ucc.farmsim.domain.crop;

import edu.ucc.farmsim.domain.time.Season;

/**
 * Pattern: State — Role: Context (a plant whose behaviour depends on its growth state).
 *
 * <p>Guarda lo que siguen RN-06 a RN-10 de una planta sembrada: días de crecimiento, días sin agua y si fue regada hoy.
 * Su etapa se deriva del progreso y se guarda como un objeto de estado.
 *
 * <p>Stories: HU-01, HU-02, HU-03, HU-05.
 */
public final class Crop {

    private final CropType type;
    private int daysGrown;
    private int daysWithoutWater;
    private boolean watered;
    private CropState state;

    /** Crea un cultivo recién sembrado: etapa Semilla, sin crecimiento, sin regar (RN-06). */
    public Crop(CropType type) {
        this(type, 0, 0, false, false);
    }

    private Crop(CropType type, int daysGrown, int daysWithoutWater, boolean watered, boolean withered) {
        this.type = type;
        this.daysGrown = daysGrown;
        this.daysWithoutWater = daysWithoutWater;
        this.watered = watered;
        this.state = CropState.forProgress(type, daysGrown, withered);
    }

    /** Reconstruye un cultivo desde una instantánea (memento, pruebas). */
    public static Crop fromSnapshot(CropSnapshot snapshot) {
        return new Crop(snapshot.type(), snapshot.daysGrown(), snapshot.daysWithoutWater(),
                snapshot.watered(), snapshot.withered());
    }

    /** Una copia inmutable de este cultivo. */
    public CropSnapshot snapshot() {
        return new CropSnapshot(type, daysGrown, daysWithoutWater, watered, isWithered());
    }

    /** Tipo de cultivo. */
    public CropType type() {
        return type;
    }

    /** Días que ha crecido el cultivo. */
    public int daysGrown() {
        return daysGrown;
    }

    /** Días consecutivos sin agua. */
    public int daysWithoutWater() {
        return daysWithoutWater;
    }

    /** Si fue regado hoy. */
    public boolean isWatered() {
        return watered;
    }

    /** Etapa de crecimiento actual. */
    public CropStage stage() {
        return state.stage();
    }

    /** Verdadero salvo que el cultivo esté marchito. */
    public boolean isAlive() {
        return state.isAlive();
    }

    /** Verdadero cuando el cultivo se puede cosechar. */
    public boolean isMature() {
        return state.canHarvest();
    }

    /** Verdadero cuando el cultivo murió. */
    public boolean isWithered() {
        return state.stage() == CropStage.WITHERED;
    }

    /**
     * Riega el cultivo por el resto del día (RN-07).
     *
     * @return {@code true} si fue regado; {@code false} si está marchito o ya fue regado hoy
     */
    public boolean water() {
        if (!isAlive() || watered) {
            return false;
        }
        watered = true;
        return true;
    }

    /**
     * Resuelve el final del día de este cultivo (paso 2 de RN-03): el estado actual decide qué pasa y la marca de riego
     * se borra para el día siguiente.
     */
    public void endOfDay(int dryDaysToWither) {
        state.onDayEnd(this, dryDaysToWither);
        watered = false;
    }

    /**
     * Marchita el cultivo por una helada a la intemperie (RN-09-b), sea cual sea su etapa.
     *
     * @return {@code true} si se marchitó ahora; {@code false} si ya lo estaba
     */
    public boolean witherByFrost() {
        if (!isAlive()) {
            return false;
        }
        wither();
        return true;
    }

    /**
     * Marchita el cultivo si la estación dada no es una de sus estaciones válidas (RN-09-c), sea cual sea su etapa.
     *
     * @return {@code true} si se marchitó ahora
     */
    public boolean witherIfOutOfSeason(Season season) {
        if (isAlive() && !type.validSeasons().contains(season)) {
            wither();
            return true;
        }
        return false;
    }

    void growOneDay() {
        daysGrown++;
        daysWithoutWater = 0;
        state = CropState.forProgress(type, daysGrown, false);
    }

    void wither() {
        state = WitheredState.INSTANCE;
    }

    void missWater() {
        daysWithoutWater++;
    }

    /** Días que aún faltan para madurar (0 cuando está maduro o marchito). */
    public int daysLeft() {
        return isAlive() ? Math.max(0, type.daysToMature() - daysGrown) : 0;
    }
}
