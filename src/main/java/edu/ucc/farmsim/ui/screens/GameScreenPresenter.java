package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.HudView;
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
 * <p>Stories: HU-10, HU-34, HU-36.
 */
public final class GameScreenPresenter {

    private static final String CLEAR = "";

    private final GameCommands controller;
    private final ToolModel tools;
    private final Consumer<String> messages;

    /**
     * Creates the presenter.
     *
     * @param controller entry point to the game
     * @param tools      the selected tool and seed
     * @param messages   where messages for the player are sent
     */
    public GameScreenPresenter(GameCommands controller, ToolModel tools, Consumer<String> messages) {
        this.controller = controller;
        this.tools = tools;
        this.messages = messages;
    }

    /** "End day": the top bar updates through the events; a message appears only if the day could not end. */
    public void onEndDay() {
        Result<HudView> result = controller.endDay();
        messages.accept(result.isOk() ? CLEAR : result.message());
    }

    /** A click on a plot: the selected tool acts on it. Without a tool, nothing happens. */
    public void onPlotClicked(int col, int row) {
        Tool tool = tools.current();
        if (tool == null) {
            return;
        }
        Result<PlotView> result = switch (tool) {
            case SOW -> controller.plant(col, row, tools.selectedSeed());
            case WATER -> controller.water(col, row);
            case HARVEST -> controller.harvest(col, row);
            case BUILD, FEED -> null;
        };
        if (result == null) {
            messages.accept(Texts.TOOL_NOT_AVAILABLE_YET);
        } else {
            messages.accept(result.isOk() ? CLEAR : result.message());
        }
    }

    /** A keyboard shortcut. Only "end day" exists in this phase; the others explain that they will come. */
    public void onShortcut(Shortcut shortcut) {
        if (shortcut == Shortcut.END_DAY) {
            onEndDay();
        } else {
            messages.accept(Texts.SHORTCUT_NOT_AVAILABLE_YET);
        }
    }
}
