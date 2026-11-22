package edu.ucc.farmsim.domain.crop;

/**
 * Immutable copy of a crop for the memento. The stage is not stored: it follows from the progress.
 *
 * <p>Stories: HU-01.
 *
 * @param type             kind of crop
 * @param daysGrown        days it has grown
 * @param daysWithoutWater consecutive days it was not watered
 * @param watered          whether it was watered today
 * @param withered         whether it died
 */
public record CropSnapshot(CropType type, int daysGrown, int daysWithoutWater, boolean watered, boolean withered) { }
