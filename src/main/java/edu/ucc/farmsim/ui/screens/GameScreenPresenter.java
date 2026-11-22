package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.controller.GameController;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.result.Result;
import java.util.function.Consumer;

/**
 * Logic of the game screen, with no Swing: what happens when a button is pressed.
 *
 * <p>Stories: HU-10, HU-34.
 */
public final class GameScreenPresenter {

    private final GameController controller;
    private final Consumer<String> messages;

    /**
     * Creates the presenter.
     *
     * @param controller entry point to the game
     * @param messages   where messages for the player are sent (a dialog in production)
     */
    public GameScreenPresenter(GameController controller, Consumer<String> messages) {
        this.controller = controller;
        this.messages = messages;
    }

    /** "End day": the top bar updates through the events; a message appears only if the day could not end. */
    public void onEndDay() {
        Result<HudView> result = controller.endDay();
        if (!result.isOk()) {
            messages.accept(result.message());
        }
    }
}
