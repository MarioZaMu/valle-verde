package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.domain.game.GameState;
import org.springframework.stereotype.Component;

/**
 * Guarda la partida que se está jugando. Los servicios leen el estado desde aquí; cargar una partida construye un
 * estado nuevo aparte y lo cambia con {@link #replace(GameState)} solo cuando está completo y es válido.
 *
 * <p>Stories: HU-28.
 */
@Component
public class GameSession {

    private GameState current;

    /** Verdadero si hay una partida en curso. */
    public boolean hasGame() {
        return current != null;
    }

    /**
     * La partida en curso.
     *
     * @throws IllegalStateException si no hay ninguna (quien llama comprueba antes {@link #hasGame()})
     */
    public GameState current() {
        if (current == null) {
            throw new IllegalStateException("No game in progress");
        }
        return current;
    }

    /** Convierte el estado dado en la partida en curso. */
    public void replace(GameState state) {
        this.current = state;
    }
}
