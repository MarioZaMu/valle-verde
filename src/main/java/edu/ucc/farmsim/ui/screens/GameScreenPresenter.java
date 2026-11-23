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
 * Logic of the game screen, with no Swing: what happens when the player clicks a plot, presses a button or
 * uses a shortcut. Messages go to a sink (the status bar in production); an empty string clears the bar.
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
     * Creates the presenter.
     *
     * @param controller entry point to the game
     * @param tools      the selected tool and seed
     * @param messages   where messages for the player are sent
     * @param dialogs    opens the dialogs
     * @param toasts     where errors and notices are shown as toasts (HU-39)
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
     * Creates the presenter without notices: only the message sink hears about the result of an order.
     *
     * @param controller entry point to the game
     * @param tools      the selected tool and seed
     * @param messages   where messages for the player are sent
     * @param dialogs    opens the dialogs
     */
    public GameScreenPresenter(GameCommands controller, ToolModel tools, Consumer<String> messages,
                               GameDialogs dialogs) {
        this(controller, tools, messages, dialogs, new ToastPresenter(new ToastQueue(() -> 0L)));
    }

    /** Sends a message to the status bar; a rejection also becomes an error notice (HU-39 · CA-2). */
    private void show(boolean ok, String onSuccess, String onFailure) {
        messages.accept(ok ? onSuccess : onFailure);
        if (!ok) {
            toasts.error(onFailure);
        } else if (!onSuccess.isEmpty()) {
            toasts.info(onSuccess);
        }
    }

    /** "End day": the top bar updates through the events; a message appears only if the day could not end. */
    public void onEndDay() {
        Result<HudView> result = controller.endDay();
        show(result.isOk(), CLEAR, result.message());
    }

    /**
     * A click on a plot: the selected tool acts on it. Without a tool nothing happens, except on a building,
     * whose panel opens. Build opens the construction dialog; Feed and Harvest act on the building under the
     * click (HU-36 · CA-4).
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
     * An arrow or W A S D: one step of the avatar (RN-36). A blocked step shows its message; holding the key
     * against a wall shows the notice once, not on every repeat.
     *
     * @param direction {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
     * @return {@code true} if the avatar moved; {@code false} if it only turned (or there is no game)
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
     * Space: the selected tool acts on the plot in front of the avatar, with the same rules as a click there
     * (RN-36).
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
     * E: talk to the person in front of the avatar, or else to any person next to it (RN-37).
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

    /** Talks to a person of the valley: its dialogue opens, or the reason it cannot (too far) is shown. */
    public void onTalk(String npcId) {
        Result<DialogueView> result = controller.talk(npcId);
        if (result.isOk()) {
            messages.accept(CLEAR);
            dialogs.showDialogue(result.value());
        } else {
            show(false, CLEAR, result.message());
        }
    }

    /** The summary of the day arrived: it is shown to the player (HU-17). */
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

    /** The slot saved or loaded last, or 0 if none has been used. */
    public int lastSlot() {
        return lastSlot;
    }

    /** Remembers the slot the player saved or loaded last, for the quick keys. */
    public void useSlot(int slot) {
        this.lastSlot = slot;
    }

    /**
     * A keyboard shortcut (RN-34): end the day, open the market, the inventory or the achievements, and save or
     * load quickly in the slot used last (the slots dialog opens if none has been used yet).
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
