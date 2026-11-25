package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import org.springframework.stereotype.Service;

/**
 * Inicia partidas (y, en fases posteriores, sigue los logros y los desbloqueos).
 *
 * <p>Stories: HU-28.
 */
@Service
public final class ServicioProgreso {

    private final SesionJuego sesion;
    private final MapeadorVista mapeador;
    private final BusEvento eventos;

    /**
     * Crea el servicio.
     *
     * @param sesion la partida en curso
     * @param mapeador  construye las vistas que dibuja la UI
     * @param eventos  donde se publican los eventos
     */
    public ServicioProgreso(SesionJuego sesion, MapeadorVista mapeador, BusEvento eventos) {
        this.sesion = sesion;
        this.mapeador = mapeador;
        this.eventos = eventos;
    }

    /** Crea el estado inicial de RN-30, lo convierte en la partida en curso y lo anuncia. */
    public Resultado<VistaHud> juegoNuevo(long semilla) {
        EstadoJuego estado = EstadoJuego.juegoNuevo(semilla);
        sesion.replace(estado);
        VistaHud hud = mapeador.hud(estado);
        eventos.publish(new EventoJuego.JuegoIniciado(hud));
        return Resultado.ok(hud);
    }
}
