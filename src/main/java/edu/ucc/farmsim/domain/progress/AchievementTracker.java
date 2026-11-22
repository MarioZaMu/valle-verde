package edu.ucc.farmsim.domain.progress;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Keeps the achievements the player has unlocked. An achievement is granted once and never revoked (RN-28);
 * the conditions that grant them are added by the progress stories.
 *
 * <p>Stories: HU-18.
 */
public final class AchievementTracker {

    private final Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);

    /** True if the player has the achievement. */
    public boolean has(Achievement achievement) {
        return unlocked.contains(achievement);
    }

    /**
     * Grants an achievement.
     *
     * @return {@code true} if it was new; {@code false} if the player already had it
     */
    public boolean unlock(Achievement achievement) {
        return unlocked.add(achievement);
    }

    /** An immutable copy of the unlocked achievements, for the memento. */
    public Set<Achievement> snapshot() {
        return unlocked.isEmpty() ? Set.of() : Set.copyOf(unlocked);
    }

    /** Replaces the unlocked achievements with a copy taken earlier. */
    public void restoreFrom(Collection<Achievement> saved) {
        unlocked.clear();
        unlocked.addAll(saved);
    }
}
