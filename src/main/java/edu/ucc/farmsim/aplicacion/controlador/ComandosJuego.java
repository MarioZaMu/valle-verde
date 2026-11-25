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
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import java.util.List;

/**
 * Todo lo que la interfaz del jugador le puede pedir al juego. Es lo único que la UI conoce del juego (AR-3): {@link
 * ControladorJuego} responde en el mismo proceso y {@code ClienteJuegoRemoto} envía cada orden al servidor REST, así que
 * las pantallas funcionan igual con ambos y nunca saben si hay red.
 *
 * <p>Toda operación responde con un {@link Resultado}; ninguna lanza excepciones (R-03, RNF-07).
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-26, HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-35, HU-37, HU-38, HU-45, HU-46, HU-47, HU-49.
 */
public interface ComandosJuego {

    /** Verdadero si hay una partida en curso (la UI pide confirmación antes de descartarla). */
    boolean tieneJuegoEnProgreso();

    /**
     * Inicia una partida nueva con el estado inicial de RN-30, reemplazando cualquier partida en curso.
     *
     * @param semilla semilla del generador aleatorio
     */
    Resultado<VistaHud> juegoNuevo(long semilla);

    /** La barra superior de la partida en curso. */
    Resultado<VistaHud> hud();

    /** Termina el día: los efectos se resuelven en el orden de RN-03 y se devuelve la nueva barra superior. */
    Resultado<VistaHud> diaFin();

    /**
     * Siembra una semilla del inventario en una parcela (RN-06).
     *
     * @param col    columna de la parcela
     * @param fila    fila de la parcela
     * @param idSemilla id del objeto semilla, por ejemplo {@code CARROT_SEED}
     */
    Resultado<VistaParcela> plantar(int col, int fila, String idSemilla);

    /**
     * Riega el cultivo de una parcela (RN-07).
     *
     * @param col columna de la parcela
     * @param fila fila de la parcela
     */
    Resultado<VistaParcela> agua(int col, int fila);

    /**
     * La herramienta de cosechar (RN-10, RN-34): cosecha un cultivo maduro o limpia uno marchito.
     *
     * @param col columna de la parcela
     * @param fila fila de la parcela
     */
    Resultado<VistaParcela> cosecha(int col, int fila);

    /**
     * Describe una parcela para el tooltip (HU-07). Una consulta pura: nunca cambia el juego.
     *
     * @param col columna de la parcela
     * @param fila fila de la parcela
     */
    Resultado<VistaParcela> inspeccionar(int col, int fila);

    /** Todo el mapa de la granja, para dibujarlo. Una consulta pura. */
    Resultado<VistaMapa> map();

    /**
     * Un paso del avatar (RN-36). No cuesta energía ni mueve el calendario; si no se puede caminar sobre la parcela de
     * ese lado, se rechaza con {@code MOVIMIENTO_BLOQUEADO} y el avatar solo gira.
     *
     * @param direccion {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
     * @return dónde está el avatar ahora
     */
    Resultado<VistaAvatar> mover(String direccion);

    /** Las personas del valle, dónde están y cómo van sus encargos (RN-37). Una consulta pura. */
    Resultado<List<VistaNpc>> npcs();

    /**
     * Lo que dice una persona del valle (RN-37). El avatar debe estar en una de las cuatro parcelas junto a ella, o se
     * rechaza con {@code NPC_LEJOS}. No cuesta nada y no cambia nada.
     *
     * @param npcId {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
     */
    Resultado<VistaConversacion> hablar(String npcId);

    /**
     * Entrega el encargo de una persona junto al avatar (RN-38): se toman los objetos y la recompensa se paga una sola
     * vez. {@code ENCARGO_INCOMPLETO} si al inventario le faltan unidades; un encargo hecho solo recibe un
     * agradecimiento.
     *
     * @param npcId {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
     * @return lo que dice la persona después
     */
    Resultado<VistaConversacion> entregar(String npcId);

