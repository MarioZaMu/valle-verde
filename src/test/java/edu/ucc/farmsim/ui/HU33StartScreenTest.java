package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.application.controller.GameController;
import edu.ucc.farmsim.support.FakeSaveRepository;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.ui.screens.ScreenNavigator;
import edu.ucc.farmsim.ui.screens.StartScreen;
import edu.ucc.farmsim.ui.screens.StartScreenPresenter;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import org.junit.jupiter.api.Test;

/**
 * HU-33: pantalla de inicio con «Nueva partida», «Cargar» y «Salir». La lógica de la pantalla vive en {@link
 * StartScreenPresenter}, que no tiene Swing, así que se prueba sin pantalla. También contiene el lado de UI de HU-28 ·
 * CA-3 (confirmación antes de descartar una partida en curso).
 */
class HU33StartScreenTest {

    /** Registra lo que el presentador le pide hacer a la navegación. */
    private static class FakeNavigator implements ScreenNavigator {
        final List<String> calls = new ArrayList<>();
        boolean answerToConfirmation = true;
        int chosenSlot;
        List<String> offered = List.of();

        @Override public void showStart() { calls.add("start"); }
        @Override public void showGame() { calls.add("game"); }
        @Override public void showMessage(String text) { calls.add("message:" + text); }
        @Override public boolean confirm(String text) { calls.add("confirm:" + text); return answerToConfirmation; }
        @Override public int choose(String title, List<String> options) {
            calls.add("choose:" + title);
            offered = options;
            return chosenSlot;
        }
        @Override public void exit() { calls.add("exit"); }
    }

    @Test
    void HU33_ca1_startScreenOffersTheThreeButtonsInSpanish() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.create();
        StartScreen screen = new StartScreen(new StartScreenPresenter(app.controller, new FakeNavigator(), () -> 1L));

        List<String> labels = new ArrayList<>();
        collectButtons(screen, labels);

