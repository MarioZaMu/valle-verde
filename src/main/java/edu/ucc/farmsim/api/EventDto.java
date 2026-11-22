package edu.ucc.farmsim.api;

import edu.ucc.farmsim.application.event.GameEvent;

/**
 * A game event as it travels in a response: its type name and its fields. The client picks the event class
 * by the name, so a new event only needs to be added to {@link GameEvent}.
 *
 * <p>Stories: none.
 *
 * @param type name of the event record, for example {@code DayEnded}
 * @param data the event itself (an immutable record of views)
 */
public record EventDto(String type, GameEvent data) {

    /** Wraps an event for the response. */
    public static EventDto of(GameEvent event) {
        return new EventDto(event.getClass().getSimpleName(), event);
    }
}
