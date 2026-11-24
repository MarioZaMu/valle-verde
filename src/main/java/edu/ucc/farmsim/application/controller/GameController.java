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
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.dto.TutorialView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.AchievementService;
import edu.ucc.farmsim.application.service.AnimalService;
import edu.ucc.farmsim.application.service.AvatarService;
import edu.ucc.farmsim.application.service.BuildingService;
import edu.ucc.farmsim.application.service.DayService;
import edu.ucc.farmsim.application.service.FarmService;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.application.service.MarketService;
import edu.ucc.farmsim.application.service.NpcService;
import edu.ucc.farmsim.application.service.ProgressService;
import edu.ucc.farmsim.application.service.SaveGameService;
import edu.ucc.farmsim.application.service.TutorialService;
import edu.ucc.farmsim.application.service.ViewMapper;
import edu.ucc.farmsim.domain.progress.TutorialStep;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 * Pattern: Controller — Role: GRASP controller (the single entry point the UI calls).
 *
 * <p>Solo orquesta: pide a los servicios que hagan el trabajo y siempre responde con un {@link Result}. Ninguna
 * excepción llega nunca a la UI ni a la API: todo imprevisto se convierte en {@code ERROR_INTERNO} (R-03, RNF-07), y
 * nunca toca los repositorios (AR-4). No es el controlador REST: el paquete {@code api} traduce HTTP en llamadas a esta
 * clase. Tras un paso, siembra, riego, conversación o final de día aceptados, avisa al tutorial de inicio, que avanza
 * si esa era la acción de su paso (RN-39).
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-26, HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-35, HU-37, HU-38, HU-45, HU-46, HU-47, HU-49.
 */
@Component
public final class GameController implements GameCommands {

    private static final Logger LOG = Logger.getLogger(GameController.class.getName());

    private final GameSession session;
    private final ViewMapper mapper;
    private final ProgressService progress;
    private final DayService day;
    private final SaveGameService saves;
    private final FarmService farm;
    private final MarketService market;
    private final BuildingService buildings;
    private final AnimalService animals;
    private final AchievementService achievements;
    private final AvatarService avatar;
    private final NpcService npcs;
    private final TutorialService tutorial;

    /**
     * Crea el controlador.
     *
     * @param session  la partida en curso
     * @param mapper   construye las vistas que dibuja la UI
     * @param progress inicia partidas
     * @param day      termina los días
     * @param saves    conoce las ranuras de guardado
     * @param farm     casos de uso de cultivos
     * @param market   compra, venta y las vistas del mercado y el inventario
     * @param buildings construcción
     * @param animals  cuidado de animales
     * @param achievements los logros y su progreso
     * @param avatar   caminar con el avatar
     * @param npcs     las personas del valle y sus encargos
     * @param tutorial el tutorial de inicio
     */
    public GameController(GameSession session, ViewMapper mapper, ProgressService progress, DayService day,
                          SaveGameService saves, FarmService farm, MarketService market,
                          BuildingService buildings, AnimalService animals,
                          AchievementService achievements, AvatarService avatar,
                          NpcService npcs, TutorialService tutorial) {
        this.session = session;
        this.mapper = mapper;
        this.progress = progress;
        this.day = day;
        this.saves = saves;
        this.farm = farm;
        this.market = market;
        this.buildings = buildings;
        this.animals = animals;
        this.achievements = achievements;
        this.avatar = avatar;
        this.npcs = npcs;
        this.tutorial = tutorial;
    }

