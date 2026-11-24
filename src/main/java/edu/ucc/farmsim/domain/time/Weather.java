package edu.ucc.farmsim.domain.time;

/**
 * Clima de un día (RN-11). El orden de declaración coincide con las columnas de las tablas del clima de {@code
 * GameRules}.
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
