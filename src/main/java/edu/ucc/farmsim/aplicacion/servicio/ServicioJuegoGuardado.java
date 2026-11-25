package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.guardado.ExcepcionAlmacenamientoGuardado;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado;
import edu.ucc.farmsim.dominio.guardado.InfoRanura;
import edu.ucc.farmsim.dominio.guardado.RepositorioGuardado;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.juego.ValidadorEstadoJuego;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Pattern: Memento — Role: Caretaker (keeps the snapshots of the game; it never looks inside them).
 *
 * <p>Guarda, carga y lista las ranuras de guardado (RN-31 a RN-33). Una carga construye la partida nueva aparte y solo
 * reemplaza la partida en curso cuando pasaron todas las comprobaciones, de modo que una ranura rechazada no cambia
 * nada. Cuando el almacenamiento no responde informa {@code ERROR_ES}: jugar nunca depende de él.
 *
 * <p>Stories: HU-27, HU-29, HU-30, HU-31, HU-32, HU-33.
 */
@Service
public final class ServicioJuegoGuardado {

    private static final Logger BITACORA = Logger.getLogger(ServicioJuegoGuardado.class.getName());
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RepositorioGuardado repositorio;
    private final SesionJuego sesion;
    private final MapeadorVista mapeador;
    private final BusEvento eventos;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param repositorio donde se guardan las partidas
     * @param sesion    la partida en curso
     * @param mapeador     construye las vistas que dibuja la UI
     * @param eventos     donde se publican los eventos
     * @param reloj      dice cuándo se guarda una partida
     */
    public ServicioJuegoGuardado(RepositorioGuardado repositorio, SesionJuego sesion, MapeadorVista mapeador, BusEvento eventos,
                           Clock reloj) {
        this.repositorio = repositorio;
        this.sesion = sesion;
        this.mapeador = mapeador;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /**
     * Éxito con true si al menos una ranura contiene una partida guardada; {@code ERROR_ES} si falla el almacenamiento.
     */
    public Resultado<Boolean> tieneGuardadoCualquier() {
        try {
            for (int ranura = 1; ranura <= ReglasJuego.RANURAS_GUARDADO; ranura++) {
                if (repositorio.existeRanura(ranura)) {
                    return Resultado.ok(true);
                }
            }
            return Resultado.ok(false);
        } catch (ExcepcionAlmacenamientoGuardado e) {
            return Resultado.fallarCon(CodigoError.ERROR_ES, e.getMessage());
        }
    }

    /** Cada ranura con lo que contiene. Una ranura dañada se lista como tal y no detiene al resto. */
    public Resultado<List<VistaRanuraGuardado>> ranuras() {
        try {
            List<VistaRanuraGuardado> vistas = new ArrayList<>();
            for (InfoRanura info : repositorio.ranuras()) {
                vistas.add(vista(info));
            }
            return Resultado.ok(List.copyOf(vistas));
        } catch (ExcepcionAlmacenamientoGuardado e) {
            return Resultado.fallarCon(CodigoError.ERROR_ES, e.getMessage());
        }
    }

    /**
     * Guarda la partida en curso en una ranura, reemplazando lo que tenía (RN-31). Si falla el almacenamiento queda el
     * contenido anterior y el juego continúa.
     *
     * @param ranura número de ranura, de 1 a 3
     */
    public Resultado<VistaRanuraGuardado> save(int ranura) {
        ranuraExigir(ranura);
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        Instant now = reloj.instant();
        try {
            repositorio.save(ranura, estado.mementoCrear(), now);
        } catch (ExcepcionAlmacenamientoGuardado e) {
            return Resultado.fallarCon(CodigoError.ERROR_ES, e.getMessage());
        }
        return Resultado.ok(vista(InfoRanura.ok(ranura, estado.fecha(), estado.dinero(), now)));
    }

    /**
     * Carga una ranura y la convierte en la partida en curso. Las comprobaciones de RN-33 corren en orden y se detienen
     * en el primer fallo; la partida en curso se reemplaza solo cuando todas pasan.
     *
     * @param ranura número de ranura, de 1 a 3
     */
    public Resultado<VistaHud> cargar(int ranura) {
        ranuraExigir(ranura);
        MementoJuego memento;
        try {
            memento = repositorio.cargar(ranura);
        } catch (ExcepcionAlmacenamientoGuardado e) {
            return Resultado.fallarCon(CodigoError.ERROR_ES, e.getMessage());
        } catch (ExcepcionCargarGuardado e) {
            BITACORA.log(Level.WARNING, "Slot " + ranura + " rejected: " + e.getMessage());
            return switch (e.motivo()) {
                case MISSING -> Resultado.fallarCon(CodigoError.ARCHIVO_INEXISTENTE, ranura);
                case UNSUPPORTED_VERSION -> Resultado.fallarCon(CodigoError.VERSION_NO_SOPORTADA);
                case CORRUPT -> Resultado.fallarCon(CodigoError.ARCHIVO_CORRUPTO);
            };
        }
        List<String> violaciones = ValidadorEstadoJuego.violaciones(memento);
        if (!violaciones.isEmpty()) {
            BITACORA.log(Level.WARNING, "Slot " + ranura + " breaks the invariants: " + violaciones);
            return Resultado.fallarCon(CodigoError.ARCHIVO_CORRUPTO);
        }
        MementoJuego antes = sesion.tieneJuego() ? sesion.actual().mementoCrear() : null;
        EstadoJuego cargado;
        try {
            cargado = EstadoJuego.desdeMemento(memento);
        } catch (RuntimeException e) {
            BITACORA.log(Level.WARNING, "Slot " + ranura + " could not be built", e);
            if (antes != null) {
                sesion.actual().restaurar(antes);
            }
            return Resultado.fallarCon(CodigoError.ARCHIVO_CORRUPTO);
        }
        sesion.replace(cargado);
        VistaHud hud = mapeador.hud(cargado);
        eventos.publish(new EventoJuego.JuegoIniciado(hud));
        eventos.publish(new EventoJuego.MapaCambiado());
        eventos.publish(new EventoJuego.InventarioCambiado());
        return Resultado.ok(hud);
    }

    private VistaRanuraGuardado vista(InfoRanura info) {
        if (info.status() != InfoRanura.Situacion.OK) {
            return new VistaRanuraGuardado(info.ranura(), info.status().name(), 0, null, 0, 0, null);
        }
        String sello = SELLO.format(LocalDateTime.ofInstant(info.archivadoEn(), reloj.getZone()));
        return new VistaRanuraGuardado(info.ranura(), info.status().name(), info.fecha().anio(), info.fecha().estacion().name(),
                info.fecha().dia(), info.dinero(), sello);
    }

    private static void ranuraExigir(int ranura) {
        if (ranura < 1 || ranura > ReglasJuego.RANURAS_GUARDADO) {
            throw new IllegalArgumentException("slot must be between 1 and " + ReglasJuego.RANURAS_GUARDADO + ": " + ranura);
        }
    }
}
