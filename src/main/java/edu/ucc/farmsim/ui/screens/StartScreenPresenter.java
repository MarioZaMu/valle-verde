package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.components.SlotsPresenter;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.function.LongSupplier;

/**
 * Lógica de la pantalla de inicio, sin Swing: qué pasa cuando se pulsa cada botón.
 *
 * <p>Stories: HU-28, HU-30, HU-33.
 */
public final class StartScreenPresenter {

    private final GameCommands controller;
    private final ScreenNavigator navigator;
    private final LongSupplier seeds;

    /**
     * Crea el presentador.
     *
     * @param controller punto de entrada al juego
     * @param navigator  cambia las pantallas y habla con el jugador
     * @param seeds      provee la semilla de cada partida nueva (el reloj en producción, una constante en las pruebas)
     */
    public StartScreenPresenter(GameCommands controller, ScreenNavigator navigator, LongSupplier seeds) {
        this.controller = controller;
        this.navigator = navigator;
        this.seeds = seeds;
    }

    /** «Nueva partida»: pide confirmación si hay una partida en curso, luego inicia una y muestra el juego. */
    public void onNewGame() {
        navigator.prepareGame();
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

    /** «Cargar»: avisa al jugador cuando no hay nada que cargar, o le deja elegir una ranura y la carga. */
    public void onLoad() {
        navigator.prepareGame();
        Result<Boolean> saved = controller.hasSavedGames();
        if (!saved.isOk()) {
            navigator.showMessage(saved.message());
        } else if (!saved.value()) {
            navigator.showMessage(Texts.NO_SAVED_GAMES);
        } else {
            chooseAndLoad();
        }
    }

    private void chooseAndLoad() {
        SlotsPresenter slots = new SlotsPresenter(controller);
        if (!slots.error().isEmpty()) {
            navigator.showMessage(slots.error());
            return;
        }
        int slot = navigator.choose(Texts.CHOOSE_SLOT, slots.lines());
        if (slot == 0) {
            return;
        }
        Result<HudView> result = slots.load(slot);
        if (result.isOk()) {
            navigator.showGame();
        } else {
            navigator.showMessage(result.message());
        }
    }

    /** «Salir»: cierra la aplicación. */
    public void onExit() {
        navigator.exit();
    }
}
