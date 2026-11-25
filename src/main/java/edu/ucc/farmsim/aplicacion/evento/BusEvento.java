package edu.ucc.farmsim.aplicacion.evento;

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
public final class BusEvento {

    private static final Logger BITACORA = Logger.getLogger(BusEvento.class.getName());

    private final List<OyenteJuego> oyentes = new ArrayList<>();

    /** Empieza a entregar eventos a un oyente. */
    public void suscribir(OyenteJuego oyente) {
        oyentes.add(oyente);
    }

    /** Deja de entregar eventos a un oyente. */
    public void desuscribir(OyenteJuego oyente) {
        oyentes.remove(oyente);
    }

    /** Entrega un evento a cada oyente, en orden de suscripción. */
    public void publish(EventoJuego evento) {
        for (OyenteJuego oyente : new ArrayList<>(oyentes)) {
            try {
                oyente.onEvento(evento);
            } catch (RuntimeException e) {
                BITACORA.log(Level.WARNING, "A listener failed on " + evento, e);
            }
        }
    }
}
