package edu.ucc.farmsim.dominio.juego;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;

/**
 * La energía diaria del jugador, entre 0 y {@link ReglasJuego#ENERGIA_MAXIMA} (RN-02).
 *
 * <p>Las acciones pagan su costo con {@link #gastarIntentar(int)}; un pago rechazado no cambia nada. El final del día llama a
 * {@link #restaurar()}: la energía sobrante nunca se acumula.
 *
 * <p>Stories: HU-01, HU-08.
 */
public final class Energia {

    private int actual;

    /** Crea la energía completa. */
    public Energia() {
        this.actual = ReglasJuego.ENERGIA_MAXIMA;
    }

    private Energia(int actual) {
        this.actual = actual;
    }

    /**
     * Crea la energía con un valor dado (lo usan las pruebas y al restaurar una partida guardada).
     *
     * @throws IllegalArgumentException si el valor está fuera de 0..máximo
     */
    public static Energia conActual(int value) {
        exigirEnRango(value);
        return new Energia(value);
    }

    /** Energía disponible ahora. */
    public int actual() {
        return actual;
    }

    /** Energía máxima. */
    public int max() {
        return ReglasJuego.ENERGIA_MAXIMA;
    }

    /** Verdadero si ahora se puede pagar una acción que cuesta tanto (no se gasta nada). */
    public boolean puedeGastar(int costo) {
        return costo >= 0 && costo <= actual;
    }

    /**
     * Paga el costo de una acción.
     *
     * @return {@code true} si se pagó; {@code false} (y no se gasta nada) si la energía no alcanza
     * @throws IllegalArgumentException si el costo es negativo
     */
    public boolean gastarIntentar(int costo) {
        if (costo < 0) {
            throw new IllegalArgumentException("cost must not be negative: " + costo);
        }
        if (costo > actual) {
            return false;
        }
        actual -= costo;
        return true;
    }

    /** Devuelve la energía al máximo (paso 7 del final del día, RN-03). */
    public void restaurar() {
        actual = ReglasJuego.ENERGIA_MAXIMA;
    }

    /** Sobrescribe el valor (lo usa el memento al restaurar una partida). */
    public void set(int value) {
        exigirEnRango(value);
        actual = value;
    }

    private static void exigirEnRango(int value) {
        if (value < 0 || value > ReglasJuego.ENERGIA_MAXIMA) {
            throw new IllegalArgumentException("energy must be between 0 and " + ReglasJuego.ENERGIA_MAXIMA + ": " + value);
        }
    }
}
