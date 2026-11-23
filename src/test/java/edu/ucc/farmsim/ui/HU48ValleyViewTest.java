package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.AnimalView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.render.MapGeometry;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.render.MapRenderer;
import edu.ucc.farmsim.ui.render.TitleScene;
import edu.ucc.farmsim.ui.render.VisualEffects;
import edu.ucc.farmsim.ui.theme.Palette;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

/** HU-48 · CA-6 to CA-8: a continuous 3/4 view of the valley, ambient life, and an animated start screen. */
class HU48ValleyViewTest {

    private static final int W = 1024;
    private static final int H = 640;

    private final AtomicLong now = new AtomicLong(0);

    private static MapView map(String fill, List<PlotView> special, List<BuildingView> buildings) {
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 16; col++) {
                int c = col;
                int r = row;
                plots.add(special.stream().filter(p -> p.col() == c && p.row() == r).findFirst()
                        .orElse(new PlotView(col, row, fill, null, null, false, 0)));
            }
        }
        return new MapView(16, 10, plots, buildings);
    }

    private static BufferedImage paint(MapRenderer renderer, MapView map, VisualEffects fx) {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        renderer.render(g, map, W, H, null, fx);
        g.dispose();
        return image;
    }

    private static int[] pixels(BufferedImage image, Rectangle r) {
        return image.getRGB(r.x, r.y, r.width, r.height, null, 0, r.width);
    }

    private static Rectangle cell(int col, int row) {
        return MapGeometry.fit(16, 10, W, H).cellBounds(col, row);
    }

    @Test
    void HU48_ca6_noGridLineIsDrawnBetweenTwoPlots() {
        BufferedImage vector = paint(new MapRenderer(AssetManager.vectorOnly()), map("GRASS", List.of(), List.of()), null);
        Rectangle left = cell(2, 5);
        for (int y = left.y; y < left.y + left.height; y++) {
            assertEquals(Palette.GRASS.getRGB(), vector.getRGB(left.x + left.width - 1, y), "seam at y " + y);
            assertEquals(Palette.GRASS.getRGB(), vector.getRGB(left.x + left.width, y), "seam at y " + y);
        }
        BufferedImage art = paint(new MapRenderer(), map("GRASS", List.of(), List.of()), null);
        int seamX = left.x + left.width;
        long seam = column(art, seamX, left);
        long darkest = Long.MAX_VALUE;
        for (int x = left.x + 2; x < seamX + left.width - 2; x++) {
            if (Math.abs(x - seamX) > 1) {
                darkest = Math.min(darkest, column(art, x, left));
            }
        }
        assertTrue(seam * 100 >= darkest * 97, "the seam (" + seam + ") is no darker than the grass itself (" + darkest + ")");
    }

    @Test
    void HU48_ca6_soilBlendsIntoTheGrassWithARaggedEdge() throws IOException {
        BufferedImage soil = ImageIO.read(getClass().getResource("/assets/terrain/SOIL.png"));
        MapView field = map("GRASS", List.of(new PlotView(5, 3, "SOIL", null, null, false, 0)), List.of());
        BufferedImage image = paint(new MapRenderer(), field, null);
        Rectangle c = cell(5, 3);

        int covered = 0;
        for (int x = 0; x < 32; x++) {
            if (image.getRGB(c.x + 2 * x, c.y) != soil.getRGB(x, 0)) {
                covered++;
            }
        }
        assertEquals(soil.getRGB(16, 16), image.getRGB(c.x + 32, c.y + 32), "the middle is plain soil");
        assertEquals(32, covered, "the grass covers the whole top edge");
        Rectangle below = new Rectangle(c.x, c.y + 4 * 2, c.width, 2);
        int[] row = pixels(image, below);
        assertTrue(Arrays.stream(row).distinct().count() > 2, "the edge is ragged, not a straight line");
    }

    @Test
    void HU48_ca6_treesRiseAboveTheirPlotAndCastAShadow() {
        List<PlotView> trees = new ArrayList<>();
        for (int col = 2; col < 14; col += 3) {
            trees.add(new PlotView(col, 6, "OBSTACLE", null, null, false, 0));
        }
        BufferedImage bare = paint(new MapRenderer(), map("GRASS", List.of(), List.of()), null);
        BufferedImage wood = paint(new MapRenderer(), map("GRASS", trees, List.of()), null);

        boolean rises = false;
        for (PlotView tree : trees) {
            rises |= !Arrays.equals(pixels(bare, cell(tree.col(), 5)), pixels(wood, cell(tree.col(), 5)));
        }
        assertTrue(rises, "a tree covers part of the plot above it");
    }

    @Test
    void HU48_ca6_aBuildingThatHidesACropTurnsSeeThrough() {
        BuildingView coop = new BuildingView(0, "COOP", "Gallinero", 2, 7, 2, 2, 4, "Huevo", 0, List.<AnimalView>of());
        PlotView behind = new PlotView(2, 6, "SOIL", "CORN", "MATURE", false, 0);
        BufferedImage without = paint(new MapRenderer(), map("GRASS", List.of(new PlotView(2, 6, "SOIL", null, null, false, 0)),
                List.of(coop)), null);
        BufferedImage with = paint(new MapRenderer(), map("GRASS", List.of(behind), List.of(coop)), null);
        Rectangle c = cell(2, 6);
        Rectangle lower = new Rectangle(c.x + c.width / 4, c.y + c.height / 2, c.width / 2, c.height / 2);

        assertFalse(Arrays.equals(pixels(without, lower), pixels(with, lower)), "the crop shows through the roof");
    }

    @Test
    void HU48_ca6_theCameraNeverPutsTheAvatarNorTheEdgeOfTheValleyUnderAFloatingBar() {
        TestApp app = TestApp.startedGame();
        MapPanel panel = new MapPanel(app.controller, app.bus, (col, row) -> { });
        JPanel bar = new JPanel();
        JPanel stage = new JPanel(null);
        stage.add(bar);
        stage.add(panel);
        stage.setBounds(0, 0, W, 760);
        panel.setBounds(0, 0, W, 760);
        bar.setBounds(0, 620, W, 140);
        assertTrue(panel.geometry().area().intersects(bar.getBounds()));

        panel.addFloating(bar);

        for (int row : new int[] {7, 22, 23}) {
            app.state().avatar().restoreFrom(new edu.ucc.farmsim.domain.farm.AvatarSnapshot(7, row,
                    edu.ucc.farmsim.domain.farm.Direction.SOUTH));
            panel.reload();
            Rectangle avatar = panel.geometry().cellBounds(7, row);
            assertFalse(avatar.intersects(bar.getBounds()), avatar + " under " + bar.getBounds());
        }
        Rectangle area = panel.geometry().area();
        assertTrue(area.y + area.height <= bar.getY(), "the last row of the valley stays above the bar: " + area);
        assertEquals(0, panel.geometry().cellAt(area.x + 1, area.y + area.height - 1).col());
    }

    @Test
    void HU48_ca7_withTheAmbientLifeOnTheWaterMovesAndOffEverythingStaysStill() {
        MapView pond = map("WATER", List.of(), List.of());
        VisualEffects fx = new VisualEffects(now::get, new Random(5));
        fx.onWorld("SPRING", "SUNNY");
        MapRenderer renderer = new MapRenderer();
        BufferedImage first = paint(renderer, pond, fx);
        now.set(500);
        assertTrue(Arrays.equals(pixels(first, cell(5, 5)), pixels(paint(renderer, pond, fx), cell(5, 5))),
                "without ambient life nothing moves");
        assertFalse(fx.needsFrames());

        fx.setAmbient(true);
        now.set(0);
        int[] still = pixels(paint(renderer, pond, fx), cell(5, 5));
        now.set(500);
        int[] later = pixels(paint(renderer, pond, fx), cell(5, 5));

        assertFalse(Arrays.equals(still, later), "the water ripples");
        assertTrue(fx.needsFrames());
        fx.setEnabled(false);
        assertFalse(fx.isAmbient());
        assertFalse(fx.needsFrames());
    }

    @Test
    void HU48_ca7_aPanelThatIsNotOnScreenRunsNoTimerForTheAmbientLife() {
        TestApp app = TestApp.startedGame();
        MapPanel panel = new MapPanel(app.controller, app.bus, (col, row) -> { });

        assertTrue(panel.effects().isAmbient());
        assertFalse(panel.isAnimating());
    }

    @Test
    void HU48_ca8_theStartSceneMovesAndIsDrawnEvenWithoutSprites() {
        for (AssetManager assets : List.of(AssetManager.fromClasspath(), AssetManager.vectorOnly())) {
            TitleScene scene = new TitleScene(assets);
            BufferedImage a = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
            BufferedImage b = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
            scene.paint(a.createGraphics(), 800, 500, 1_000);
            scene.paint(b.createGraphics(), 800, 500, 4_000);

            assertFalse(Arrays.equals(a.getRGB(0, 0, 800, 500, null, 0, 800), b.getRGB(0, 0, 800, 500, null, 0, 800)));
            assertEquals(0xFF, a.getRGB(400, 480) >>> 24, "the whole area is painted");
        }
    }

    private static long column(BufferedImage image, int x, Rectangle rows) {
        long sum = 0;
        for (int y = rows.y; y < rows.y + rows.height; y++) {
            sum += brightness(image.getRGB(x, y));
        }
        return sum;
    }

    private static long brightness(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }
}
