package edu.ucc.farmsim.domain.game;

/**
 * Un paso del final del día. Cada historia que agrega un efecto diario registra su propio paso en el {@link
 * DayPipeline}; el pipeline los ejecuta en el orden de su {@link DayPhase}.
 *
 * <p>Stories: HU-10.
 */
public interface DayStep {

    /** La fase de RN-03 a la que pertenece este paso. */
    DayPhase phase();

    /**
     * Aplica el paso a la partida.
     *
     * @param state  la partida que se avanza
     * @param report hechos del día que la aplicación convierte en eventos
     */
    void apply(GameState state, DayReport report);
}
