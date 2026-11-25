package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.granja.Direccion;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import org.springframework.stereotype.Service;

/**
 * Caminar con el avatar (RN-36). Un paso no cuesta energía ni mueve el calendario. Cuando el camino está bloqueado el
 * paso se rechaza, pero el avatar igual gira para mirar hacia allí, como pide la regla.
 *
 * <p>Stories: HU-45.
 */
@Service
public final class ServicioAvatar {

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
    public ServicioAvatar(SesionJuego sesion, MapeadorVista mapeador, BusEvento eventos) {
        this.sesion = sesion;
        this.mapeador = mapeador;
        this.eventos = eventos;
    }

    /**
     * Un paso del avatar.
     *
     * @param idDireccion {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
     * @return dónde está ahora; {@code MOVIMIENTO_BLOQUEADO} si no se puede caminar sobre la parcela de ese lado
     */
    public Resultado<VistaAvatar> mover(String idDireccion) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        Direccion direccion = Direccion.valueOf(idDireccion);
        EstadoJuego estado = sesion.actual();
        if (!estado.avatarPaso(direccion)) {
            return Resultado.fallarCon(CodigoError.MOVIMIENTO_BLOQUEADO);
        }
        VistaAvatar avatar = mapeador.avatar(estado);
        eventos.publish(new EventoJuego.AvatarMovido(avatar));
        return Resultado.ok(avatar);
    }
}
