package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.components.Shortcut;
import edu.ucc.farmsim.ui.components.Tool;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.function.Consumer;

/**
 * Logic of the game screen, with no Swing: what happens when the player clicks a plot, presses a button or
 * uses a shortcut. Messages go to a sink (the status bar in production); an empty string clears the bar.
 *
 * <p>Stories: HU-10, HU-17, HU-18, HU-20, HU-22, HU-34, HU-36.
 */
public final class GameScreenPresenter {

    private static final String CLEAR = "";

    private final GameCommands controller;
    private final ToolModel tools;
    private final Consumer<String> messages;
    private final GameDialogs dialogs;

    /**
     * Creates the presenter.
     *
     * @param controller entry point to the game
     * @param tools      the selected tool and seed
     * @param messages   where messages for the player are sent
     */
    public GameScreenPresenter(GameCommands controller, ToolModel tools, Consumer<String> messages,
                               GameDialogs dialogs) {
        this.controller = controller;
        this.tools = tools;
        this.messages = messages;
        this.dialogs = dialogs;
    }

    /** "End day": the top bar updates through the events; a message appears only if the day could not end. */
    public void onEndDay() {
        Result<HudView> result = controller.endDay();
        messages.accept(result.isOk() ? CLEAR : result.message());
    }

    /**
     * A click on a plot: the selected tool acts on it. Without a tool nothing happens, except on a building,
     * whose panel opens. Build opens the construction dialog; Feed and Harvest act on the building under the
     * click (HU-36 · CA-4).
     */
    public void onPlotClicked(int col, int row) {
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
            messages.accept(Texts.NO_SEED_SELECTED);
            return;
        }
        BuildingView building = tool == Tool.FEED || tool == Tool.HARVEST ? buildingAt(col, row) : null;
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
            messages.accept(result.isOk() ? CLEAR : result.message());
        }
    }

    /** The summary of the day arrived: it is shown to the player (HU-17). */
    public void onSummary(DaySummaryView summary) {
        dialogs.showSummary(summary);
    }

    private void onFeed(BuildingView building) {
        if (building == null) {
            messages.accept(Texts.NO_BUILDING_HERE);
        } else {
            report(controller.feed(building.id()));
        }
    }

    private void onCollect(BuildingView building) {
        report(controller.collect(building.id()));
    }

    private void report(Result<BuildingActionView> result) {
        if (!result.isOk()) {
            messages.accept(result.message());
        } else {
            String notice = result.value().notice();
            messages.accept(notice == null ? CLEAR : notice);
        }
    }

    private BuildingView buildingAt(int col, int row) {
        Result<MapView> map = controller.map();
        return map.isOk() ? map.value().buildingAt(col, row) : null;
    }

    /** A keyboard shortcut. Only "end day" exists in this phase; the others explain that they will come. */
    public void onShortcut(Shortcut shortcut) {
        if (shortcut == Shortcut.END_DAY) {
            onEndDay();
        } else if (shortcut == Shortcut.MARKET) {
            dialogs.showMarket();
        } else if (shortcut == Shortcut.INVENTORY) {
            dialogs.showInventory();
        } else {
            messages.accept(Texts.SHORTCUT_NOT_AVAILABLE_YET);
        }
    }
}
