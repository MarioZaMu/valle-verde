package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.AnimalView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.render.MapGeometry;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.render.MapRenderer;
import edu.ucc.farmsim.ui.render.VisualEffects;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;

/** HU-41: los cultivos y los animales se mueven con suavidad, y el movimiento nunca toca el juego (RN-08, RN-25). */
class HU41AnimationsTest {

    private static final int W = 1024;
    private static final int H = 600;

    private final AtomicLong now = new AtomicLong(0);
    private final VisualEffects fx = new VisualEffects(now::get, new Random(3));

    private static MapView farm(String stage, boolean withHen) {
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 16; col++) {
                plots.add(col == 5 && row == 3
                        ? new PlotView(col, row, "SOIL", "CARROT", stage, true, 2)
                        : new PlotView(col, row, "SOIL", null, null, false, 0));
            }
        }
        List<BuildingView> buildings = withHen
                ? List.of(new BuildingView(0, "COOP", "Gallinero", 2, 7, 2, 2, 4, "Huevo", 0,
                        List.of(new AnimalView(0, "HEN", "Gallina", "HEALTHY", "Sana", 0, true, 0),
                                new AnimalView(1, "HEN", "Gallina", "HEALTHY", "Sana", 0, true, 0))))
                : List.of();
        return new MapView(16, 10, plots, buildings);
    }

    private static int[] paint(MapView map, VisualEffects effects) {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        new MapRenderer().render(g, map, W, H, null, effects);
        g.dispose();
        var area = MapGeometry.fit(16, 10, W, H).cellBounds(2, 7);
        return image.getRGB(area.x, area.y, area.width * 2, area.height * 2, null, 0, area.width * 2);
    }

    @Test
    void HU41_ca1_everyAnimalCyclesThroughTwoOrThreeFrames() {
        Set<Integer> frames = new HashSet<>();
        for (int t = 0; t < 2_000; t += 50) {
            now.set(t);
            frames.add(fx.animalFrame(0));
        }

        assertTrue(frames.size() >= 2 && frames.size() <= 3, "frames: " + frames);
        assertEquals(VisualEffects.ANIMAL_FRAMES, 3);
        assertTrue(frames.stream().allMatch(f -> f >= 0 && f < VisualEffects.ANIMAL_FRAMES));
    }

    @Test
    void HU41_ca1_theAnimalIsDrawnDifferentlyOnEachFrame() {
        MapView map = farm("MATURE", true);
        fx.onMap(map);
        now.set(0);
        int[] first = paint(map, fx);
        now.set(VisualEffects.FRAME_MS);
        int[] second = paint(map, fx);

        assertFalse(Arrays.equals(first, second));
    }

    @Test
    void HU41_ca1_twoAnimalsDoNotMoveInStep() {
        for (int t = 0; t < 1_000; t += 25) {
            now.set(t);
            if (fx.animalFrame(0) != fx.animalFrame(1)) {
                return;
            }
        }
        throw new AssertionError("both animals always show the same frame");
    }

    @Test
    void HU41_ca2_aCropThatChangesStageGrowsInAtMost500Milliseconds() {
        fx.onMap(farm("SPROUT", false));
        assertEquals(1f, fx.cropScale(5, 3));

        now.set(1_000);
        fx.onMap(farm("GROWING", false));

        assertTrue(fx.cropScale(5, 3) < 1f);
        assertTrue(VisualEffects.GROWTH_MS <= 500);
        now.set(1_000 + VisualEffects.GROWTH_MS);
        assertEquals(1f, fx.cropScale(5, 3));
        assertEquals(1f, fx.cropScale(0, 0));
    }

    @Test
    void HU41_ca2_theFirstMapAndAnUnchangedMapAnimateNothing() {
        fx.onMap(farm("SPROUT", false));
        fx.onMap(farm("SPROUT", false));

        assertEquals(1f, fx.cropScale(5, 3));
        assertFalse(fx.needsFrames());
    }

    @Test
    void HU41_ca3_theStateAlreadyAdvancedWhenTheTransitionStartsAndStepsNeverChangeIt() {
        TestApp app = TestApp.startedGame();
        List<Integer> dayWhenAnnounced = new ArrayList<>();
        app.bus.subscribe(event -> {
            if (event instanceof GameEvent.DayEnded ended) {
                dayWhenAnnounced.add(app.controller.hud().value().day());
                assertEquals(ended.hud().day(), app.controller.hud().value().day());
                fx.onDayEnded();
            }
        });

        app.controller.endDay();
        var advanced = app.state().createMemento();
        for (int i = 0; i < 40; i++) {
            now.addAndGet(25);
            fx.step();
        }

        assertEquals(List.of(2), dayWhenAnnounced);
        assertEquals(advanced, app.state().createMemento());
    }

    @Test
    void HU41_ca4_theViewMovesThemWithASwingTimerAndSwitchingThemOffStopsIt() throws Exception {
        TestApp app = TestApp.startedGame();
        MapPanel panel = new MapPanel(app.controller, app.bus, (col, row) -> { });
        Field timerField = null;
        for (Field field : MapPanel.class.getDeclaredFields()) {
            if (field.getType() == Timer.class) {
                timerField = field;
            }
        }
        assertTrue(timerField != null, "MapPanel owns a javax.swing.Timer");
        timerField.setAccessible(true);
        app.controller.endDay();
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
        var before = app.state().createMemento();

        panel.setEffectsEnabled(false);

        assertFalse(panel.isAnimating());
        assertFalse(((Timer) timerField.get(panel)).isRunning());
        assertEquals(before, app.state().createMemento());
        panel.setEffectsEnabled(true);
        assertTrue(panel.effects().isEnabled());
        app.controller.endDay();
        assertEquals(3, app.controller.hud().value().day());
        assertEquals(GameFixtures.SOIL_ROW, 3);
    }
}
