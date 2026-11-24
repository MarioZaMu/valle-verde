package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * La energía diaria del jugador, entre 0 y {@link GameRules#MAX_ENERGY} (RN-02).
 *
 * <p>Las acciones pagan su costo con {@link #trySpend(int)}; un pago rechazado no cambia nada. El final del día llama a
 * {@link #restore()}: la energía sobrante nunca se acumula.
 *
 * <p>Stories: HU-01, HU-08.
 */
public final class Energy {

    private int current;

    /** Crea la energía completa. */
    public Energy() {
        this.current = GameRules.MAX_ENERGY;
    }

    private Energy(int current) {
        this.current = current;
    }

    /**
     * Crea la energía con un valor dado (lo usan las pruebas y al restaurar una partida guardada).
     *
     * @throws IllegalArgumentException si el valor está fuera de 0..máximo
     */
    public static Energy withCurrent(int value) {
        requireInRange(value);
        return new Energy(value);
    }

    /** Energía disponible ahora. */
    public int current() {
        return current;
    }

    /** Energía máxima. */
    public int max() {
        return GameRules.MAX_ENERGY;
    }

    /** Verdadero si ahora se puede pagar una acción que cuesta tanto (no se gasta nada). */
    public boolean canSpend(int cost) {
        return cost >= 0 && cost <= current;
    }

    /**
     * Paga el costo de una acción.
     *
     * @return {@code true} si se pagó; {@code false} (y no se gasta nada) si la energía no alcanza
     * @throws IllegalArgumentException si el costo es negativo
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

    /** Devuelve la energía al máximo (paso 7 del final del día, RN-03). */
    public void restore() {
        current = GameRules.MAX_ENERGY;
    }

    /** Sobrescribe el valor (lo usa el memento al restaurar una partida). */
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
