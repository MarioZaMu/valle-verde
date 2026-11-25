package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.evento.OyenteJuego;
import edu.ucc.farmsim.interfaz.tema.Textos;

/**
 * Pattern: Observer — Role: ConcreteObserver (turns game events into notices).
 *
 * <p>Escucha el bus de eventos: un logro desbloqueado y el comienzo de una estación nueva se vuelven avisos, y la
 * pantalla le pide directamente los errores y la información que vienen de las órdenes del propio jugador. No tiene
 * Swing.
 *
 * <p>Stories: HU-26, HU-39, HU-47, HU-49.
 */
public final class PresentadorAviso implements OyenteJuego {

    private final ColaAviso cola;
    private String estacion;

    /**
     * Crea el presentador.
     *
     * @param cola donde se publican los avisos
     */
    public PresentadorAviso(ColaAviso cola) {
        this.cola = cola;
    }

    @Override
    public void onEvento(EventoJuego evento) {
        if (evento instanceof EventoJuego.JuegoIniciado iniciado) {
            estacion = iniciado.hud().estacion();
        } else if (evento instanceof EventoJuego.DiaTerminado terminado) {
            String now = terminado.hud().estacion();
            if (estacion != null && !estacion.equals(now)) {
                cola.post(CategoriaAviso.INFO, Textos.estacionIniciada(now));
            }
            estacion = now;
        } else if (evento instanceof EventoJuego.LogroDesbloqueado desbloqueado) {
            cola.post(CategoriaAviso.ACHIEVEMENT, Textos.logroDesbloqueado(desbloqueado.name(), desbloqueado.recompensa()));
        } else if (evento instanceof EventoJuego.EncargoCompletado encargo) {
            cola.post(CategoriaAviso.INFO, Textos.encargoCompletado(encargo.name(), encargo.recompensa()));
        } else if (evento instanceof EventoJuego.TutorialAvanzado avanzado && "DONE".equals(avanzado.tutorial().paso())) {
            cola.post(CategoriaAviso.INFO, avanzado.tutorial().texto());
        }
    }

    /** Un rechazo: el mensaje de RN-35 como aviso de error. */
    public void error(String mensaje) {
        cola.post(CategoriaAviso.ERROR, mensaje);
    }

    /** Información para el jugador, como un aviso que vino con una orden exitosa. */
    public void info(String mensaje) {
        cola.post(CategoriaAviso.INFO, mensaje);
    }
}
