package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.evento.OyenteJuego;
import edu.ucc.farmsim.interfaz.tema.Textos;

/**
 * Pattern: Observer — Role: ConcreteObserver (keeps the top-bar data up to date from game events).
 *
 * <p>No tiene Swing: escucha el bus de eventos, recuerda el último {@link VistaHud} y expone los textos en español que
 * se muestran. El {@link HudPanel} de Swing solo pinta esos textos, de modo que la lógica del HUD se prueba sin
 * pantalla y la pantalla se actualiza por eventos, nunca por sondeo.
 *
 * <p>Stories: HU-34, HU-48.
 */
public class ModeloHud implements OyenteJuego {

    private VistaHud hud;

    @Override
    public void onEvento(EventoJuego evento) {
        if (evento instanceof EventoJuego.JuegoIniciado iniciado) {
            hud = iniciado.hud();
        } else if (evento instanceof EventoJuego.DiaTerminado terminado) {
            hud = terminado.hud();
        } else if (evento instanceof EventoJuego.EnergiaCambiada cambiado && hud != null) {
            hud = new VistaHud(hud.dinero(), hud.anio(), hud.estacion(), hud.dia(), hud.clima(), hud.pronostico(),
                    cambiado.energia(), cambiado.energiaMaxima());
        } else if (evento instanceof EventoJuego.DineroCambiado cambiado && hud != null) {
            hud = new VistaHud(cambiado.dinero(), hud.anio(), hud.estacion(), hud.dia(), hud.clima(), hud.pronostico(),
                    hud.energia(), hud.energiaMaxima());
        }
    }

    /** Dinero, por ejemplo {@code 500 m}. */
    public String textoDinero() {
        return hud == null ? Textos.NO_VALOR : Textos.dinero(hud.dinero());
    }

    /** Fecha, por ejemplo {@code Año 1 · Primavera · Día 1}. */
    public String textoFecha() {
        return hud == null ? Textos.NO_VALOR : Textos.fecha(hud.anio(), hud.estacion(), hud.dia());
    }

    /** Clima de hoy, por ejemplo {@code Soleado}. */
    public String textoClima() {
        return hud == null ? Textos.NO_VALOR : Textos.clima(hud.clima());
    }

    /** Pronóstico de mañana, por ejemplo {@code Mañana: Lluvioso}. */
    public String textoPronostico() {
        return hud == null ? Textos.NO_VALOR : Textos.pronostico(hud.pronostico());
    }

    /** Id de la estación, por ejemplo {@code SPRING}, o {@code null} antes de que empiece una partida. */
    public String idEstacion() {
        return hud == null ? null : hud.estacion();
    }

    /** Id del clima de hoy, por ejemplo {@code RAINY}, o {@code null} antes de que empiece una partida. */
    public String idClima() {
        return hud == null ? null : hud.clima();
    }

    /** Id del clima de mañana, o {@code null} antes de que empiece una partida. */
    public String idPronostico() {
        return hud == null ? null : hud.pronostico();
    }

    /** Energía como texto, por ejemplo {@code 98/100}: la información nunca depende solo de un color. */
    public String textoEnergia() {
        return hud == null ? Textos.NO_VALOR + "/" + Textos.NO_VALOR : Textos.energia(hud.energia(), hud.energiaMaxima());
    }

    /** Energía como una fracción de 0 a 1, para la barra. */
    public double fraccionEnergia() {
        return hud == null || hud.energiaMaxima() == 0 ? 0.0 : (double) hud.energia() / hud.energiaMaxima();
    }
}
