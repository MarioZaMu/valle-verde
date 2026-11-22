package edu.ucc.farmsim.domain.progress;

import edu.ucc.farmsim.domain.config.GameRules;
import java.util.function.Predicate;

/**
 * The achievements of RN-28 with their reward and the condition that grants each one. Some of them unlock
 * buildings (RN-29).
 *
 * <p>Stories: HU-18, HU-26.
 */
public enum Achievement {
    FIRST_HARVEST("LG-01", GameRules.REWARD_FIRST_HARVEST, f -> f.harvests() >= 1),
    GREEN_THUMB("LG-02", GameRules.REWARD_GREEN_THUMB, f -> f.harvests() >= GameRules.HARVESTS_GREEN_THUMB),
    BIG_HARVESTER("LG-03", GameRules.REWARD_BIG_HARVESTER, f -> f.harvests() >= GameRules.HARVESTS_BIG_HARVESTER),
    TRADER("LG-04", GameRules.REWARD_TRADER, f -> f.unitsSold() >= GameRules.UNITS_SOLD_TRADER),
    CAPITALIST("LG-05", GameRules.REWARD_CAPITALIST, f -> f.money() >= GameRules.BALANCE_CAPITALIST),
    BUILDER("LG-06", GameRules.REWARD_BUILDER, f -> f.buildingsBuilt() >= 1),
    BARNYARD_FARMER("LG-07", GameRules.REWARD_BARNYARD_FARMER,
            f -> f.animals() >= GameRules.ANIMALS_BARNYARD_FARMER),
    VETERINARIAN("LG-08", GameRules.REWARD_VETERINARIAN, f -> f.animalsCured() >= 1),
    ONE_YEAR_IN_THE_VALLEY("LG-09", GameRules.REWARD_ONE_YEAR_IN_THE_VALLEY,
            f -> f.year() >= GameRules.YEAR_ONE_YEAR_IN_THE_VALLEY);

    private final String code;
    private final int reward;
    private final Predicate<ProgressFacts> condition;

    Achievement(String code, int reward, Predicate<ProgressFacts> condition) {
        this.code = code;
        this.reward = reward;
        this.condition = condition;
    }

    /** The identifier of RN-28, for example {@code LG-02}. */
    public String code() {
        return code;
    }

    /** Money in m paid when the achievement is unlocked. */
    public int reward() {
        return reward;
    }

    /** True if the facts meet the condition of this achievement. */
    public boolean isMetBy(ProgressFacts facts) {
        return condition.test(facts);
    }
}
