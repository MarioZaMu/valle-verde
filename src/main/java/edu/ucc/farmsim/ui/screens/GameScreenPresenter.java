package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.components.Shortcut;
import edu.ucc.farmsim.ui.components.ToastPresenter;
import edu.ucc.farmsim.ui.components.ToastQueue;
import edu.ucc.farmsim.ui.components.Tool;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.function.Consumer;

/**
 * Lógica de la pantalla de juego, sin Swing: qué pasa cuando el jugador hace clic en una parcela, pulsa un botón o usa
 * un atajo. Los mensajes van a un destino (la barra de estado en producción); una cadena vacía borra la barra.
 *
 * <p>Stories: HU-10, HU-17, HU-18, HU-20, HU-22, HU-26, HU-29, HU-30, HU-34, HU-36, HU-39, HU-45, HU-46.
 */
public final class GameScreenPresenter {

    private static final String CLEAR = "";

    private final GameCommands controller;
    private final ToolModel tools;
    private final Consumer<String> messages;
    private final GameDialogs dialogs;
    private final ToastPresenter toasts;
    private int lastSlot;
    private boolean blocked;

    /**
     * Crea el presentador.
     *
     * @param controller punto de entrada al juego
     * @param tools      la herramienta y la semilla seleccionadas
     * @param messages   donde se envían los mensajes para el jugador
     * @param dialogs    abre los diálogos
     * @param toasts     donde se muestran los errores y avisos como toasts (HU-39)
     */
    public GameScreenPresenter(GameCommands controller, ToolModel tools, Consumer<String> messages,
                               GameDialogs dialogs, ToastPresenter toasts) {
        this.controller = controller;
        this.tools = tools;
        this.messages = messages;
        this.dialogs = dialogs;
        this.toasts = toasts;
    }

    /**
     * Crea el presentador sin avisos: solo el destino de mensajes se entera del resultado de una orden.
     *
     * @param controller punto de entrada al juego
     * @param tools      la herramienta y la semilla seleccionadas
     * @param messages   donde se envían los mensajes para el jugador
     * @param dialogs    abre los diálogos
     */
    public GameScreenPresenter(GameCommands controller, ToolModel tools, Consumer<String> messages,
                               GameDialogs dialogs) {
        this(controller, tools, messages, dialogs, new ToastPresenter(new ToastQueue(() -> 0L)));
    }

    /** Envía un mensaje a la barra de estado; un rechazo también se vuelve un aviso de error (HU-39 · CA-2). */
    private void show(boolean ok, String onSuccess, String onFailure) {
        messages.accept(ok ? onSuccess : onFailure);
        if (!ok) {
            toasts.error(onFailure);
        } else if (!onSuccess.isEmpty()) {
            toasts.info(onSuccess);
        }
    }

    /**
     * «Terminar día»: la barra superior se actualiza por los eventos; solo aparece un mensaje si el día no pudo
     * terminar.
     */
    public void onEndDay() {
        Result<HudView> result = controller.endDay();
        show(result.isOk(), CLEAR, result.message());
    }

    /**
     * Un clic en una parcela: la herramienta seleccionada actúa sobre ella. Sin herramienta no pasa nada, salvo sobre
     * un edificio, cuyo panel se abre. Construir abre el diálogo de construcción; Alimentar y Cosechar actúan sobre el
     * edificio bajo el clic (HU-36 · CA-4).
     */
    public void onPlotClicked(int col, int row) {
        Result<MapView> map = controller.map();
        NpcView npc = map.isOk() ? map.value().npcAt(col, row) : null;
        if (npc != null) {
            onTalk(npc.id());
            return;
        }
        Tool tool = tools.current();
        if (tool == null) {
            BuildingView building = buildingAt(col, row);
            if (building != null) {
                dialogs.showBuilding(building.id());
            }
            return;
        }
        if (tool == Tool.BUILD) {
            dialogs.showConstruction(col, row);
            return;
        }
        if (tool == Tool.SOW && tools.selectedSeed() == null) {
            show(false, CLEAR, Texts.NO_SEED_SELECTED);
            return;
        }
        BuildingView building = tool == Tool.FEED || tool == Tool.HARVEST ? buildingAt(col, row) : null;
        if (tool == Tool.HARVEST && building != null && building.capacity() == 0) {
            building = null;
        }
        if (tool == Tool.FEED) {
            onFeed(building);
        } else if (tool == Tool.HARVEST && building != null) {
            onCollect(building);
        } else {
            Result<PlotView> result = switch (tool) {
                case SOW -> controller.plant(col, row, tools.selectedSeed());
                case WATER -> controller.water(col, row);
                default -> controller.harvest(col, row);
            };
            show(result.isOk(), CLEAR, result.message());
        }
    }

    /**
     * Una flecha o W A S D: un paso del avatar (RN-36). Un paso bloqueado muestra su mensaje; mantener la tecla contra
     * una pared muestra el aviso una vez, no en cada repetición.
     *
     * @param direction {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
     * @return {@code true} si el avatar se movió; {@code false} si solo giró (o no hay partida)
     */
    public boolean onMove(String direction) {
        Result<AvatarView> result = controller.move(direction);
        if (result.isOk()) {
            blocked = false;
            messages.accept(CLEAR);
            return true;
        }
        messages.accept(result.message());
        if (!blocked) {
            toasts.error(result.message());
        }
        blocked = true;
        return false;
    }

