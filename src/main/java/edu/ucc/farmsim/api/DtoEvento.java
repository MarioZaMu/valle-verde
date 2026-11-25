package edu.ucc.farmsim.api;

import edu.ucc.farmsim.aplicacion.evento.EventoJuego;

/**
 * Un evento del juego tal como viaja en una respuesta: el nombre de su tipo y sus campos. El cliente elige la clase del
 * evento por el nombre, así que un evento nuevo solo necesita agregarse a {@link EventoJuego}.
 *
 * <p>Stories: none.
 *
 * @param tipo nombre del record del evento, por ejemplo {@code DiaTerminado}
 * @param datos el evento en sí (un record inmutable de vistas)
 */
public record DtoEvento(String tipo, EventoJuego datos) {

    /** Envuelve un evento para la respuesta. */
    public static DtoEvento of(EventoJuego evento) {
        return new DtoEvento(evento.getClass().getSimpleName(), evento);
    }
}
