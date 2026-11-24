package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: State interface (what a crop can do at each growth stage).
 *
 * <p>Los estados no guardan datos y se comparten; un {@link Crop} guarda el actual y lo cambia a medida que crece, de
 * modo que ningún código necesita una cadena de {@code if} sobre la etapa.
 *
 * <p>Stories: HU-01, HU-03, HU-05.
 */
public interface CropState {

    /** La etapa que representa este estado. */
    CropStage stage();

    /** Verdadero salvo que el cultivo esté marchito. */
    boolean isAlive();

    /** Verdadero solo cuando el cultivo está maduro y se puede cosechar. */
    boolean canHarvest();

    /**
     * Lo que le pasa al cultivo cuando termina el día (paso 2 de RN-03): un cultivo joven vivo que fue regado crece;
     * uno que no lo fue cuenta un día sin agua y se marchita al llegar al límite. Un cultivo maduro o marchito no hace
     * nada.
     *
     * @param crop            el cultivo cuyo día termina
     * @param dryDaysToWither días consecutivos sin agua que matan a un cultivo joven (3, o 2 en una sequía)
     */
    void onDayEnd(Crop crop, int dryDaysToWither);

    /**
     * El estado que corresponde al progreso de un cultivo (RN-08): Semilla con 0 días; Brote hasta la mitad de los días
     * para madurar; Crecimiento hasta completarlos; Maduro después; Marchito si murió.
     */
    static CropState forProgress(CropType type, int daysGrown, boolean withered) {
        if (withered) {
            return WitheredState.INSTANCE;
        }
        if (daysGrown >= type.daysToMature()) {
            return MatureState.INSTANCE;
        }
        if (daysGrown >= type.halfwayDays()) {
            return GrowingState.INSTANCE;
        }
        if (daysGrown >= 1) {
            return SproutState.INSTANCE;
        }
        return SeedState.INSTANCE;
    }
}
