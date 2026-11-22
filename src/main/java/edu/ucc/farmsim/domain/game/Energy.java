package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * The player's daily energy, between 0 and {@link GameRules#MAX_ENERGY} (RN-02).
 *
 * <p>Actions pay their cost with {@link #trySpend(int)}; a rejected payment changes nothing.
 * The end of the day calls {@link #restore()}: leftover energy never accumulates.
 *
 * <p>Stories: HU-08.
 */
public final class Energy {

    private int current;

    /** Creates full energy. */
    public Energy() {
        this.current = GameRules.MAX_ENERGY;
    }

    private Energy(int current) {
        this.current = current;
    }

    /**
     * Creates energy with a given value (used by tests and when restoring a saved game).
     *
     * @throws IllegalArgumentException if the value is outside 0..maximum
     */
    public static Energy withCurrent(int value) {
        requireInRange(value);
        return new Energy(value);
    }

    /** Energy available now. */
    public int current() {
        return current;
    }

    /** Maximum energy. */
    public int max() {
        return GameRules.MAX_ENERGY;
    }

    /**
     * Pays an action cost.
     *
     * @return {@code true} if paid; {@code false} (and nothing spent) if energy is insufficient
     * @throws IllegalArgumentException if the cost is negative
     */
    public boolean trySpend(int cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("cost must not be negative: " + cost);
        }
        if (cost > current) {
            return false;
        }
        current -= cost;
        return true;
    }

    /** Sets energy back to the maximum (step 7 of the end of the day, RN-03). */
    public void restore() {
        current = GameRules.MAX_ENERGY;
    }

    /** Overwrites the value (used by the memento when restoring a game). */
    public void set(int value) {
        requireInRange(value);
        current = value;
    }

    private static void requireInRange(int value) {
        if (value < 0 || value > GameRules.MAX_ENERGY) {
            throw new IllegalArgumentException("energy must be between 0 and " + GameRules.MAX_ENERGY + ": " + value);
        }
    }
}
