package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.RecordingDialogs;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.Tool;
import edu.ucc.farmsim.ui.components.ToastKind;
import edu.ucc.farmsim.ui.components.ToastLayer;
import edu.ucc.farmsim.ui.components.ToastPresenter;
import edu.ucc.farmsim.ui.components.ToastQueue;
import edu.ucc.farmsim.ui.components.ToastView;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.screens.GameScreenPresenter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-39: short notices that appear and fade without interrupting the game. */
class HU39ToastsTest {

    private final AtomicLong now = new AtomicLong(1_000);
    private final ToastQueue queue = new ToastQueue(now::get);

    private static HudView hud(String season) {
        return new HudView(500, 1, season, 1, "SUNNY", "SUNNY", 100, 100);
    }

    @Test
    void HU39_ca1_aSuccessToastStaysThreeSecondsAndFadesInFourTenths() {
        queue.post(ToastKind.SUCCESS, "Listo");

        assertEquals(1.0f, queue.visible().get(0).opacity());
        now.addAndGet(3_000);
        assertEquals(1, queue.visible().size());
        assertEquals(1.0f, queue.visible().get(0).opacity());
        now.addAndGet(200);
        float halfway = queue.visible().get(0).opacity();
        assertTrue(halfway > 0.4f && halfway < 0.6f, "fading: " + halfway);
        now.addAndGet(200);
        assertTrue(queue.visible().isEmpty());
    }

    @Test
    void HU39_ca1_anAchievementToastLastsFiveSeconds() {
        queue.post(ToastKind.ACHIEVEMENT, "Logro");

        now.addAndGet(4_999);
        assertEquals(1.0f, queue.visible().get(0).opacity());
        now.addAndGet(401);
        assertTrue(queue.visible().isEmpty());
        assertEquals(5_000, ToastKind.ACHIEVEMENT.durationMs());
        assertEquals(3_000, ToastKind.INFO.durationMs());
        assertEquals(3_000, ToastKind.ERROR.durationMs());
    }

    @Test
    void HU39_ca2_aRejectionBecomesAnErrorToastWithTheSpanishMessage() {
        TestApp app = TestApp.startedGame();
        ToastPresenter toasts = new ToastPresenter(queue);
        ToolModel tools = new ToolModel();
        tools.select(Tool.SOW);
        tools.selectSeed("PUMPKIN_SEED");
        app.state().inventory().add(edu.ucc.farmsim.domain.economy.ItemType.PUMPKIN_SEED, 2);
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, tools, text -> { },
                new RecordingDialogs(), toasts);

        presenter.onPlotClicked(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW);

        List<ToastView> shown = queue.visible();
        assertEquals(1, shown.size());
        assertEquals(ToastKind.ERROR, shown.get(0).kind());
        assertEquals("No se puede sembrar Calabaza en Primavera: solo en Otoño.", shown.get(0).text());
    }

    @Test
    void HU39_ca3_onlyFourAreVisibleAndTheFifthWaitsUntilOneDisappears() {
        for (int i = 1; i <= 5; i++) {
            queue.post(ToastKind.INFO, "Aviso " + i);
        }

        assertEquals(4, queue.visible().size());
        assertEquals(1, queue.waiting());
        assertEquals("Aviso 1", queue.visible().get(0).text());

        now.addAndGet(3_400);
        List<ToastView> later = queue.visible();
        assertEquals(1, later.size());
        assertEquals("Aviso 5", later.get(0).text());
        assertEquals(0, queue.waiting());
    }

    @Test
    void HU39_ca3_theFifthStartsItsOwnCountWhenItBecomesVisible() {
        for (int i = 1; i <= 5; i++) {
            queue.post(ToastKind.INFO, "Aviso " + i);
        }
        now.addAndGet(3_400);
        queue.visible();

        now.addAndGet(2_900);

        assertEquals(1, queue.visible().size());
        assertFalse(queue.isIdle());
    }

    @Test
    void HU39_ca4_theStartOfASeasonShowsAnInformativeToast() {
        ToastPresenter toasts = new ToastPresenter(queue);
        toasts.onEvent(new GameEvent.GameStarted(hud("SPRING")));
        toasts.onEvent(new GameEvent.DayEnded(hud("SPRING")));
        assertTrue(queue.visible().isEmpty());

        toasts.onEvent(new GameEvent.DayEnded(hud("SUMMER")));

        List<ToastView> shown = queue.visible();
        assertEquals(1, shown.size());
        assertEquals(ToastKind.INFO, shown.get(0).kind());
        assertEquals("Comienza el Verano.", shown.get(0).text());
    }

    @Test
    void HU39_ca4_aRealSeasonChangeReachesTheToastsThroughTheBus() {
        TestApp app = TestApp.startedGame();
        ToastPresenter toasts = new ToastPresenter(queue);
        app.bus.subscribe(toasts);
        app.controller.hud();
        toasts.onEvent(new GameEvent.GameStarted(hud("SPRING")));
        GameFixtures.setDate(1, Season.SPRING, 28);

        app.controller.endDay();

        assertEquals("Comienza el Verano.", queue.visible().get(0).text());
    }

    @Test
    void HU39_ca1_anUnlockedAchievementShowsAFiveSecondToast() {
        ToastPresenter toasts = new ToastPresenter(queue);

        toasts.onEvent(new GameEvent.AchievementUnlocked("LG-01", "Primera cosecha", 100));

        ToastView toast = queue.visible().get(0);
        assertEquals(ToastKind.ACHIEVEMENT, toast.kind());
        assertEquals("¡Logro desbloqueado: Primera cosecha! +100 m", toast.text());
    }

    @Test
    void HU39_theLayerPaintsTheToastsAndNeverCatchesTheMouse() {
        queue.post(ToastKind.ERROR, "Algo falló");
        ToastLayer layer = new ToastLayer(queue);
        layer.setSize(600, 400);
        BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();

        layer.paint(g);
        g.dispose();

        boolean painted = false;
        for (int y = 0; y < 400 && !painted; y++) {
            for (int x = 0; x < 600; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    painted = true;
                    break;
                }
            }
        }
        assertTrue(painted);
        assertFalse(layer.contains(300, 200));
    }

    @Test
    void HU39_postsFromAnyThreadAreKept() throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Thread thread = new Thread(() -> queue.post(ToastKind.INFO, "x"));
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(4, queue.visible().size());
        assertEquals(4, queue.waiting());
    }
}
