package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.domain.farm.AvatarSnapshot;
import edu.ucc.farmsim.domain.farm.Direction;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.components.WalkKeys;
import edu.ucc.farmsim.ui.render.CellPos;
import edu.ucc.farmsim.ui.render.MapGeometry;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.render.MapRenderer;
import edu.ucc.farmsim.ui.render.VisualEffects;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-50 on the screen: 48 px plots, a camera that follows the avatar, tall people and a walk without pauses. */
class HU50CameraAndWalkTest {

    private static final int COLS = 48;
    private static final int ROWS = 24;
    private static final Rectangle SCREEN = new Rectangle(0, 0, 1920, 1080);

    private static MapGeometry follow(double col, double row) {
        return MapGeometry.follow(COLS, ROWS, MapGeometry.SCREEN_CELL, SCREEN, col, row);
    }

    @Test
    void HU50_ca3_at1920x1080APlotIs48PxAndTheCameraCentersTheAvatarWithoutShowingOutsideTheValley() {
        assertEquals(48, MapGeometry.cellFor(1920, 1080, 1.0));
        assertEquals(38, MapGeometry.cellFor(1536, 864, 1.25), "48 screen pixels with Windows at 125 %");
        assertEquals(40, MapGeometry.cellFor(480, 320, 1.0), "a tiny window fits 12 x 8 plots");

        MapGeometry middle = follow(24, 12);
        Rectangle avatar = middle.cellBounds(24, 12);
        assertEquals(960, avatar.x + avatar.width / 2, "centered across");
        assertEquals(540, avatar.y + avatar.height / 2, "centered down");

        MapGeometry west = follow(2, 1);
        assertEquals(0, west.originX(), "never left of the valley");
        assertEquals(0, west.originY());
        MapGeometry east = follow(46, 22);
        assertEquals(1920 - COLS * 48, east.originX(), "never right of it");
        assertEquals(1080 - ROWS * 48, east.originY());

        MapGeometry small = MapGeometry.follow(16, 10, 48, SCREEN, 0, 0);
        assertEquals((1920 - 16 * 48) / 2, small.originX(), "a valley narrower than the window is centered");
        assertEquals(new CellPos(24, 12), middle.cellAt(960, 540), "a click hits the plot drawn there");
        assertNull(west.cellAt(-5, 10));
    }

    @Test
    void HU50_ca3_theCameraFollowsTheStepWithoutJumpsAndKeepsTheAvatarInTheFreePartOfTheView() {
        AtomicLong now = new AtomicLong(5_000);
        VisualEffects fx = new VisualEffects(now::get, new Random(1));
        fx.onMap(field(new AvatarView(24, 12, "EAST")));
        fx.onMap(field(new AvatarView(25, 12, "EAST")));
        long start = now.get();
        List<Integer> origins = new ArrayList<>();
        for (long t = 0; t <= VisualEffects.STEP_MS; t += 30) {
            now.set(start + t);
            double[] at = fx.avatarPosition();
            origins.add(follow(at[0], at[1]).originX());
        }
        for (int i = 1; i < origins.size(); i++) {
            int jump = origins.get(i - 1) - origins.get(i);
            assertTrue(jump >= 0 && jump <= 12, "smooth: " + origins);
        }
        assertEquals(follow(25, 12).originX(), (int) origins.get(origins.size() - 1));
        now.set(start + 10 * VisualEffects.STEP_MS);
        assertArrayEquals(new double[] {25, 12}, fx.avatarPosition());

        Rectangle free = new Rectangle(0, 120, 1920, 760);
        MapGeometry withBars = MapGeometry.follow(COLS, ROWS, 48, free, 24, 12);
        Rectangle avatar = withBars.cellBounds(24, 12);
        assertTrue(free.contains(avatar), "the avatar is never under a bar");
    }

    @Test
    void HU50_ca3_theMapPanelUsesTheCameraOnTheRealValley() {
        TestApp app = TestApp.startedGame();
        MapPanel panel = new MapPanel(app.controller, app.bus, (col, row) -> { });
        panel.setSize(1920, 1080);

        assertEquals(48, panel.geometry().cellSize());
        assertEquals(0, panel.geometry().originX(), "the avatar starts in the west of the farm");
        app.state().avatar().restoreFrom(new AvatarSnapshot(40, 12, Direction.EAST));
        panel.reload();
        assertEquals(1920 - COLS * 48, panel.geometry().originX(), "in the village the camera went east");
    }

