package edu.ucc.farmsim.application.event;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pattern: Observer — Role: Subject (publishes events to its listeners).
 *
 * <p>Delivery is synchronous, on the caller's thread (the Swing thread), after the operation succeeded
 * (DT-05). A listener that fails is logged and skipped: it never stops the others or breaks the game.
 *
 * <p>Stories: HU-10.
 */
public final class EventBus {

    private static final Logger LOG = Logger.getLogger(EventBus.class.getName());

    private final List<GameListener> listeners = new ArrayList<>();

    /** Starts delivering events to a listener. */
    public void subscribe(GameListener listener) {
        listeners.add(listener);
    }

    /** Stops delivering events to a listener. */
    public void unsubscribe(GameListener listener) {
        listeners.remove(listener);
    }

    /** Delivers an event to every listener, in subscription order. */
    public void publish(GameEvent event) {
        for (GameListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "A listener failed on " + event, e);
            }
        }
    }
}
