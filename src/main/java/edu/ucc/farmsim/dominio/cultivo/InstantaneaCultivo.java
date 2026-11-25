package edu.ucc.farmsim.dominio.cultivo;

/**
 * Copia inmutable de un cultivo para el memento. La etapa no se guarda: se deduce del progreso.
 *
 * <p>Stories: HU-01.
 *
 * @param tipo             tipo de cultivo
 * @param diasCrecidos        días que ha crecido
 * @param diasSinAgua días consecutivos sin regar
 * @param regado          si fue regado hoy
 * @param marchito         si murió
 */
public record InstantaneaCultivo(TipoCultivo tipo, int diasCrecidos, int diasSinAgua, boolean regado, boolean marchito) { }
