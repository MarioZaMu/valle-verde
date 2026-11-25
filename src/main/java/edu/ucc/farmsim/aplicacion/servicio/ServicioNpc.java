package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.granja.Avatar;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.npc.Npc;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Hablar con las personas del valle y entregar sus encargos (RN-37, RN-38). Hablar necesita al avatar en una de las
 * cuatro parcelas junto a la persona, no cuesta energía y no cambia nada. Entregar toma los objetos, paga la recompensa
 * como ingreso del día y marca el encargo como hecho; nunca se paga dos veces.
 *
 * <p>Stories: HU-46, HU-47.
 */
@Service
public final class ServicioNpc {

    private final SesionJuego sesion;
    private final MapeadorVista mapeador;
    private final ServicioConversacion conversaciones;
    private final BusEvento eventos;
    private final ServicioLogro logros;

    /**
     * Crea el servicio.
     *
     * @param sesion      la partida en curso
     * @param mapeador       construye las vistas que dibuja la UI
     * @param conversaciones    escribe lo que dice cada persona
     * @param eventos       donde se publican los eventos
     * @param logros evalúa los logros después de una recompensa
     */
    public ServicioNpc(SesionJuego sesion, MapeadorVista mapeador, ServicioConversacion conversaciones, BusEvento eventos,
                      ServicioLogro logros) {
        this.sesion = sesion;
        this.mapeador = mapeador;
        this.conversaciones = conversaciones;
        this.eventos = eventos;
        this.logros = logros;
    }

    /** Las personas del valle, dónde están y cómo van sus encargos. Una consulta pura. */
    public Resultado<List<VistaNpc>> npcs() {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        return Resultado.ok(mapeador.npcs(sesion.actual()));
    }

    /**
     * Lo que dice una persona, si el avatar está a su lado. Una consulta pura.
     *
     * @param npcId {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
     */
    public Resultado<VistaConversacion> hablar(String npcId) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        Npc npc = Npc.valueOf(npcId);
        EstadoJuego estado = sesion.actual();
        if (!esCerca(npc, estado)) {
            return Resultado.fallarCon(CodigoError.NPC_LEJOS, Nombres.npc(npc.name()));
        }
        return Resultado.ok(conversaciones.conversacion(npc, estado));
    }

    /**
     * Entrega el encargo de una persona que está junto al avatar. Un encargo hecho se agradece y no se vuelve a pagar.
     *
     * @param npcId {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
     * @return lo que dice la persona ahora; {@code ENCARGO_INCOMPLETO} si al inventario le faltan unidades
     */
    public Resultado<VistaConversacion> entregar(String npcId) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        Npc npc = Npc.valueOf(npcId);
        EstadoJuego estado = sesion.actual();
        if (!esCerca(npc, estado)) {
            return Resultado.fallarCon(CodigoError.NPC_LEJOS, Nombres.npc(npc.name()));
        }
        if (estado.encargos().isDone(npc)) {
            return Resultado.ok(conversaciones.conversacion(npc, estado));
        }
        int faltante = npc.montoEncargo() - estado.inventario().count(npc.articuloEncargo());
        if (faltante > 0) {
            return Resultado.fallarCon(CodigoError.ENCARGO_INCOMPLETO, Nombres.conteoArticulo(npc.articuloEncargo().name(), faltante));
        }
        estado.inventario().remove(npc.articuloEncargo(), npc.montoEncargo());
        estado.ganar(npc.recompensaEncargo());
        estado.encargos().complete(npc);
        eventos.publish(new EventoJuego.InventarioCambiado());
        eventos.publish(new EventoJuego.DineroCambiado(estado.dinero()));
        eventos.publish(new EventoJuego.EncargoCompletado(npc.name(), Nombres.npc(npc.name()), npc.recompensaEncargo()));
        logros.comprobar(estado);
        return Resultado.ok(conversaciones.conversacion(npc, estado));
    }

    private static boolean esCerca(Npc npc, EstadoJuego estado) {
        Avatar avatar = estado.avatar();
        return npc.esSiguienteA(avatar.col(), avatar.fila());
    }
}
