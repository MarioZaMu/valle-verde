package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.TutorialView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;

/**
 * La lógica del cuadro del avatar del tutorial de inicio (HU-49), sin Swing: qué mostrar y saltarlo.
 *
 * <p>Stories: HU-49.
 */
public final class TutorialPresenter {

    private final GameCommands controller;
    private TutorialView view;

    /**
     * Crea el presentador.
     *
     * @param controller punto de entrada al juego
     */
    public TutorialPresenter(GameCommands controller) {
        this.controller = controller;
    }

    /** Le pide otra vez el tutorial a la partida; sin partida no hay nada que mostrar. */
    public void reload() {
        Result<TutorialView> result = controller.tutorial();
        view = result.isOk() ? result.value() : null;
    }

    /** Muestra un tutorial que acaba de cambiar, tal como llegó en su evento. */
    public void show(TutorialView changed) {
        view = changed;
    }

    /** Verdadero mientras hay un tutorial todavía en marcha. */
    public boolean isActive() {
        return view != null && !view.finished();
    }

    /** «Paso n de 5»; vacío cuando no se muestra nada. */
    public String stepText() {
        return isActive() ? Texts.tutorialStep(view.number(), view.total()) : "";
    }

    /** Lo que dice ahora el avatar; vacío cuando no se muestra nada. */
    public String messageText() {
        return isActive() ? view.text() : "";
    }

    /** Termina el tutorial. */
    public void skip() {
        Result<TutorialView> result = controller.skipTutorial();
        if (result.isOk()) {
            view = result.value();
        }
    }
}
