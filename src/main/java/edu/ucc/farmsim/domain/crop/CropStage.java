package edu.ucc.farmsim.domain.crop;

/**
 * Etapa de crecimiento de un cultivo (RN-08), más la etapa final de un cultivo muerto.
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
