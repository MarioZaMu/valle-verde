package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.juego.ReporteDia;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Termina el día: ejecuta el pipeline de RN-03 y publica los eventos (paso 9).
 *
 * <p>Antes de ejecutar los pasos toma una instantánea; si algún paso falla se restaura la instantánea, de modo que un
 * final de día fallido deja la partida exactamente como estaba y no publica nada (HU-10 · CA-4).
 *
 * <p>Stories: HU-10, HU-17, HU-26.
 */
@Service
public final class ServicioDia {

    private static final Logger BITACORA = Logger.getLogger(ServicioDia.class.getName());

    private final SesionJuego sesion;
    private final SecuenciaDia secuencia;
    private final MapeadorVista mapeador;
    private final BusEvento eventos;
    private final ServicioLogro logros;

    /**
     * Crea el servicio.
     *
     * @param sesion  la partida en curso
     * @param secuencia los pasos del final del día
     * @param mapeador   construye las vistas que dibuja la UI
     * @param eventos   donde se publican los eventos
     * @param logros anuncia los logros desbloqueados al final del día
     */
    public ServicioDia(SesionJuego sesion, SecuenciaDia secuencia, MapeadorVista mapeador, BusEvento eventos,
                      ServicioLogro logros) {
        this.sesion = sesion;
        this.secuencia = secuencia;
        this.mapeador = mapeador;
        this.eventos = eventos;
        this.logros = logros;
    }

    /** Termina el día de la partida en curso y devuelve la nueva barra superior. */
    public Resultado<VistaHud> diaFin() {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        MementoJuego antes = estado.mementoCrear();
        ReporteDia reporte;
        try {
            reporte = secuencia.run(estado);
        } catch (RuntimeException e) {
            estado.restaurar(antes);
            BITACORA.log(Level.SEVERE, "The end of the day failed; the state was restored", e);
            return Resultado.fallarCon(CodigoError.ERROR_INTERNO);
        }
        VistaHud hud = mapeador.hud(estado);
        eventos.publish(new EventoJuego.DiaTerminado(hud));
        logros.anunciar(estado, reporte.desbloqueado());
        eventos.publish(new EventoJuego.ResumenDiaListo(mapeador.resumen(estado, reporte)));
        return Resultado.ok(hud);
    }
}
