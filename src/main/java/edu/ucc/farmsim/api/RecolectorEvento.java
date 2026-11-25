package edu.ucc.farmsim.api;

import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.evento.OyenteJuego;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Pattern: Observer — Role: ConcreteObserver (keeps the events of the current request so they travel in the response).
 *
 * <p>El servidor no tiene pantalla que refrescar: escucha su propio {@link BusEvento} y entrega lo ocurrido al
 * controlador REST, que se lo devuelve al cliente (DT-19). Las peticiones se atienden de una en una (DT-20), así que
 * una lista simple basta.
 *
 * <p>Stories: none.
 */
@Component
public final class RecolectorEvento implements OyenteJuego {

    private final List<EventoJuego> eventos = new ArrayList<>();

    /**
     * Empieza a escuchar el bus de eventos del servidor.
     *
     * @param bus donde los servicios publican los eventos
     */
    public RecolectorEvento(BusEvento bus) {
        bus.suscribir(this);
    }

    @Override
    public synchronized void onEvento(EventoJuego evento) {
        eventos.add(evento);
    }

    /** Devuelve los eventos recogidos hasta ahora, en orden, y los olvida. */
    public synchronized List<EventoJuego> drenar() {
        List<EventoJuego> recolectado = List.copyOf(eventos);
        eventos.clear();
        return recolectado;
    }
}
