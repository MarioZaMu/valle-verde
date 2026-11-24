package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: State interface (the behaviour that changes with the season).
 *
 * <p>Cada estación sabe qué estación le sigue. Los objetos de estado no guardan datos y se comparten.
 *
 * <p>Stories: HU-09, HU-11.
 */
public interface SeasonState {

    /** La estación que representa este estado. */
    Season season();

    /** El estado que sigue a este en el calendario (después del Invierno viene la Primavera). */
    SeasonState next();

    /** La estrategia que sortea el clima de los días de esta estación (RN-11). */
    WeatherStrategy weatherStrategy();

    /** Devuelve el objeto de estado compartido de una estación. */
    static SeasonState of(Season season) {
        return switch (season) {
            case SPRING -> SpringState.INSTANCE;
            case SUMMER -> SummerState.INSTANCE;
            case AUTUMN -> AutumnState.INSTANCE;
            case WINTER -> WinterState.INSTANCE;
        };
    }
}