    /** El tutorial de inicio de la partida en curso: su paso y lo que dice el avatar (RN-39). Una consulta pura. */
    Resultado<VistaTutorial> tutorial();

    /** Termina el tutorial de inicio de inmediato; no vuelve (RN-39). */
    Resultado<VistaTutorial> tutorialSaltar();

    /** Verdadero si al menos una de las ranuras de guardado contiene una partida. */
    Resultado<Boolean> tieneJuegosArchivados();

    /**
     * Compra unidades de una semilla, heno o medicina al precio fijo (RN-16).
     *
     * @param idArticulo   id del objeto, por ejemplo {@code CORN_SEED}
     * @param cantidad 1 to 99
     * @return el total pagado en m
     */
    Resultado<Integer> comprar(String idArticulo, int cantidad);

    /**
     * Vende unidades de un cultivo, huevo o leche al precio dinámico (RN-17, RN-18).
     *
     * @param idArticulo   id del objeto, por ejemplo {@code CARROT}
     * @param cantidad de 1 hasta las unidades que se tienen
     * @return el total recibido en m
     */
    Resultado<Integer> vender(String idArticulo, int cantidad);

    /** Lo que pagaría ahora vender la cantidad. Una consulta pura. */
    Resultado<Integer> cotizar(String idArticulo, int cantidad);

    /** Precios, tendencias y el catálogo de compra. Una consulta pura. */
    Resultado<VistaMercado> mercado();

    /** El inventario como una grilla. Una consulta pura. */
    Resultado<VistaInventario> inventario();

    /** Los edificios de la granja y el catálogo de lo que se puede construir. Una consulta pura. */
    Resultado<VistaEdificios> edificios();

    /**
     * Construye un edificio con su parcela superior izquierda en el lugar dado (RN-20, RN-21).
     *
     * @param idEdificio id del edificio, por ejemplo {@code COOP}
     * @param col        columna de la parcela superior izquierda
     * @param fila        fila de la parcela superior izquierda
     */
    Resultado<VistaEdificio> build(String idEdificio, int col, int fila);

    /**
     * Compra un animal y lo aloja en el primer edificio compatible con espacio (RN-23).
     *
     * @param animalId id del animal, por ejemplo {@code HEN}
     * @return el edificio donde vive ahora
     */
    Resultado<VistaEdificio> animalComprar(String animalId);

    /**
     * Alimenta a los animales de un edificio que pueden comer y no han comido hoy (RN-24).
     *
     * @param idEdificio id del edificio
     */
    Resultado<VistaAccionEdificio> alimentar(int idEdificio);

    /**
     * Recoge los productos que esperan en un edificio (RN-26).
     *
     * @param idEdificio id del edificio
     */
    Resultado<VistaAccionEdificio> collect(int idEdificio);

    /**
     * Cura a un animal enfermo con medicina (RN-27).
     *
     * @param idEdificio  id del edificio
     * @param indiceAnimal posición del animal dentro del edificio
     */
    Resultado<VistaAccionEdificio> curar(int idEdificio, int indiceAnimal);

    /** Todos los logros con el progreso del jugador. Una consulta pura. */
    Resultado<List<VistaLogro>> logros();

    /** Lo que guarda cada ranura de partida. Una consulta pura. */
    Resultado<List<VistaRanuraGuardado>> guardarRanuras();

    /**
     * Guarda la partida en curso en una ranura, reemplazando lo que tenía (RN-31).
     *
     * @param ranura número de ranura, de 1 a 3
     * @return la ranura tal como queda ahora
     */
    Resultado<VistaRanuraGuardado> save(int ranura);

    /**
     * Carga una ranura y la convierte en la partida en curso; si la ranura se rechaza, la partida no cambia (RN-33).
     *
     * @param ranura número de ranura, de 1 a 3
     */
    Resultado<VistaHud> cargar(int ranura);
}
