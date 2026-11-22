package edu.ucc.farmsim.domain.time;

import java.util.Random;

/**
 * A reproducible random generator whose whole internal state is a single {@code long} (SplitMix64).
 *
 * <p>{@link Random} does not let anyone read its internal state, so a saved game could not continue the
 * same sequence. With {@code StateRandom}, {@link #state()} and {@link #restore(long)} make saving and
 * loading continue exactly the same draws (RN-32, DT-07). It is injected everywhere randomness is needed.
 *
 * <p>Stories: HU-11.
 */
public final class StateRandom extends Random {

    private static final long serialVersionUID = 1L;
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final long MIX_1 = 0xBF58476D1CE4E5B9L;
    private static final long MIX_2 = 0x94D049BB133111EBL;
    private static final int MIX_SHIFT_1 = 30;
    private static final int MIX_SHIFT_2 = 27;
    private static final int MIX_SHIFT_3 = 31;
    private static final int LONG_BITS = 64;

    // No initializer on purpose: Random's constructor calls setSeed, which fills this field first.
    private long state;

    /**
     * Creates a generator from a seed.
     *
     * @param seed initial seed; the same seed always gives the same sequence
     */
    public StateRandom(long seed) {
        super(seed);
    }

    /** The complete internal state, enough to continue the sequence later. */
    public long state() {
        return state;
    }

    /** Sets the internal state to a value previously returned by {@link #state()}. */
    public void restore(long savedState) {
        this.state = savedState;
    }

    @Override
    public synchronized void setSeed(long seed) {
        this.state = seed;
    }

    @Override
    protected int next(int bits) {
        state += GOLDEN_GAMMA;
        long z = state;
        z = (z ^ (z >>> MIX_SHIFT_1)) * MIX_1;
        z = (z ^ (z >>> MIX_SHIFT_2)) * MIX_2;
        z ^= z >>> MIX_SHIFT_3;
        return (int) (z >>> (LONG_BITS - bits));
    }
}