    @Test
    void HU50_ca4_peopleAre32x64SpritesDrawnOneByTwoPlotsWithTheirFeetOnTheirPlot() {
        AssetManager assets = AssetManager.fromClasspath();
        for (String name : List.of("AVATAR_S_0", "AVATAR_E_2", "MARTA", "TOMAS", "LUCIA", "INES")) {
            BufferedImage sprite = assets.sprite("characters/" + name + ".png").orElseThrow();
            assertEquals(32, sprite.getWidth(), name);
            assertEquals(64, sprite.getHeight(), name);
        }
        for (AssetManager pack : List.of(assets, AssetManager.vectorOnly())) {
            MapRenderer renderer = new MapRenderer(pack);
            assertFalse(Arrays.equals(paint(renderer, field(null), 7, 6), paint(renderer, field(new AvatarView(7, 7, "SOUTH")), 7, 6)),
                    "the head rises into the plot above");
            assertTrue(Arrays.equals(paint(renderer, field(null), 7, 8), paint(renderer, field(new AvatarView(7, 7, "SOUTH")), 7, 8)),
                    "nothing below the feet");
        }
    }

    @Test
    void HU50_ca5_holdingAKeyStepsAtOnceThenEvery180MsWithoutPauseAndReleasingStops() {
        WalkKeys keys = new WalkKeys(VisualEffects.STEP_MS);

        assertEquals("EAST", keys.press("EAST", 1_000), "the first step goes at once");
        assertNull(keys.press("EAST", 1_030), "the key repeat of the system is ignored");
        assertNull(keys.tick(1_100));
        List<Long> steps = new ArrayList<>();
        for (long t = 1_100; t <= 1_800; t += 10) {
            if ("EAST".equals(keys.tick(t))) {
                steps.add(t);
            }
        }
        assertEquals(List.of(1_180L, 1_360L, 1_540L, 1_720L), steps, "one step every 180 ms, no pause");

        assertEquals("NORTH", keys.press("NORTH", 1_900), "the last key pressed leads");
        keys.release("NORTH");
        assertEquals("EAST", keys.tick(2_080), "back to the key still held");
        keys.release("EAST");
        assertFalse(keys.isWalking());
        assertNull(keys.tick(3_000), "released: it stops");
        assertEquals("WEST", keys.press("WEST", 3_050), "a new press walks again at once");
    }

    @Test
    void HU50_ca5_holdingTwoKeysLikeWAndDWalksDiagonallyAtTheSamePaceWithoutAnExtraStep() {
        WalkKeys keys = new WalkKeys(VisualEffects.STEP_MS);

        assertEquals("NORTH", keys.press("NORTH", 1_000));
        assertNull(keys.press("EAST", 1_060), "the second key waits for the step under way: no jerk");
        List<String> steps = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        for (long t = 1_000; t <= 1_900; t += 10) {
            String step = keys.tick(t);
            if (step != null) {
                steps.add(step);
                times.add(t);
            }
        }
        assertEquals(List.of("EAST", "NORTH", "EAST", "NORTH", "EAST"), steps, "it zigzags: a diagonal");
        assertEquals(List.of(1_180L, 1_360L, 1_540L, 1_720L, 1_900L), times, "still one step every 180 ms");
        assertEquals("NORTH", keys.alternative("EAST"), "if east is blocked, north is tried instead");

        keys.release("NORTH");
        assertEquals("EAST", keys.tick(2_080), "with one key left it walks straight");
        assertEquals("EAST", keys.tick(2_260));
        assertNull(keys.alternative("EAST"));
        keys.press("WEST", 2_300);
        assertEquals("WEST", keys.tick(2_440), "opposite keys do not zigzag: the last one leads");
        assertEquals("WEST", keys.tick(2_620));
    }

    @Test
    void HU50_ca5_theWalkAnimationKeepsGoingAcrossStepsInsteadOfRestarting() {
        AtomicLong now = new AtomicLong(10_000);
        VisualEffects fx = new VisualEffects(now::get, new Random(1));
        fx.onMap(field(new AvatarView(5, 5, "EAST")));
        fx.onMap(field(new AvatarView(6, 5, "EAST")));
        now.addAndGet(VisualEffects.STEP_MS);
        fx.onMap(field(new AvatarView(7, 5, "EAST")));
        now.addAndGet(VisualEffects.STEP_MS / 3 + 1);

        assertTrue(fx.isWalking());
        assertEquals(1, fx.avatarFrame(), "the second step goes on with the cycle of the first");
    }

    private static MapView field(AvatarView avatar) {
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 16; col++) {
                plots.add(new PlotView(col, row, "GRASS", null, null, false, 0));
            }
        }
        return new MapView(16, 10, plots, List.of(), avatar);
    }

    private static int[] paint(MapRenderer renderer, MapView map, int col, int row) {
        BufferedImage image = new BufferedImage(1024, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        renderer.render(g, map, 1024, 640, null, null);
        g.dispose();
        Rectangle c = MapGeometry.fit(16, 10, 1024, 640).cellBounds(col, row);
        return image.getRGB(c.x, c.y, c.width, c.height, null, 0, c.width);
    }
}