    /**
     * Espacio: la herramienta seleccionada actúa sobre la parcela que tiene enfrente el avatar, con las mismas reglas
     * que un clic allí (RN-36).
     */
    public void onUseTool() {
        Result<MapView> map = controller.map();
        if (!map.isOk()) {
            show(false, CLEAR, map.message());
            return;
        }
        AvatarView avatar = map.value().avatar();
        int col = avatar == null ? -1 : avatar.frontCol();
        int row = avatar == null ? -1 : avatar.frontRow();
        if (col < 0 || row < 0 || col >= map.value().cols() || row >= map.value().rows()) {
            show(false, CLEAR, Texts.NOTHING_IN_FRONT);
            return;
        }
        onPlotClicked(col, row);
    }

    /**
     * E: hablar con la persona que está frente al avatar, o si no con cualquier persona a su lado (RN-37).
     */
    public void onTalkKey() {
        Result<MapView> map = controller.map();
        if (!map.isOk()) {
            show(false, CLEAR, map.message());
            return;
        }
        AvatarView avatar = map.value().avatar();
        if (avatar == null) {
            show(false, CLEAR, Texts.NOBODY_TO_TALK);
            return;
        }
        NpcView npc = map.value().npcAt(avatar.frontCol(), avatar.frontRow());
        if (npc == null) {
            npc = map.value().npcs().stream().filter(n -> n.isNextTo(avatar.col(), avatar.row())).findFirst()
                    .orElse(null);
        }
        if (npc == null) {
            show(false, CLEAR, Texts.NOBODY_TO_TALK);
            return;
        }
        onTalk(npc.id());
    }

    /**
     * Habla con una persona del valle: se abre su diálogo, o se muestra la razón por la que no se puede (muy lejos).
     */
    public void onTalk(String npcId) {
        Result<DialogueView> result = controller.talk(npcId);
        if (result.isOk()) {
            messages.accept(CLEAR);
            dialogs.showDialogue(result.value());
        } else {
            show(false, CLEAR, result.message());
        }
    }

    /** Llegó el resumen del día: se muestra al jugador (HU-17). */
    public void onSummary(DaySummaryView summary) {
        dialogs.showSummary(summary);
    }

    private void onFeed(BuildingView building) {
        if (building == null) {
            show(false, CLEAR, Texts.NO_BUILDING_HERE);
        } else {
            report(controller.feed(building.id()));
        }
    }

    private void onCollect(BuildingView building) {
        report(controller.collect(building.id()));
    }

    private void report(Result<BuildingActionView> result) {
        if (!result.isOk()) {
            show(false, CLEAR, result.message());
        } else {
            String notice = result.value().notice();
            messages.accept(notice == null ? CLEAR : notice);
            if (notice != null) {
                toasts.info(notice);
            }
        }
    }

    private BuildingView buildingAt(int col, int row) {
        Result<MapView> map = controller.map();
        return map.isOk() ? map.value().buildingAt(col, row) : null;
    }

    /** El botón Guardar: se abren las ranuras para que el jugador elija dónde guardar (CU-14, HU-29). */
    public void onSave() {
        dialogs.showSlots(true);
    }

    /** La última ranura guardada o cargada, o 0 si no se ha usado ninguna. */
    public int lastSlot() {
        return lastSlot;
    }

    /** Recuerda la última ranura que el jugador guardó o cargó, para las teclas rápidas. */
    public void useSlot(int slot) {
        this.lastSlot = slot;
    }

    /**
     * Un atajo de teclado (RN-34): terminar el día, abrir el mercado, el inventario o los logros, y guardar o cargar
     * rápido en la última ranura usada (se abre el diálogo de ranuras si todavía no se ha usado ninguna).
     */
    public void onShortcut(Shortcut shortcut) {
        switch (shortcut) {
            case END_DAY -> onEndDay();
            case MARKET -> dialogs.showMarket();
            case INVENTORY -> dialogs.showInventory();
            case ACHIEVEMENTS -> dialogs.showAchievements();
            case QUICK_SAVE -> quickSave();
            case QUICK_LOAD -> quickLoad();
            default -> throw new IllegalStateException("Unhandled shortcut " + shortcut);
        }
    }

    private void quickSave() {
        if (lastSlot == 0) {
            dialogs.showSlots(true);
            return;
        }
        Result<SaveSlotView> result = controller.save(lastSlot);
        show(result.isOk(), Texts.SAVED_IN_SLOT + lastSlot + ".", result.message());
    }

    private void quickLoad() {
        if (lastSlot == 0) {
            dialogs.showSlots(false);
            return;
        }
        Result<HudView> result = controller.load(lastSlot);
        show(result.isOk(), Texts.LOADED_FROM_SLOT + lastSlot + ".", result.message());
    }
}
