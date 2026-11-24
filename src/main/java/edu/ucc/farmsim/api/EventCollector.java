package edu.ucc.farmsim.api;

import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Pattern: Observer — Role: ConcreteObserver (keeps the events of the current request so they travel in the response).
 *
 * <p>El servidor no tiene pantalla que refrescar: escucha su propio {@link EventBus} y entrega lo ocurrido al
 * controlador REST, que se lo devuelve al cliente (DT-19). Las peticiones se atienden de una en una (DT-20), así que
 * una lista simple basta.
 *
 * <p>Stories: none.
 */
@Component
public final class EventCollector implements GameListener {

    private final List<GameEvent> events = new ArrayList<>();

    /**
     * Empieza a escuchar el bus de eventos del servidor.
     *
     * @param bus donde los servicios publican los eventos
     */
    public EventCollector(EventBus bus) {
        bus.subscribe(this);
    }

    @Override
    public synchronized void onEvent(GameEvent event) {
        events.add(event);
    }

    /** Devuelve los eventos recogidos hasta ahora, en orden, y los olvida. */
    public synchronized List<GameEvent> drain() {
        List<GameEvent> collected = List.copyOf(events);
        events.clear();
        return collected;
    }
}
