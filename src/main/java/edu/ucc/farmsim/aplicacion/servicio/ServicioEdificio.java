package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificios;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.dominio.progreso.Logro;
import org.springframework.stereotype.Service;

/**
 * Los casos de uso de construcción (RN-20, RN-21): construir un edificio sobre pasto libre, y listar lo construido y lo
 * que se puede construir. Las reglas se comprueban en el orden de RN-21 y un rechazo nunca cambia la partida.
 *
 * <p>Stories: HU-18, HU-24, HU-26, HU-45, HU-46.
 */
@Service
public final class ServicioEdificio {

    private final SesionJuego sesion;
    private final MapeadorVista mapeador;
    private final BusEvento eventos;
    private final ServicioLogro logros;

    /**
     * Crea el servicio.
     *
     * @param sesion la partida en curso
     * @param mapeador  construye las vistas que dibuja la UI
     * @param eventos  donde se publican los eventos
     * @param logros evalúa los logros después de cada construcción
     */
    public ServicioEdificio(SesionJuego sesion, MapeadorVista mapeador, BusEvento eventos, ServicioLogro logros) {
        this.sesion = sesion;
        this.mapeador = mapeador;
        this.eventos = eventos;
        this.logros = logros;
    }

    /**
     * Construye un edificio con su parcela superior izquierda en el lugar dado (RN-21): el área debe ser pasto libre
     * dentro del mapa (el avatar y las personas del valle no cuentan como libres: un edificio nunca cae sobre ellos,
     * RN-36, RN-37), el edificio debe estar desbloqueado, bajo su límite y al alcance del dinero. La construcción es
     * instantánea y no cuesta energía.
     *
     * @param idTipo id del edificio, por ejemplo {@code COOP}
     * @param col    columna de la parcela superior izquierda
     * @param fila    fila de la parcela superior izquierda
     */
    public Resultado<VistaEdificio> construir(String idTipo, int col, int fila) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        TipoEdificio tipo = TipoEdificio.valueOf(idTipo);
        EstadoJuego estado = sesion.actual();
        if (!estado.granja().puedeColocar(tipo, col, fila)
                || estado.avatar().esDentro(col, fila, tipo.width(), tipo.height())
                || Npc.dentroCualquier(col, fila, tipo.width(), tipo.height())) {
            return Resultado.fallarCon(CodigoError.UBICACION_INVALIDA);
        }
        Logro requerido = tipo.requisito();
        if (requerido != null && !estado.progreso().has(requerido)) {
            return Resultado.fallarCon(CodigoError.EDIFICIO_BLOQUEADO, Nombres.logro(requerido.name()));
        }
        if (estado.granja().contarEdificios(tipo) >= tipo.conteoMaximo()) {
            return Resultado.fallarCon(CodigoError.LIMITE_EDIFICIOS, Nombres.edificioPlural(tipo.name()));
        }
        if (tipo.costo() > estado.dinero()) {
            return Resultado.fallarCon(CodigoError.SIN_FONDOS, tipo.costo() - estado.dinero());
        }
        estado.gastar(tipo.costo());
        Edificio edificio = estado.granja().colocar(tipo, col, fila);
        if (edificio.ranurasInventarioExtra() > 0) {
            estado.inventario().ranurasAgregar(edificio.ranurasInventarioExtra());
            eventos.publish(new EventoJuego.InventarioCambiado());
        }
        eventos.publish(new EventoJuego.DineroCambiado(estado.dinero()));
        eventos.publish(new EventoJuego.MapaCambiado());
        estado.progreso().construirRegistro();
        logros.comprobar(estado);
        return Resultado.ok(mapeador.edificio(edificio));
    }

    /** Los edificios de la granja y el catálogo de lo que se puede construir. Una consulta pura. */
    public Resultado<VistaEdificios> edificios() {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        return Resultado.ok(mapeador.edificios(sesion.actual()));
    }
}
