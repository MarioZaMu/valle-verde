package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.domain.game.GameState;
import org.springframework.stereotype.Component;

/**
 * Holds the game currently being played. Services read the state from here; loading a game builds a new
 * state on the side and swaps it in with {@link #replace(GameState)} only when it is complete and valid.
 *
 * <p>Stories: HU-28.
 */
@Component
public class GameSession {

    private GameState current;

    /** True if a game is in progress. */
    public boolean hasGame() {
        return current != null;
    }

    /**
     * The game in progress.
     *
     * @throws IllegalStateException if there is none (callers check {@link #hasGame()} first)
     */
    public GameState current() {
        if (current == null) {
            throw new IllegalStateException("No game in progress");
        }
        return current;
    }

    /** Makes the given state the game in progress. */
    public void replace(GameState state) {
        this.current = state;
    }
}
