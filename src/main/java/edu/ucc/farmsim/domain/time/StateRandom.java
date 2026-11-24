package edu.ucc.farmsim.domain.time;

import java.util.Random;

/**
 * Un generador aleatorio reproducible cuyo estado interno completo es un solo {@code long} (SplitMix64).
 *
 * <p>{@link Random} no deja leer su estado interno, así que una partida guardada no podría continuar la misma
 * secuencia. Con {@code StateRandom}, {@link #state()} y {@link #restore(long)} hacen que guardar y cargar continúen
 * exactamente los mismos sorteos (RN-32, DT-07). Se inyecta en todo lugar donde hace falta azar.
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

        // Sin inicializador a propósito: el constructor de Random llama a setSeed, que llena este campo primero.
    private long state;

    /**
     * Crea un generador a partir de una semilla.
     *
     * @param seed semilla inicial; la misma semilla siempre da la misma secuencia
     */
    public StateRandom(long seed) {
        super(seed);
    }

    /** El estado interno completo, suficiente para continuar la secuencia más tarde. */
    public long state() {
        return state;
    }

    /** Fija el estado interno en un valor devuelto antes por {@link #state()}. */
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
