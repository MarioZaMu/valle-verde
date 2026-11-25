package edu.ucc.farmsim.aplicacion.controlador;

import edu.ucc.farmsim.aplicacion.dto.VistaAccionEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificios;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaInventario;
import edu.ucc.farmsim.aplicacion.dto.VistaLogro;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaMercado;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado;
import edu.ucc.farmsim.aplicacion.dto.VistaTutorial;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.MapeadorVista;
import edu.ucc.farmsim.aplicacion.servicio.ServicioAnimal;
import edu.ucc.farmsim.aplicacion.servicio.ServicioAvatar;
import edu.ucc.farmsim.aplicacion.servicio.ServicioDia;
import edu.ucc.farmsim.aplicacion.servicio.ServicioEdificio;
import edu.ucc.farmsim.aplicacion.servicio.ServicioGranja;
import edu.ucc.farmsim.aplicacion.servicio.ServicioJuegoGuardado;
import edu.ucc.farmsim.aplicacion.servicio.ServicioLogro;
import edu.ucc.farmsim.aplicacion.servicio.ServicioMercado;
import edu.ucc.farmsim.aplicacion.servicio.ServicioNpc;
import edu.ucc.farmsim.aplicacion.servicio.ServicioProgreso;
import edu.ucc.farmsim.aplicacion.servicio.ServicioTutorial;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.dominio.progreso.PasoTutorial;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 * Pattern: Controller — Role: GRASP controller (the single entry point the UI calls).
 *
 * <p>Solo orquesta: pide a los servicios que hagan el trabajo y siempre responde con un {@link Resultado}. Ninguna
 * excepción llega nunca a la UI ni a la API: todo imprevisto se convierte en {@code ERROR_INTERNO} (R-03, RNF-07), y
 * nunca toca los repositorios (AR-4). No es el controlador REST: el paquete {@code api} traduce HTTP en llamadas a esta
 * clase. Tras un paso, siembra, riego, conversación o final de día aceptados, avisa al tutorial de inicio, que avanza
 * si esa era la acción de su paso (RN-39).
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-26, HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-35, HU-37, HU-38, HU-45, HU-46, HU-47, HU-49.
 */
@Component
public final class ControladorJuego implements ComandosJuego {

    private static final Logger BITACORA = Logger.getLogger(ControladorJuego.class.getName());

    private final SesionJuego sesion;
    private final MapeadorVista mapeador;
    private final ServicioProgreso progreso;
    private final ServicioDia dia;
    private final ServicioJuegoGuardado guardados;
    private final ServicioGranja granja;
    private final ServicioMercado mercado;
    private final ServicioEdificio edificios;
    private final ServicioAnimal animales;
    private final ServicioLogro logros;
    private final ServicioAvatar avatar;
    private final ServicioNpc npcs;
    private final ServicioTutorial tutorial;

    /**
     * Crea el controlador.
     *
     * @param sesion  la partida en curso
     * @param mapeador   construye las vistas que dibuja la UI
     * @param progreso inicia partidas
     * @param dia      termina los días
     * @param guardados    conoce las ranuras de guardado
     * @param granja     casos de uso de cultivos
     * @param mercado   compra, venta y las vistas del mercado y el inventario
     * @param edificios construcción
     * @param animales  cuidado de animales
     * @param logros los logros y su progreso
     * @param avatar   caminar con el avatar
     * @param npcs     las personas del valle y sus encargos
     * @param tutorial el tutorial de inicio
     */
    public ControladorJuego(SesionJuego sesion, MapeadorVista mapeador, ServicioProgreso progreso, ServicioDia dia,
                          ServicioJuegoGuardado guardados, ServicioGranja granja, ServicioMercado mercado,
                          ServicioEdificio edificios, ServicioAnimal animales,
                          ServicioLogro logros, ServicioAvatar avatar,
                          ServicioNpc npcs, ServicioTutorial tutorial) {
        this.sesion = sesion;
        this.mapeador = mapeador;
        this.progreso = progreso;
        this.dia = dia;
        this.guardados = guardados;
        this.granja = granja;
        this.mercado = mercado;
        this.edificios = edificios;
        this.animales = animales;
        this.logros = logros;
        this.avatar = avatar;
        this.npcs = npcs;
        this.tutorial = tutorial;
    }

    /** Verdadero si hay una partida en curso (la UI pide confirmación antes de descartarla). */
    @Override
    public boolean tieneJuegoEnProgreso() {
        try {
            return sesion.tieneJuego();
        } catch (RuntimeException e) {
            BITACORA.log(Level.SEVERE, "hasGameInProgress failed", e);
            return false;
        }
    }

    /**
     * Inicia una partida nueva con el estado inicial de RN-30, reemplazando cualquier partida en curso.
     *
     * @param semilla semilla del generador aleatorio
     */
    @Override
    public Resultado<VistaHud> juegoNuevo(long semilla) {
        return guardia(() -> progreso.juegoNuevo(semilla));
    }

    /** Termina el día: los efectos se resuelven en el orden de RN-03 y se devuelve la nueva barra superior. */
    @Override
    public Resultado<VistaHud> diaFin() {
        return guardia(() -> rastreado(PasoTutorial.END_DAY, dia.diaFin()));
    }

    /**
     * Siembra una semilla del inventario en una parcela (RN-06).
     *
     * @param col    columna de la parcela
     * @param fila    fila de la parcela
     * @param idSemilla id del objeto semilla, por ejemplo {@code CARROT_SEED}
     */
    @Override
    public Resultado<VistaParcela> plantar(int col, int fila, String idSemilla) {
        return guardia(() -> rastreado(PasoTutorial.SOW, granja.plantar(col, fila, idSemilla)));
    }

