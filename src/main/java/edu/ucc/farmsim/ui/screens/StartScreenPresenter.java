package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.controller.GameController;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.function.LongSupplier;

/**
 * Logic of the start screen, with no Swing: what happens when each button is pressed.
 *
 * <p>Stories: HU-33, HU-28.
 */
public final class StartScreenPresenter {

    private final GameController controller;
    private final ScreenNavigator navigator;
    private final LongSupplier seeds;

    /**
     * Creates the presenter.
     *
     * @param controller entry point to the game
     * @param navigator  switches screens and talks to the player
     * @param seeds      supplies the seed of each new game (the clock in production, a constant in tests)
     */
    public StartScreenPresenter(GameController controller, ScreenNavigator navigator, LongSupplier seeds) {
        this.controller = controller;
        this.navigator = navigator;
        this.seeds = seeds;
    }

    /** "New game": asks for confirmation if a game is in progress, then starts one and shows the game. */
    public void onNewGame() {
        if (controller.hasGameInProgress() && !navigator.confirm(Texts.CONFIRM_DISCARD_GAME)) {
            return;
        }
        Result<HudView> result = controller.newGame(seeds.getAsLong());
        if (result.isOk()) {
            navigator.showGame();
        } else {
            navigator.showMessage(result.message());
        }
    }

    /** "Load": tells the player when there is nothing to load (loading itself arrives with HU-30). */
    public void onLoad() {
        Result<Boolean> saved = controller.hasSavedGames();
        if (!saved.isOk()) {
            navigator.showMessage(saved.message());
        } else if (!saved.value()) {
            navigator.showMessage(Texts.NO_SAVED_GAMES);
        } else {
            navigator.showMessage(Texts.LOAD_NOT_AVAILABLE_YET);
        }
    }

    /** "Exit": closes the application. */
    public void onExit() {
        navigator.exit();
    }
}
