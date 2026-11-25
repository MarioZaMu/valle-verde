package edu.ucc.farmsim.dominio.tiempo;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;

/**
 * Una fecha del calendario: año (desde 1), estación y día de la estación (1 a 28).
 *
 * <p>Stories: HU-09.
 *
 * @param anio   año, al menos {@link ReglasJuego#ANIO_INICIO}
 * @param estacion estación del año
 * @param dia    día de la estación, de 1 a {@link ReglasJuego#DIAS_POR_ESTACION}
 */
public record FechaJuego(int anio, Estacion estacion, int dia) {

    /** Valida los campos. */
    public FechaJuego {
        if (anio < ReglasJuego.ANIO_INICIO) {
            throw new IllegalArgumentException("year must be at least " + ReglasJuego.ANIO_INICIO + ": " + anio);
        }
        if (estacion == null) {
            throw new IllegalArgumentException("season is required");
        }
        if (dia < 1 || dia > ReglasJuego.DIAS_POR_ESTACION) {
            throw new IllegalArgumentException("day must be between 1 and " + ReglasJuego.DIAS_POR_ESTACION + ": " + dia);
        }
    }

    /** La fecha en que empieza una partida nueva: Año 1, Primavera, día 1 (RN-30). */
    public static FechaJuego start() {
        return new FechaJuego(ReglasJuego.ANIO_INICIO, Estacion.SPRING, ReglasJuego.DIA_INICIO);
    }
}