        assertEquals(List.of("Nueva partida", "Cargar", "Salir"), labels);
        assertEquals("Nueva partida", Texts.START_NEW_GAME);
        assertEquals("Cargar", Texts.START_LOAD);
        assertEquals("Salir", Texts.START_EXIT);
    }

    /** El controlador, escribiendo el nombre de cada orden en {@code calls}. */
    private static edu.ucc.farmsim.application.controller.GameCommands recording(TestApp app, List<String> calls) {
        return (edu.ucc.farmsim.application.controller.GameCommands) java.lang.reflect.Proxy.newProxyInstance(
                HU33StartScreenTest.class.getClassLoader(),
                new Class<?>[] {edu.ucc.farmsim.application.controller.GameCommands.class},
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    try {
                        return method.invoke(app.controller, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @Test
    void RNF02_newGameAndLoadPrepareTheGameScreenBeforeTheirFirstOrder() {
        TestApp app = TestApp.create();
        List<String> order = new ArrayList<>();
        FakeNavigator navigator = new FakeNavigator() {
            @Override
            public void prepareGame() {
                order.add("prepare");
            }
        };
        StartScreenPresenter presenter = new StartScreenPresenter(recording(app, order), navigator, () -> TestApp.SEED);

        presenter.onNewGame();
        assertEquals("prepare", order.get(0), String.valueOf(order));
        assertTrue(order.contains("newGame"));
        order.clear();
        presenter.onLoad();
        assertEquals("prepare", order.get(0), String.valueOf(order));
    }

    @Test
    void HU33_ca2_newGameStartsAGameAndShowsTheGameScreen() {
        TestApp app = TestApp.create();
        FakeNavigator navigator = new FakeNavigator();
        StartScreenPresenter presenter = new StartScreenPresenter(app.controller, navigator, () -> TestApp.SEED);

        presenter.onNewGame();

        assertTrue(app.controller.hasGameInProgress());
        assertEquals(List.of("game"), navigator.calls);
    }

    @Test
    void HU33_ca3_loadWithAllSlotsEmptyShowsAMessageAndStaysOnTheStartScreen() {
        TestApp app = TestApp.create();
        FakeNavigator navigator = new FakeNavigator();
        StartScreenPresenter presenter = new StartScreenPresenter(app.controller, navigator, () -> 1L);

        presenter.onLoad();

        assertEquals(List.of("message:No hay partidas guardadas."), navigator.calls);
        assertFalse(navigator.calls.contains("game"));
    }

    @Test
    void HU33_ca3_loadWithASavedGameOffersTheSlotsAndCancellingChangesNothing() {
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), new FakeSaveRepository().withSlot(2));
        FakeNavigator navigator = new FakeNavigator();

        new StartScreenPresenter(app.controller, navigator, () -> 1L).onLoad();

        assertEquals(List.of("choose:" + Texts.CHOOSE_SLOT), navigator.calls);
        assertEquals(3, navigator.offered.size());
        assertFalse(app.controller.hasGameInProgress());
    }

    @Test
    void HU30_ca3_choosingASavedSlotLoadsItAndShowsTheGameScreen() {
        FakeSaveRepository repository = new FakeSaveRepository();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), repository);
        app.controller.newGame(1L);
        app.controller.endDay();
        app.controller.save(3);
        app.controller.newGame(2L);
        FakeNavigator navigator = new FakeNavigator();
        navigator.chosenSlot = 3;

        new StartScreenPresenter(app.controller, navigator, () -> 1L).onLoad();

        assertEquals(List.of("choose:" + Texts.CHOOSE_SLOT, "game"), navigator.calls);
        assertEquals(2, app.controller.hud().value().day());
    }

    @Test
    void HU31_choosingAnEmptySlotShowsTheRn35MessageAndStaysOnTheStartScreen() {
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), new FakeSaveRepository().withSlot(1));
        FakeNavigator navigator = new FakeNavigator();
        navigator.chosenSlot = 3;

        new StartScreenPresenter(app.controller, navigator, () -> 1L).onLoad();

        assertEquals(List.of("choose:" + Texts.CHOOSE_SLOT, "message:No existe una partida en la ranura 3."),
                navigator.calls);
    }

    @Test
    void HU33_ca4_exitAsksTheNavigationToCloseTheApplication() {
        TestApp app = TestApp.create();
        FakeNavigator navigator = new FakeNavigator();

        new StartScreenPresenter(app.controller, navigator, () -> 1L).onExit();

        assertEquals(List.of("exit"), navigator.calls);
    }

    @Test
    void HU28_ca3_cancellingTheConfirmationKeepsTheGameInProgress() {
                // Dada una partida en curso donde se gastó energía
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        app.session.current().energy().trySpend(10);
        FakeNavigator navigator = new FakeNavigator();
        navigator.answerToConfirmation = false;

                // Cuando el jugador elige «Nueva partida» y cancela la confirmación
        new StartScreenPresenter(app.controller, navigator, () -> TestApp.SEED).onNewGame();

                // Entonces se preguntó la confirmación, no se mostró ninguna pantalla de juego y la partida no se tocó
        assertEquals(List.of("confirm:" + Texts.CONFIRM_DISCARD_GAME), navigator.calls);
        assertEquals(90, app.session.current().energy().current());
    }

    @Test
    void HU28_ca3_acceptingTheConfirmationStartsAFreshGame() {
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        app.session.current().energy().trySpend(10);
        FakeNavigator navigator = new FakeNavigator();

        new StartScreenPresenter(app.controller, navigator, () -> TestApp.SEED).onNewGame();

        assertEquals(List.of("confirm:" + Texts.CONFIRM_DISCARD_GAME, "game"), navigator.calls);
        assertEquals(100, app.session.current().energy().current());
    }

    @Test
    void HU33_aFailedNewGameShowsTheErrorMessageInsteadOfTheGameScreen() {
        GameSession faulty = new GameSession() {
            @Override
            public void replace(edu.ucc.farmsim.domain.game.GameState state) {
                throw new IllegalStateException("cannot start");
            }
        };
        GameController controller = TestApp.create(faulty, DayPipeline.standard()).controller;
        FakeNavigator navigator = new FakeNavigator();

        new StartScreenPresenter(controller, navigator, () -> 1L).onNewGame();

        assertEquals(List.of("message:Ocurrió un error inesperado. Tu partida no cambió."), navigator.calls);
    }

    private static void collectButtons(Container container, List<String> labels) {
        for (Component child : container.getComponents()) {
            if (child instanceof JButton button) {
                labels.add(button.getText());
            }
            if (child instanceof Container nested) {
                collectButtons(nested, labels);
            }
        }
    }
}
