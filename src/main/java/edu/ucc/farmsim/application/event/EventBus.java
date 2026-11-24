package edu.ucc.farmsim.application.event;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pattern: Observer — Role: Subject (publishes events to its listeners).
 *
 * <p>La entrega es síncrona, en el hilo de quien llama (el hilo de Swing), después de que la operación tuvo éxito
 * (DT-05). Un oyente que falla se registra en el log y se omite: nunca detiene a los demás ni rompe el juego.
 *
 * <p>Stories: HU-10.
 */
public final class EventBus {

    private static final Logger LOG = Logger.getLogger(EventBus.class.getName());

    private final List<GameListener> listeners = new ArrayList<>();

    /** Empieza a entregar eventos a un oyente. */
    public void subscribe(GameListener listener) {
        listeners.add(listener);
    }

    /** Deja de entregar eventos a un oyente. */
    public void unsubscribe(GameListener listener) {
        listeners.remove(listener);
    }

    /** Entrega un evento a cada oyente, en orden de suscripción. */
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
