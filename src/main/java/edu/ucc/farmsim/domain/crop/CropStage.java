package edu.ucc.farmsim.domain.crop;

/**
 * Growth stage of a crop (RN-08), plus the terminal stage of a dead crop.
 *
 * <p>Stories: HU-01.
 */
public enum CropStage {
    SEED,
    SPROUT,
    GROWING,
    MATURE,
    WITHERED
}