    /**
     * Riega el cultivo de una parcela (RN-07).
     *
     * @param col columna de la parcela
     * @param fila fila de la parcela
     */
    @Override
    public Resultado<VistaParcela> agua(int col, int fila) {
        return guardia(() -> rastreado(PasoTutorial.WATER, granja.agua(col, fila)));
    }

    /**
     * La herramienta de cosechar (RN-10, RN-34): cosecha un cultivo maduro o limpia uno marchito.
     *
     * @param col columna de la parcela
     * @param fila fila de la parcela
     */
    @Override
    public Resultado<VistaParcela> cosecha(int col, int fila) {
        return guardia(() -> granja.cosecha(col, fila));
    }

    /**
     * Describe una parcela para el tooltip (HU-07). Una consulta pura: nunca cambia el juego.
     *
     * @param col columna de la parcela
     * @param fila fila de la parcela
     */
    @Override
    public Resultado<VistaParcela> inspeccionar(int col, int fila) {
        return guardia(() -> granja.inspeccionar(col, fila));
    }

    /** Todo el mapa de la granja, para dibujarlo. Una consulta pura. */
    @Override
    public Resultado<VistaMapa> mapa() {
        return guardia(granja::mapa);
    }

    @Override
    public Resultado<VistaAvatar> mover(String direccion) {
        return guardia(() -> rastreado(PasoTutorial.WALK, avatar.mover(direccion)));
    }

    @Override
    public Resultado<List<VistaNpc>> npcs() {
        return guardia(npcs::npcs);
    }

    @Override
    public Resultado<VistaConversacion> hablar(String npcId) {
        return guardia(() -> rastreado(PasoTutorial.TALK, npcs.hablar(npcId)));
    }

    @Override
    public Resultado<VistaConversacion> entregar(String npcId) {
        return guardia(() -> npcs.entregar(npcId));
    }

    @Override
    public Resultado<VistaTutorial> tutorial() {
        return guardia(tutorial::tutorial);
    }

    @Override
    public Resultado<VistaTutorial> tutorialSaltar() {
        return guardia(tutorial::saltar);
    }

    /** Verdadero si al menos una de las ranuras de guardado contiene una partida. */
    @Override
    public Resultado<Boolean> tieneJuegosArchivados() {
        return guardia(guardados::tieneGuardadoCualquier);
    }

    /** La barra superior de la partida en curso. */
    @Override
    public Resultado<VistaHud> hud() {
        return guardia(() -> {
            if (!sesion.tieneJuego()) {
                return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
            }
            return Resultado.ok(mapeador.hud(sesion.actual()));
        });
    }

    @Override
    public Resultado<Integer> comprar(String idArticulo, int cantidad) {
        return guardia(() -> mercado.comprar(idArticulo, cantidad));
    }

    @Override
    public Resultado<Integer> vender(String idArticulo, int cantidad) {
        return guardia(() -> mercado.vender(idArticulo, cantidad));
    }

    @Override
    public Resultado<Integer> cotizar(String idArticulo, int cantidad) {
        return guardia(() -> mercado.cotizar(idArticulo, cantidad));
    }

    @Override
    public Resultado<VistaMercado> mercado() {
        return guardia(mercado::mercado);
    }

    @Override
    public Resultado<VistaInventario> inventario() {
        return guardia(mercado::inventario);
    }

    @Override
    public Resultado<VistaEdificios> edificios() {
        return guardia(edificios::edificios);
    }

    @Override
    public Resultado<VistaEdificio> construir(String idEdificio, int col, int fila) {
        return guardia(() -> edificios.construir(idEdificio, col, fila));
    }

    @Override
    public Resultado<VistaEdificio> animalComprar(String animalId) {
        return guardia(() -> animales.animalComprar(animalId));
    }

    @Override
    public Resultado<VistaAccionEdificio> alimentar(int idEdificio) {
        return guardia(() -> animales.alimentar(idEdificio));
    }

    @Override
    public Resultado<VistaAccionEdificio> collect(int idEdificio) {
        return guardia(() -> animales.collect(idEdificio));
    }

    @Override
    public Resultado<VistaAccionEdificio> curar(int idEdificio, int indiceAnimal) {
        return guardia(() -> animales.curar(idEdificio, indiceAnimal));
    }

    @Override
    public Resultado<List<VistaLogro>> logros() {
        return guardia(logros::logros);
    }

    @Override
    public Resultado<List<VistaRanuraGuardado>> guardarRanuras() {
        return guardia(guardados::ranuras);
    }

    @Override
    public Resultado<VistaRanuraGuardado> guardar(int ranura) {
        return guardia(() -> guardados.guardar(ranura));
    }

    @Override
    public Resultado<VistaHud> cargar(int ranura) {
        return guardia(() -> guardados.cargar(ranura));
    }

    /** Una acción aceptada de un paso avisa al tutorial, después de los eventos de la propia acción. */
    private <T> Resultado<T> rastreado(PasoTutorial paso, Resultado<T> resultado) {
        if (resultado.isOk()) {
            tutorial.hecho(paso);
        }
        return resultado;
    }

    private <T> Resultado<T> guardia(Supplier<Resultado<T>> operacion) {
        try {
            return operacion.get();
        } catch (RuntimeException e) {
            BITACORA.log(Level.SEVERE, "Unexpected error", e);
            return Resultado.fallarCon(CodigoError.ERROR_INTERNO);
        }
    }
}
