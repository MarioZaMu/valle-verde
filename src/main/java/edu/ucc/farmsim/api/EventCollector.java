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
 * <p>The server has no screen to refresh: it listens to its own {@link EventBus} and hands what happened to
 * the REST controller, which returns it to the client (DT-19). Requests are served one at a time (DT-20), so
 * a plain list is enough.
 *
 * <p>Stories: none.
 */
@Component
public final class EventCollector implements GameListener {

    private final List<GameEvent> events = new ArrayList<>();

    /**
     * Starts listening to the server's event bus.
     *
     * @param bus where the services publish events
     */
    public EventCollector(EventBus bus) {
        bus.subscribe(this);
    }

    @Override
    public synchronized void onEvent(GameEvent event) {
        events.add(event);
    }

    /** Returns the events collected so far, in order, and forgets them. */
    public synchronized List<GameEvent> drain() {
        List<GameEvent> collected = List.copyOf(events);
        events.clear();
        return collected;
    }
}
