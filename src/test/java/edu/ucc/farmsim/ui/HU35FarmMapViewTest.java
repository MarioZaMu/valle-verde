package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.render.CellPos;
import edu.ucc.farmsim.ui.render.MapGeometry;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.render.MapRenderer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/**
 * HU-35: the farm map drawn with Java2D. Geometry and drawing are separate classes with no component, so they
 * are tested by painting on an image. Buildings and animals are drawn over the plots (HU-35 · CA-3).
 */
class HU35FarmMapViewTest {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 600;

    private static BufferedImage paint(MapView map, CellPos hover) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        new MapRenderer().render(g, map, WIDTH, HEIGHT, hover);
        g.dispose();
        return image;
    }

    private static int[] cellPixels(BufferedImage image, MapGeometry geometry, int col, int row) {
        var b = geometry.cellBounds(col, row);
        int[] pixels = new int[b.width * b.height];
        image.getRGB(b.x, b.y, b.width, b.height, pixels, 0, b.width);
        return pixels;
    }

    private static int centerColor(BufferedImage image, MapGeometry geometry, int col, int row) {
        var b = geometry.cellBounds(col, row);
        return image.getRGB(b.x + 2, b.y + 2, 1, 1, null, 0, 1)[0];
    }

    private static MapView mapWith(PlotView plot) {
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 16; col++) {
                plots.add(col == 5 && row == 3 ? plot : new PlotView(col, row, "SOIL", null, null, false, 0));
            }
        }
        return new MapView(16, 10, plots);
    }

    @Test
    void HU35_ca1_theMapShows16x10PlotsAndTheFourTerrainsLookDifferent() {
        // Given a new game
        TestApp app = TestApp.startedGame();
        MapView map = app.controller.map().value();
        assertEquals(160, map.plots().size());
        MapGeometry geometry = MapGeometry.fit(16, 10, WIDTH, HEIGHT);

        // When it is drawn
        BufferedImage image = paint(map, null);

        // Then soil, grass, water and obstacles use different colors (corner pixel of each cell)
        Set<Integer> colors = new HashSet<>();
        colors.add(centerColor(image, geometry, GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW)); // soil
        colors.add(centerColor(image, geometry, 1, 1));   // grass
        colors.add(centerColor(image, geometry, 13, 0));  // water
        colors.add(centerColor(image, geometry, 0, 0));   // obstacle
        assertEquals(4, colors.size(), "four terrains must look different");
    }

    @Test
    void HU35_ca2_everyCropStageHasADifferentAppearanceFromTheOthersAndFromBareSoil() {
        MapGeometry geometry = MapGeometry.fit(16, 10, WIDTH, HEIGHT);
        List<int[]> looks = new ArrayList<>();
        looks.add(cellPixels(paint(mapWith(new PlotView(5, 3, "SOIL", null, null, false, 0)), null), geometry, 5, 3));
        for (String stage : new String[] {"SEED", "SPROUT", "GROWING", "MATURE", "WITHERED"}) {
            PlotView plot = new PlotView(5, 3, "SOIL", "CARROT", stage, false, 2);
            looks.add(cellPixels(paint(mapWith(plot), null), geometry, 5, 3));
        }
        for (int i = 0; i < looks.size(); i++) {
            for (int j = i + 1; j < looks.size(); j++) {
                assertFalse(Arrays.equals(looks.get(i), looks.get(j)), "appearances " + i + " and " + j + " are equal");
            }
        }
    }

    @Test
    void HU35_ca2_aWateredCropLooksDifferentFromADryOne() {
        MapGeometry geometry = MapGeometry.fit(16, 10, WIDTH, HEIGHT);
        int[] dry = cellPixels(paint(mapWith(new PlotView(5, 3, "SOIL", "CARROT", "SPROUT", false, 3)), null), geometry, 5, 3);
        int[] wet = cellPixels(paint(mapWith(new PlotView(5, 3, "SOIL", "CARROT", "SPROUT", true, 3)), null), geometry, 5, 3);

        assertFalse(Arrays.equals(dry, wet));
    }

    @Test
    void HU35_ca2_differentCropsHaveADifferentMatureColor() {
        MapGeometry geometry = MapGeometry.fit(16, 10, WIDTH, HEIGHT);
        int[] carrot = cellPixels(paint(mapWith(new PlotView(5, 3, "SOIL", "CARROT", "MATURE", false, 0)), null), geometry, 5, 3);
        int[] tomato = cellPixels(paint(mapWith(new PlotView(5, 3, "SOIL", "TOMATO", "MATURE", false, 0)), null), geometry, 5, 3);

        assertFalse(Arrays.equals(carrot, tomato));
    }

    @Test
    void HU35_theHoveredPlotIsHighlighted() {
        MapView map = TestApp.startedGame().controller.map().value();
        MapGeometry geometry = MapGeometry.fit(16, 10, WIDTH, HEIGHT);

        int[] plain = cellPixels(paint(map, null), geometry, 5, 3);
        int[] hovered = cellPixels(paint(map, new CellPos(5, 3)), geometry, 5, 3);

        assertFalse(Arrays.equals(plain, hovered));
    }

    @Test
    void HU35_ca4_clicksOutsideTheMapAreaMapToNoPlot() {
        MapGeometry geometry = MapGeometry.fit(16, 10, WIDTH, HEIGHT);
        var first = geometry.cellBounds(0, 0);
        var last = geometry.cellBounds(15, 9);

        assertEquals(new CellPos(0, 0), geometry.cellAt(first.x + 1, first.y + 1));
        assertEquals(new CellPos(15, 9), geometry.cellAt(last.x + last.width - 1, last.y + last.height - 1));
        assertEquals(new CellPos(5, 3), geometry.cellAt(geometry.cellBounds(5, 3).x + 3, geometry.cellBounds(5, 3).y + 3));
        assertNull(geometry.cellAt(-5, -5));
        assertNull(geometry.cellAt(first.x - 1, first.y + 5));
        assertNull(geometry.cellAt(last.x + last.width + 1, last.y + 5));
        assertNull(geometry.cellAt(WIDTH + 50, HEIGHT + 50));
    }

    @Test
    void HU35_ca4_aClickOutsideTheMapOnThePanelDoesNothing() {
        TestApp app = TestApp.startedGame();
        List<CellPos> clicks = new ArrayList<>();
        MapPanel panel = new MapPanel(app.controller, app.bus, (col, row) -> clicks.add(new CellPos(col, row)));
        panel.setSize(WIDTH, HEIGHT);

        panel.handleClick(-10, -10);
        assertTrue(clicks.isEmpty());

        var inside = MapGeometry.fit(16, 10, WIDTH, HEIGHT).cellBounds(5, 3);
        panel.handleClick(inside.x + 4, inside.y + 4);
        assertEquals(List.of(new CellPos(5, 3)), clicks);
    }

    @Test
    void HU35_ca5_theMapIsLegibleFrom1024x768WithCellsOfAtLeast40Pixels() {
        // The map area of a 1024 x 768 window is about 1024 x 600 once the top bar and buttons are taken out
        MapGeometry geometry = MapGeometry.fit(16, 10, 1024, 600);

        assertTrue(geometry.cellSize() >= 40, "cell size " + geometry.cellSize());
        var last = geometry.cellBounds(15, 9);
        assertTrue(last.x + last.width <= 1024 && last.y + last.height <= 600, "the whole map fits without clipping");
    }

    @Test
    void HU35_ca5_thePanelRedrawsWhenAMapEventArrivesAndNeverUsesATimerToPoll() throws Exception {
        // Given a panel showing the map of a new game
        TestApp app = TestApp.startedGame();
        MapPanel panel = new MapPanel(app.controller, app.bus, (col, row) -> { });
        SwingUtilities.invokeAndWait(() -> { });
        assertNull(panel.currentView().plotAt(5, 3).crop());

        // When a crop is sown (the controller publishes MapChanged)
        app.controller.plant(5, 3, "CARROT_SEED");
        SwingUtilities.invokeAndWait(() -> { });

        // Then the panel already holds the new view, without any polling
        assertNotNull(panel.currentView().plotAt(5, 3).crop());
        for (Field field : MapPanel.class.getDeclaredFields()) {
            assertNotEquals(javax.swing.Timer.class, field.getType(), "the map must be redrawn by events, not by a timer");
        }
    }

    @Test
    void HU07_ca1_theTooltipAppearsAfter300Milliseconds() {
        assertEquals(300, MapPanel.TOOLTIP_DELAY_MS);
    }

    @Test
    void HU35_geometryRejectsInvalidSizes() {
        assertEquals(1, MapGeometry.fit(16, 10, 0, 0).cellSize());
    }

    @Test
    void HU35_ca3_aCoopWithTwoHensIsDrawnInItsPositionAndTheHensChangeWhatIsSeen() {
        // Given a coop built at (2,7), which covers (2,7) to (3,8)
        TestApp app = TestApp.startedGame();
        var coop = GameFixtures.coop(app.state());
        MapGeometry geometry = MapGeometry.fit(16, 10, WIDTH, HEIGHT);
        MapView empty = app.controller.map().value();
        MapView noBuildings = new MapView(empty.cols(), empty.rows(), empty.plots());
        int[] grass = cellPixels(paint(noBuildings, null), geometry, 2, 8);
        int[] coopEmpty = cellPixels(paint(empty, null), geometry, 2, 8);

        // When two hens move in
        GameFixtures.hen(coop);
        GameFixtures.hen(coop);
        MapView withHens = app.controller.map().value();
        int[] coopWithHens = cellPixels(paint(withHens, null), geometry, 2, 8);

        // Then the building replaces the grass, the hens change its look and the plot next to it is untouched
        assertEquals(1, empty.buildings().size());
        assertEquals(2, withHens.buildingAt(3, 8).animals().size());
        assertNotEquals(Arrays.hashCode(grass), Arrays.hashCode(coopEmpty));
        assertNotEquals(Arrays.hashCode(coopEmpty), Arrays.hashCode(coopWithHens));
        assertEquals(Arrays.hashCode(cellPixels(paint(noBuildings, null), geometry, 4, 8)),
                Arrays.hashCode(cellPixels(paint(withHens, null), geometry, 4, 8)));
    }

    @Test
    void HU35_ca3_aSickHenAndTheProductsWaitingLookDifferentFromAHealthyOne() {
        MapGeometry geometry = MapGeometry.fit(16, 10, WIDTH, HEIGHT);
        int[] healthy = coopCell(edu.ucc.farmsim.domain.animal.AnimalCondition.HEALTHY, 0, geometry, 2, 8);
        int[] sick = coopCell(edu.ucc.farmsim.domain.animal.AnimalCondition.SICK, 0, geometry, 2, 8);
        int[] hungry = coopCell(edu.ucc.farmsim.domain.animal.AnimalCondition.HUNGRY, 0, geometry, 2, 8);
        int[] noProducts = coopCell(edu.ucc.farmsim.domain.animal.AnimalCondition.HEALTHY, 0, geometry, 3, 7);
        int[] products = coopCell(edu.ucc.farmsim.domain.animal.AnimalCondition.HEALTHY, 1, geometry, 3, 7);

        assertNotEquals(Arrays.hashCode(healthy), Arrays.hashCode(sick));
        assertNotEquals(Arrays.hashCode(healthy), Arrays.hashCode(hungry));
        assertNotEquals(Arrays.hashCode(noProducts), Arrays.hashCode(products));
    }

    private static int[] coopCell(edu.ucc.farmsim.domain.animal.AnimalCondition condition, int pending,
                                  MapGeometry geometry, int col, int row) {
        TestApp app = TestApp.startedGame();
        GameFixtures.house(GameFixtures.coop(app.state()), edu.ucc.farmsim.domain.animal.AnimalType.HEN, condition, 0,
                false, pending);
        return cellPixels(paint(app.controller.map().value(), null), geometry, col, row);
    }

    @Test
    void HU35_everyKindOfBuildingHasItsOwnLook() {
        MapGeometry geometry = MapGeometry.fit(16, 10, WIDTH, HEIGHT);
        Set<Integer> looks = new HashSet<>();
        for (var type : edu.ucc.farmsim.domain.building.BuildingType.values()) {
            TestApp app = TestApp.startedGame();
            GameFixtures.build(app.state(), type, 2, 7);
            looks.add(Arrays.hashCode(cellPixels(paint(app.controller.map().value(), null), geometry, 2, 7)));
        }
        assertEquals(4, looks.size());
    }

    @Test
    void HU35_theTooltipOfABuildingTellsItsAnimalsAndWhatWaits() {
        TestApp app = TestApp.startedGame();
        var coop = GameFixtures.coop(app.state());
        GameFixtures.house(coop, edu.ucc.farmsim.domain.animal.AnimalType.HEN,
                edu.ucc.farmsim.domain.animal.AnimalCondition.HEALTHY, 0, false, 1);

        String text = edu.ucc.farmsim.ui.components.PlotText.describe(app.controller.buildings().value().buildings().get(0));

        assertEquals("Gallinero 1 · 1/4 animales · 1 por recolectar", text);
    }
}
