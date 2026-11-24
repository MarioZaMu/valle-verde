package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.ToastKind;
import edu.ucc.farmsim.ui.components.ToastPresenter;
import edu.ucc.farmsim.ui.components.ToastQueue;
import edu.ucc.farmsim.ui.components.TutorialBox;
import edu.ucc.farmsim.ui.components.TutorialPresenter;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-49 en pantalla: el cuadro del avatar con el paso, qué hacer y el botón para saltarlo. */
class HU49TutorialBoxTest {

    private static TestApp newGame() {
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        return app;
    }

    private static TutorialBox box(TestApp app) {
        TutorialBox box = new TutorialBox(new TutorialPresenter(app.controller));
        app.bus.subscribe(box);
        box.refresh();
        return box;
    }

    @Test
    void HU49_ca1_aNewGameShowsTheAvatarsBoxWithStepOneAndTheSkipButton() {
        TestApp app = newGame();

        TutorialBox box = box(app);

        assertTrue(box.isVisible());
        assertEquals("Paso 1 de 5", box.stepText());
        assertTrue(box.messageText().contains("flechas"));
        assertEquals(Texts.SKIP_TUTORIAL, box.skipButton().getText());
        assertTrue(box.hasPortrait(), "the avatar is the one who speaks");
        Dimension size = box.getPreferredSize();
        assertTrue(size.width <= TutorialBox.MAX_WIDTH && size.height <= TutorialBox.MAX_HEIGHT,
                "it fits next to the top bar at 1024 x 768: " + size);
    }

    @Test
    void HU49_ca2_theBoxFollowsTheStepsAndDisappearsWithANoticeAtTheEnd() {
        TestApp app = newGame();
        TutorialBox box = box(app);
        ToastQueue queue = new ToastQueue(new AtomicLong(0)::get);
        app.bus.subscribe(new ToastPresenter(queue));

        app.controller.move("EAST");
        assertEquals("Paso 2 de 5", box.stepText());
        assertTrue(box.messageText().startsWith("Elige Sembrar"));
        app.controller.plant(8, 6, "CARROT_SEED");
        app.controller.water(8, 6);
        assertEquals("Paso 4 de 5", box.stepText());
        app.state().avatar().restoreFrom(new edu.ucc.farmsim.domain.farm.AvatarSnapshot(28, 11,
                edu.ucc.farmsim.domain.farm.Direction.EAST));
        app.controller.talk("MARTA");
        assertTrue(queue.visible().isEmpty(), "no notice before the end");
        app.controller.endDay();

        assertFalse(box.isVisible());
        assertEquals(1, queue.visible().stream().filter(t -> t.kind() == ToastKind.INFO
                && t.text().equals("¡Listo! Ya sabes lo básico. ¡Buena suerte en el valle!")).count());
    }

    @Test
    void HU49_ca4_theSkipButtonHidesTheBoxWithoutANoticeAndANewGameShowsItAgain() {
        TestApp app = newGame();
        TutorialBox box = box(app);
        ToastQueue queue = new ToastQueue(new AtomicLong(0)::get);
        app.bus.subscribe(new ToastPresenter(queue));

        box.skipButton().doClick();

        assertFalse(box.isVisible());
        assertTrue(app.controller.tutorial().value().finished());
        assertTrue(queue.visible().isEmpty(), "skipping is not celebrated");
        app.controller.newGame(TestApp.SEED);
        assertTrue(box.isVisible());
        assertEquals("Paso 1 de 5", box.stepText());
    }

    @Test
    void HU49_ca5_aLoadedGameWithTheTutorialFinishedOrNoGameAtAllShowNoBox() {
        TestApp none = TestApp.create();
        assertFalse(box(none).isVisible(), "without a game there is nothing to explain");

        TestApp app = TestApp.startedGame();
        TutorialBox box = box(app);
        assertFalse(box.isVisible());
        box.onEvent(new GameEvent.MapChanged());
        assertFalse(box.isVisible());
    }
}
