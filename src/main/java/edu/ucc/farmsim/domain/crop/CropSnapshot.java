package edu.ucc.farmsim.domain.crop;

/**
 * Copia inmutable de un cultivo para el memento. La etapa no se guarda: se deduce del progreso.
 *
 * <p>Stories: HU-01.
 *
 * @param type             tipo de cultivo
 * @param daysGrown        días que ha crecido
 * @param daysWithoutWater días consecutivos sin regar
 * @param watered          si fue regado hoy
 * @param withered         si murió
 */
public record CropSnapshot(CropType type, int daysGrown, int daysWithoutWater, boolean watered, boolean withered) { }
