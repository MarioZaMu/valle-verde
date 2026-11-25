package edu.ucc.farmsim.aplicacion.evento;

/**
 * Pattern: Observer — Role: Observer interface (anything that wants to hear about game events).
 *
 * <p>Stories: HU-10.
 */
public interface OyenteJuego {

    /** Lo llama el {@link BusEvento} cada vez que se publica un evento. */
    void onEvento(EventoJuego evento);
}
