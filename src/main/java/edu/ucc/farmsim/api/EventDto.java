package edu.ucc.farmsim.api;

import edu.ucc.farmsim.application.event.GameEvent;

/**
 * Un evento del juego tal como viaja en una respuesta: el nombre de su tipo y sus campos. El cliente elige la clase del
 * evento por el nombre, así que un evento nuevo solo necesita agregarse a {@link GameEvent}.
 *
 * <p>Stories: none.
 *
 * @param type nombre del record del evento, por ejemplo {@code DayEnded}
 * @param data el evento en sí (un record inmutable de vistas)
 */
public record EventDto(String type, GameEvent data) {

    /** Envuelve un evento para la respuesta. */
    public static EventDto of(GameEvent event) {
        return new EventDto(event.getClass().getSimpleName(), event);
    }
}
