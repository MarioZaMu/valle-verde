package edu.ucc.farmsim.domain.time;

/**
 * Weather of a day (RN-11). The declaration order matches the columns of the weather tables in
 * {@code GameRules}.
 *
 * <p>Stories: HU-11.
 */
public enum Weather {
    SUNNY,
    CLOUDY,
    RAINY,
    DROUGHT,
    FROST,
    SNOWY
}
