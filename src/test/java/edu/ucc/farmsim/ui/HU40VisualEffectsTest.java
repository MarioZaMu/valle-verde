package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.render.MapGeometry;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.render.MapRenderer;
import edu.ucc.farmsim.ui.render.Particle;
import edu.ucc.farmsim.ui.render.VisualEffects;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-40: the season and the weather are seen on the map, and only on the map (RN-01, RN-11, RN-12). */
class HU40VisualEffectsTest {

    private static final int W = 1024;
    private static final int H = 600;

    private final AtomicLong now = new AtomicLong(0);
    private final VisualEffects fx = new VisualEffects(now::get, new Random(7));

    private static MapView grassMap() {
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 16; col++) {
                plots.add(new PlotView(col, row, "GRASS", null, null, false, 0));
            }
        }
        return new MapView(16, 10, plots, List.<BuildingView>of());
    }

    private static BufferedImage paint(VisualEffects effects) {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        new MapRenderer().render(g, grassMap(), W, H, null, effects);
        g.dispose();
        return image;
    }

    private static int grassPixel(BufferedImage image) {
        var cell = MapGeometry.fit(16, 10, W, H).cellBounds(2, 5);
        return image.getRGB(cell.x + cell.width / 2, cell.y + cell.height / 2);
    }

    private static long brightness(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }

    @Test
    void HU40_ca1_everySeasonHasItsOwnPaletteOnTheMap() {
        Set<Integer> grass = new HashSet<>();
        for (String season : List.of("SPRING", "SUMMER", "AUTUMN", "WINTER")) {
            fx.onWorld(season, "SUNNY");
            grass.add(grassPixel(paint(fx)));
        }

        assertEquals(4, grass.size());
    }

    @Test
    void HU40_ca1_theNightTintDarkensTheMapDuringTheEndOfDayTransition() {
        fx.onWorld("SPRING", "SUNNY");
        long day = brightness(grassPixel(paint(fx)));

        fx.onDayEnded();
        now.set(VisualEffects.TRANSITION_MS / 2);
        long night = brightness(grassPixel(paint(fx)));
        now.set(VisualEffects.TRANSITION_MS);
        long after = brightness(grassPixel(paint(fx)));

        assertTrue(night < day, "night " + night + " day " + day);
        assertEquals(day, after);
        assertEquals(0f, fx.nightAlpha());
    }

    @Test
    void HU40_ca2_rainAndSnowShowParticlesAndOtherWeatherShowsNone() {
        fx.onWorld("SPRING", "RAINY");
        assertFalse(fx.particles().isEmpty());
        assertTrue(fx.particles().stream().allMatch(p -> p.kind() == Particle.Kind.RAIN));

        fx.onWorld("WINTER", "SNOWY");
        assertFalse(fx.particles().isEmpty());
        assertTrue(fx.particles().stream().allMatch(p -> p.kind() == Particle.Kind.SNOW));

        fx.onWorld("SUMMER", "SUNNY");
        assertTrue(fx.particles().isEmpty());
        fx.onWorld("SUMMER", "DROUGHT");
        assertTrue(fx.particles().isEmpty());
    }

    @Test
    void HU40_ca2_particlesMoveAndStayInsideTheMap() {
        fx.onWorld("SPRING", "RAINY");
        double before = fx.particles().get(0).y();

        for (int i = 0; i < 200; i++) {
            now.addAndGet(33);
            fx.step();
        }

        assertNotEquals(before, fx.particles().get(0).y());
        assertTrue(fx.particles().stream().allMatch(p -> p.x() >= 0 && p.x() <= 1 && p.y() >= 0 && p.y() <= 1));
    }

    @Test
    void HU40_ca2_rainFallsOnTheImageAndDroughtAndFrostHaveTheirOwnIndicator() {
        fx.onWorld("SPRING", "SUNNY");
        int[] sunny = pixels(paint(fx));

        fx.onWorld("SPRING", "RAINY");
        now.addAndGet(100);
        fx.step();
        int[] rain = pixels(paint(fx));
        fx.onWorld("SPRING", "DROUGHT");
        int[] drought = pixels(paint(fx));
        fx.onWorld("SPRING", "FROST");
        int[] frost = pixels(paint(fx));

        assertFalse(java.util.Arrays.equals(sunny, rain));
        assertFalse(java.util.Arrays.equals(sunny, drought));
        assertFalse(java.util.Arrays.equals(sunny, frost));
        assertFalse(java.util.Arrays.equals(drought, frost));
    }

    @Test
    void HU40_ca3_theGameStateIsTheSameWithTheEffectsOnOrOff() {
        edu.ucc.farmsim.domain.game.GameMemento with = play(true);
        edu.ucc.farmsim.domain.game.GameMemento without = play(false);

        assertEquals(without, with);
    }

    private static edu.ucc.farmsim.domain.game.GameMemento play(boolean effects) {
        TestApp app = TestApp.startedGame();
        MapPanel panel = new MapPanel(app.controller, app.bus, (col, row) -> { });
        panel.setEffectsEnabled(effects);
        app.controller.plant(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW, "CARROT_SEED");
        app.controller.water(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW);
        for (int i = 0; i < 4; i++) {
            app.controller.endDay();
            app.controller.water(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW);
        }
        return app.state().createMemento();
    }

    @Test
    void HU40_ca4_withParticlesActiveAFrameTakesLessThan33Milliseconds() {
        fx.onWorld("WINTER", "SNOWY");
        MapView map = grassMap();
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        MapRenderer renderer = new MapRenderer();
        renderer.render(g, map, W, H, null, fx);

        long start = System.nanoTime();
        for (int i = 0; i < 30; i++) {
            now.addAndGet(33);
            fx.step();
            renderer.render(g, map, W, H, null, fx);
        }
        double average = (System.nanoTime() - start) / 30 / 1_000_000.0;
        g.dispose();

        assertTrue(average < 33.0, "average frame: " + average + " ms");
        assertTrue(fx.particles().size() <= VisualEffects.MAX_PARTICLES);
    }

    @Test
    void HU40_ca4_theEffectsCanBeSwitchedOffAndTheMapGoesBackToItsPlainLook() {
        BufferedImage plain = paint(null);
        fx.onWorld("WINTER", "SNOWY");
        fx.onDayEnded();
        assertNotEquals(grassPixel(plain), grassPixel(paint(fx)));

        fx.setEnabled(false);

        assertFalse(fx.isEnabled());
        assertTrue(fx.particles().isEmpty());
        assertEquals(0f, fx.nightAlpha());
        assertEquals(grassPixel(plain), grassPixel(paint(fx)));
        assertFalse(fx.needsFrames());
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, W, H, null, 0, W);
    }
}
