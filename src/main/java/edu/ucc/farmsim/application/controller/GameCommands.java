package edu.ucc.farmsim.application.controller;

import edu.ucc.farmsim.application.dto.AchievementView;
import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.InventoryView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.MarketView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.dto.TutorialView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.result.Result;
import java.util.List;

/**
 * Todo lo que la interfaz del jugador le puede pedir al juego. Es lo único que la UI conoce del juego (AR-3): {@link
 * GameController} responde en el mismo proceso y {@code RemoteGameClient} envía cada orden al servidor REST, así que
 * las pantallas funcionan igual con ambos y nunca saben si hay red.
 *
 * <p>Toda operación responde con un {@link Result}; ninguna lanza excepciones (R-03, RNF-07).
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-26, HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-35, HU-37, HU-38, HU-45, HU-46, HU-47, HU-49.
 */
public interface GameCommands {

    /** Verdadero si hay una partida en curso (la UI pide confirmación antes de descartarla). */
    boolean hasGameInProgress();

    /**
     * Inicia una partida nueva con el estado inicial de RN-30, reemplazando cualquier partida en curso.
     *
     * @param seed semilla del generador aleatorio
     */
    Result<HudView> newGame(long seed);

    /** La barra superior de la partida en curso. */
    Result<HudView> hud();

    /** Termina el día: los efectos se resuelven en el orden de RN-03 y se devuelve la nueva barra superior. */
    Result<HudView> endDay();

    /**
     * Siembra una semilla del inventario en una parcela (RN-06).
     *
     * @param col    columna de la parcela
     * @param row    fila de la parcela
     * @param seedId id del objeto semilla, por ejemplo {@code CARROT_SEED}
     */
    Result<PlotView> plant(int col, int row, String seedId);

    /**
     * Riega el cultivo de una parcela (RN-07).
     *
     * @param col columna de la parcela
     * @param row fila de la parcela
     */
    Result<PlotView> water(int col, int row);

    /**
     * La herramienta de cosechar (RN-10, RN-34): cosecha un cultivo maduro o limpia uno marchito.
     *
     * @param col columna de la parcela
     * @param row fila de la parcela
     */
    Result<PlotView> harvest(int col, int row);

    /**
     * Describe una parcela para el tooltip (HU-07). Una consulta pura: nunca cambia el juego.
     *
     * @param col columna de la parcela
     * @param row fila de la parcela
     */
    Result<PlotView> inspect(int col, int row);

    /** Todo el mapa de la granja, para dibujarlo. Una consulta pura. */
    Result<MapView> map();

    /**
     * Un paso del avatar (RN-36). No cuesta energía ni mueve el calendario; si no se puede caminar sobre la parcela de
     * ese lado, se rechaza con {@code MOVIMIENTO_BLOQUEADO} y el avatar solo gira.
     *
     * @param direction {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
     * @return dónde está el avatar ahora
     */
    Result<AvatarView> move(String direction);

    /** Las personas del valle, dónde están y cómo van sus encargos (RN-37). Una consulta pura. */
    Result<List<NpcView>> npcs();

    /**
     * Lo que dice una persona del valle (RN-37). El avatar debe estar en una de las cuatro parcelas junto a ella, o se
     * rechaza con {@code NPC_LEJOS}. No cuesta nada y no cambia nada.
     *
     * @param npcId {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
     */
    Result<DialogueView> talk(String npcId);

    /**
     * Entrega el encargo de una persona junto al avatar (RN-38): se toman los objetos y la recompensa se paga una sola
     * vez. {@code ENCARGO_INCOMPLETO} si al inventario le faltan unidades; un encargo hecho solo recibe un
     * agradecimiento.
     *
     * @param npcId {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
     * @return lo que dice la persona después
     */
    Result<DialogueView> deliver(String npcId);

    /** El tutorial de inicio de la partida en curso: su paso y lo que dice el avatar (RN-39). Una consulta pura. */
    Result<TutorialView> tutorial();

    /** Termina el tutorial de inicio de inmediato; no vuelve (RN-39). */
    Result<TutorialView> skipTutorial();

    /** Verdadero si al menos una de las ranuras de guardado contiene una partida. */
    Result<Boolean> hasSavedGames();

    /**
     * Compra unidades de una semilla, heno o medicina al precio fijo (RN-16).
     *
     * @param itemId   id del objeto, por ejemplo {@code CORN_SEED}
     * @param quantity 1 to 99
     * @return el total pagado en m
     */
    Result<Integer> buy(String itemId, int quantity);

    /**
     * Vende unidades de un cultivo, huevo o leche al precio dinámico (RN-17, RN-18).
     *
     * @param itemId   id del objeto, por ejemplo {@code CARROT}
     * @param quantity de 1 hasta las unidades que se tienen
     * @return el total recibido en m
     */
    Result<Integer> sell(String itemId, int quantity);

    /** Lo que pagaría ahora vender la cantidad. Una consulta pura. */
    Result<Integer> quote(String itemId, int quantity);

    /** Precios, tendencias y el catálogo de compra. Una consulta pura. */
    Result<MarketView> market();

    /** El inventario como una grilla. Una consulta pura. */
    Result<InventoryView> inventory();

    /** Los edificios de la granja y el catálogo de lo que se puede construir. Una consulta pura. */
    Result<BuildingsView> buildings();

    /**
     * Construye un edificio con su parcela superior izquierda en el lugar dado (RN-20, RN-21).
     *
     * @param buildingId id del edificio, por ejemplo {@code COOP}
     * @param col        columna de la parcela superior izquierda
     * @param row        fila de la parcela superior izquierda
     */
    Result<BuildingView> build(String buildingId, int col, int row);

    /**
     * Compra un animal y lo aloja en el primer edificio compatible con espacio (RN-23).
     *
     * @param animalId id del animal, por ejemplo {@code HEN}
     * @return el edificio donde vive ahora
     */
    Result<BuildingView> buyAnimal(String animalId);

    /**
     * Alimenta a los animales de un edificio que pueden comer y no han comido hoy (RN-24).
     *
     * @param buildingId id del edificio
     */
    Result<BuildingActionView> feed(int buildingId);

    /**
     * Recoge los productos que esperan en un edificio (RN-26).
     *
     * @param buildingId id del edificio
     */
    Result<BuildingActionView> collect(int buildingId);

    /**
     * Cura a un animal enfermo con medicina (RN-27).
     *
     * @param buildingId  id del edificio
     * @param animalIndex posición del animal dentro del edificio
     */
    Result<BuildingActionView> cure(int buildingId, int animalIndex);

    /** Todos los logros con el progreso del jugador. Una consulta pura. */
    Result<List<AchievementView>> achievements();

    /** Lo que guarda cada ranura de partida. Una consulta pura. */
    Result<List<SaveSlotView>> saveSlots();

    /**
     * Guarda la partida en curso en una ranura, reemplazando lo que tenía (RN-31).
     *
     * @param slot número de ranura, de 1 a 3
     * @return la ranura tal como queda ahora
     */
    Result<SaveSlotView> save(int slot);

    /**
     * Carga una ranura y la convierte en la partida en curso; si la ranura se rechaza, la partida no cambia (RN-33).
     *
     * @param slot número de ranura, de 1 a 3
     */
    Result<HudView> load(int slot);
}