    /** Verdadero si hay una partida en curso (la UI pide confirmación antes de descartarla). */
    @Override
    public boolean hasGameInProgress() {
        try {
            return session.hasGame();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "hasGameInProgress failed", e);
            return false;
        }
    }

    /**
     * Inicia una partida nueva con el estado inicial de RN-30, reemplazando cualquier partida en curso.
     *
     * @param seed semilla del generador aleatorio
     */
    @Override
    public Result<HudView> newGame(long seed) {
        return guard(() -> progress.newGame(seed));
    }

    /** Termina el día: los efectos se resuelven en el orden de RN-03 y se devuelve la nueva barra superior. */
    @Override
    public Result<HudView> endDay() {
        return guard(() -> tracked(TutorialStep.END_DAY, day.endDay()));
    }

    /**
     * Siembra una semilla del inventario en una parcela (RN-06).
     *
     * @param col    columna de la parcela
     * @param row    fila de la parcela
     * @param seedId id del objeto semilla, por ejemplo {@code CARROT_SEED}
     */
    @Override
    public Result<PlotView> plant(int col, int row, String seedId) {
        return guard(() -> tracked(TutorialStep.SOW, farm.plant(col, row, seedId)));
    }

    /**
     * Riega el cultivo de una parcela (RN-07).
     *
     * @param col columna de la parcela
     * @param row fila de la parcela
     */
    @Override
    public Result<PlotView> water(int col, int row) {
        return guard(() -> tracked(TutorialStep.WATER, farm.water(col, row)));
    }

    /**
     * La herramienta de cosechar (RN-10, RN-34): cosecha un cultivo maduro o limpia uno marchito.
     *
     * @param col columna de la parcela
     * @param row fila de la parcela
     */
    @Override
    public Result<PlotView> harvest(int col, int row) {
        return guard(() -> farm.harvest(col, row));
    }

    /**
     * Describe una parcela para el tooltip (HU-07). Una consulta pura: nunca cambia el juego.
     *
     * @param col columna de la parcela
     * @param row fila de la parcela
     */
    @Override
    public Result<PlotView> inspect(int col, int row) {
        return guard(() -> farm.inspect(col, row));
    }

    /** Todo el mapa de la granja, para dibujarlo. Una consulta pura. */
    @Override
    public Result<MapView> map() {
        return guard(farm::map);
    }

    @Override
    public Result<AvatarView> move(String direction) {
        return guard(() -> tracked(TutorialStep.WALK, avatar.move(direction)));
    }

    @Override
    public Result<List<NpcView>> npcs() {
        return guard(npcs::npcs);
    }

    @Override
    public Result<DialogueView> talk(String npcId) {
        return guard(() -> tracked(TutorialStep.TALK, npcs.talk(npcId)));
    }

    @Override
    public Result<DialogueView> deliver(String npcId) {
        return guard(() -> npcs.deliver(npcId));
    }

    @Override
    public Result<TutorialView> tutorial() {
        return guard(tutorial::tutorial);
    }

    @Override
    public Result<TutorialView> skipTutorial() {
        return guard(tutorial::skip);
    }

    /** Verdadero si al menos una de las ranuras de guardado contiene una partida. */
    @Override
    public Result<Boolean> hasSavedGames() {
        return guard(saves::hasAnySave);
    }

    /** La barra superior de la partida en curso. */
    @Override
    public Result<HudView> hud() {
        return guard(() -> {
            if (!session.hasGame()) {
                return Result.failWith(ErrorCode.SIN_PARTIDA);
            }
            return Result.ok(mapper.hud(session.current()));
        });
    }

    @Override
    public Result<Integer> buy(String itemId, int quantity) {
        return guard(() -> market.buy(itemId, quantity));
    }

    @Override
    public Result<Integer> sell(String itemId, int quantity) {
        return guard(() -> market.sell(itemId, quantity));
    }

    @Override
    public Result<Integer> quote(String itemId, int quantity) {
        return guard(() -> market.quote(itemId, quantity));
    }

    @Override
    public Result<MarketView> market() {
        return guard(market::market);
    }

    @Override
    public Result<InventoryView> inventory() {
        return guard(market::inventory);
    }

    @Override
    public Result<BuildingsView> buildings() {
        return guard(buildings::buildings);
    }

    @Override
    public Result<BuildingView> build(String buildingId, int col, int row) {
        return guard(() -> buildings.build(buildingId, col, row));
    }

    @Override
    public Result<BuildingView> buyAnimal(String animalId) {
        return guard(() -> animals.buyAnimal(animalId));
    }

    @Override
    public Result<BuildingActionView> feed(int buildingId) {
        return guard(() -> animals.feed(buildingId));
    }

    @Override
    public Result<BuildingActionView> collect(int buildingId) {
        return guard(() -> animals.collect(buildingId));
    }

    @Override
    public Result<BuildingActionView> cure(int buildingId, int animalIndex) {
        return guard(() -> animals.cure(buildingId, animalIndex));
    }

    @Override
    public Result<List<AchievementView>> achievements() {
        return guard(achievements::achievements);
    }

    @Override
    public Result<List<SaveSlotView>> saveSlots() {
        return guard(saves::slots);
    }

    @Override
    public Result<SaveSlotView> save(int slot) {
        return guard(() -> saves.save(slot));
    }

    @Override
    public Result<HudView> load(int slot) {
        return guard(() -> saves.load(slot));
    }

    /** Una acción aceptada de un paso avisa al tutorial, después de los eventos de la propia acción. */
    private <T> Result<T> tracked(TutorialStep step, Result<T> result) {
        if (result.isOk()) {
            tutorial.done(step);
        }
        return result;
    }

    private <T> Result<T> guard(Supplier<Result<T>> operation) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error", e);
            return Result.failWith(ErrorCode.ERROR_INTERNO);
        }
    }
}
