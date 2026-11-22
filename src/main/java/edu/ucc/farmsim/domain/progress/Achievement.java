package edu.ucc.farmsim.domain.progress;

/**
 * The achievements of RN-28. Some of them unlock buildings (RN-29), so the construction stories already need
 * to know which ones the player has; the conditions that grant them are evaluated by the progress stories.
 *
 * <p>Stories: HU-18.
 */
public enum Achievement {
    FIRST_HARVEST("LG-01"),
    GREEN_THUMB("LG-02"),
    BIG_HARVESTER("LG-03"),
    TRADER("LG-04"),
    CAPITALIST("LG-05"),
    BUILDER("LG-06"),
    BARNYARD_FARMER("LG-07"),
    VETERINARIAN("LG-08"),
    ONE_YEAR_IN_THE_VALLEY("LG-09");

    private final String code;

    Achievement(String code) {
        this.code = code;
    }

    /** The identifier of RN-28, for example {@code LG-02}. */
    public String code() {
        return code;
    }
}
