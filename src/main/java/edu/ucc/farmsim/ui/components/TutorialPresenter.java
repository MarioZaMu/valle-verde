package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.TutorialView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;

/**
 * The logic of the avatar's box of the start tutorial (HU-49), without Swing: what to show and skipping it.
 *
 * <p>Stories: HU-49.
 */
public final class TutorialPresenter {

    private final GameCommands controller;
    private TutorialView view;

    /**
     * Creates the presenter.
     *
     * @param controller entry point to the game
     */
    public TutorialPresenter(GameCommands controller) {
        this.controller = controller;
    }

    /** Asks the game for the tutorial again; without a game there is nothing to show. */
    public void reload() {
        Result<TutorialView> result = controller.tutorial();
        view = result.isOk() ? result.value() : null;
    }

    /** Shows a tutorial that just changed, as it came in its event. */
    public void show(TutorialView changed) {
        view = changed;
    }

    /** True while there is a tutorial still running. */
    public boolean isActive() {
        return view != null && !view.finished();
    }

    /** "Paso n de 5"; empty when nothing is shown. */
    public String stepText() {
        return isActive() ? Texts.tutorialStep(view.number(), view.total()) : "";
    }

    /** What the avatar says now; empty when nothing is shown. */
    public String messageText() {
        return isActive() ? view.text() : "";
    }

    /** Ends the tutorial. */
    public void skip() {
        Result<TutorialView> result = controller.skipTutorial();
        if (result.isOk()) {
            view = result.value();
        }
    }
}
