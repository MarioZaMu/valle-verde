package edu.ucc.farmsim.dominio.guardado;

import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import java.time.Instant;

/**
 * Lo que muestra la lista de ranuras sobre una ranura (RN-31): vacía, dañada, o la fecha, el dinero y el momento del
 * guardado.
 *
 * <p>Stories: HU-32.
 *
 * @param ranura    número de ranura, desde 1
 * @param status  vacía, utilizable o dañada
 * @param fecha    fecha del calendario de la partida guardada, o {@code null} si la ranura no es utilizable
 * @param dinero   dinero de la partida guardada, 0 si la ranura no es utilizable
 * @param archivadoEn cuándo se guardó la partida, o {@code null} si la ranura no es utilizable
 */
public record InfoRanura(int ranura, Situacion status, FechaJuego fecha, int dinero, Instant archivadoEn) {

    /** Estado de una ranura. */
    public enum Situacion { EMPTY, OK, DAMAGED }

    /** Una ranura sin documento. */
    public static InfoRanura empty(int ranura) {
        return new InfoRanura(ranura, Situacion.EMPTY, null, 0, null);
    }

    /** Una ranura cuyo documento no se puede leer. */
    public static InfoRanura danado(int ranura) {
        return new InfoRanura(ranura, Situacion.DAMAGED, null, 0, null);
    }

    /** Una ranura utilizable. */
    public static InfoRanura ok(int ranura, FechaJuego fecha, int dinero, Instant archivadoEn) {
        return new InfoRanura(ranura, Situacion.OK, fecha, dinero, archivadoEn);
    }
}
