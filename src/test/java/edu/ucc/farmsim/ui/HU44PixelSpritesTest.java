package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.render.MapGeometry;
import edu.ucc.farmsim.ui.render.MapRenderer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** HU-44: our own 32 x 32 pixel-art sprites, drawn sharp at integer scales. */
class HU44PixelSpritesTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets");
    /** Tall sprites: buildings have one tile of roof above the ground they use, trees are two tiles high. */
    private static final Map<String, int[]> SHEETS = Map.of(
            "buildings/COOP.png", new int[] {64, 96}, "buildings/BARN.png", new int[] {96, 96},
            "buildings/WAREHOUSE.png", new int[] {64, 96}, "buildings/GREENHOUSE.png", new int[] {96, 128});

    private static int[] sheet(String name) {
        if (name.startsWith("terrain/TREE_") || name.startsWith("terrain/PINE_")) {
            return new int[] {64, 64};
        }
        if (name.startsWith("characters/")) {
            return new int[] {32, 64};
        }
        if (name.startsWith("village/")) {
            return name.equals("village/SHOP.png") || name.equals("village/CLINIC.png") ? new int[] {160, 128}
                    : new int[] {128, 128};
        }
        return SHEETS.get(name);
    }

    private static List<Path> images() throws IOException {
        try (Stream<Path> files = Files.walk(ASSETS)) {
            return files.filter(p -> p.toString().endsWith(".png")).toList();
        }
    }

    private static String rel(Path p) {
        return ASSETS.relativize(p).toString().replace('\\', '/');
    }

    @Test
    void HU44_ca1_everyImageIs32x32AndTheBuildingSheetsAreWholeTiles() throws IOException {
        for (Path path : images()) {
            BufferedImage image = ImageIO.read(path.toFile());
            int[] sheet = sheet(rel(path));
            if (sheet == null) {
                assertEquals(32, image.getWidth(), rel(path));
                assertEquals(32, image.getHeight(), rel(path));
            } else {
                assertEquals(sheet[0], image.getWidth(), rel(path));
                assertEquals(sheet[1], image.getHeight(), rel(path));
            }
        }
    }

    @Test
    void HU44_ca2_thePackCoversTerrainsCropsAnimalsBuildingsItemsCharactersAndIcons() throws IOException {
        Set<String> have = new HashSet<>();
        for (Path path : images()) {
            have.add(rel(path));
        }
        List<String> need = new ArrayList<>(List.of("terrain/SOIL.png", "terrain/SOIL_WATERED.png", "terrain/WATER.png",
                "terrain/OBSTACLE.png", "buildings/COOP.png", "buildings/BARN.png", "buildings/WAREHOUSE.png",
                "buildings/GREENHOUSE.png", "icons/COIN.png", "icons/ENERGY.png", "terrain/PATH.png",
                "village/HOME.png", "village/SHOP.png", "village/FARMHOUSE.png", "village/CLINIC.png",
                "village/COTTAGE.png"));
        for (String season : List.of("SPRING", "SUMMER", "AUTUMN", "WINTER")) {
            need.add("terrain/GRASS_" + season + ".png");
            need.add("icons/" + season + ".png");
        }
        for (String crop : List.of("CARROT", "POTATO", "TOMATO", "CORN", "PUMPKIN")) {
            for (String stage : List.of("SEED", "SPROUT", "GROWING", "MATURE", "WITHERED")) {
                need.add("crops/" + crop + "_" + stage + ".png");
            }
        }
        for (String animal : List.of("HEN", "COW")) {
            for (int frame = 0; frame < 3; frame++) {
                need.add("animals/" + animal + "_" + frame + ".png");
            }
        }
        for (ItemType item : ItemType.values()) {
            need.add("items/" + item.name() + ".png");
        }
        for (String direction : List.of("S", "N", "E", "W")) {
            for (int frame = 0; frame < 3; frame++) {
                need.add("characters/AVATAR_" + direction + "_" + frame + ".png");
            }
        }
        for (String npc : List.of("MARTA", "TOMAS", "LUCIA", "INES")) {
            need.add("characters/" + npc + ".png");
        }
        for (String name : List.of("SOW", "WATER", "HARVEST", "BUILD", "FEED", "SUNNY", "CLOUDY", "RAINY", "DROUGHT",
                "FROST", "SNOWY")) {
            need.add("icons/" + name + ".png");
        }
        for (String file : need) {
            assertTrue(have.contains(file), "missing sprite " + file);
        }
    }

    @Test
    void HU44_ca2_theMapDrawsTheRealPackInsteadOfTheVectors() throws IOException {
        BufferedImage image = paint(new MapRenderer(), soilMap());
        BufferedImage sprite = ImageIO.read(ASSETS.resolve("terrain/SOIL.png").toFile());
        var cell = MapGeometry.fit(16, 10, 1024, 640).cellBounds(2, 2);

        assertEquals(64, cell.width);
        assertEquals(sprite.getRGB(5, 5), image.getRGB(cell.x + 10, cell.y + 10));
        assertEquals(sprite.getRGB(20, 9), image.getRGB(cell.x + 40, cell.y + 18));
    }

    @Test
    void HU44_ca3_theCellSnapsToAWholeScaleWhenItWastesLittleAndAScaleOf2FitsExactly() {
        assertEquals(64, MapGeometry.fit(16, 10, 1024, 640).cellSize());
        assertEquals(64, MapGeometry.fit(16, 10, 1100, 700).cellSize());
        assertEquals(96, MapGeometry.fit(16, 10, 1536, 960).cellSize());
        assertEquals(61, MapGeometry.fit(16, 10, 1024, 618).cellSize(), "a whole scale would waste too much");
        assertEquals(32, MapGeometry.fit(16, 10, 512, 320).cellSize());
        assertEquals(20, MapGeometry.fit(16, 10, 320, 200).cellSize(), "smaller than one tile: any scale");
    }

    @Test
    void HU44_ca3_aSpriteScaledByAWholeFactorKeepsOnlyItsOwnColours() throws IOException {
        BufferedImage half = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                half.setRGB(x, y, x < 16 ? 0xFFFF0000 : 0xFF0000FF);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(half, "png", out);
        AssetManager assets = new AssetManager(name -> name.equals("terrain/SOIL.png")
                ? new ByteArrayInputStream(out.toByteArray()) : failing(name));

        BufferedImage image = paint(new MapRenderer(assets), soilMap());

        var cell = MapGeometry.fit(16, 10, 1024, 640).cellBounds(3, 3);
        Set<Integer> colours = new HashSet<>();
        for (int y = 1; y < cell.height - 1; y++) {
            for (int x = 1; x < cell.width - 1; x++) {
                colours.add(image.getRGB(cell.x + x, cell.y + y));
            }
        }
        assertEquals(Set.of(0xFFFF0000, 0xFF0000FF), colours);
    }

    @Test
    void HU44_ca4_aMissingSpriteStillDrawsTheVectorGraphic() {
        BufferedImage image = paint(new MapRenderer(AssetManager.vectorOnly()), soilMap());
        var cell = MapGeometry.fit(16, 10, 1024, 640).cellBounds(2, 2);

        assertEquals(0xFF8B5A2B, image.getRGB(cell.x + 2, cell.y + 2));
    }

    @Test
    void HU44_ca5_theScriptAndTheLicenceFileExistAndEveryFolderIsListed() throws IOException {
        assertTrue(Files.exists(Path.of("tools/gen_sprites.py")));
        String listed = Files.readString(Path.of("docs/ASSETS.md"));
        for (String folder : List.of("terrain/", "crops/", "animals/", "buildings/", "items/", "characters/", "icons/")) {
            assertTrue(listed.contains(folder), "ASSETS.md must list " + folder);
        }
        assertTrue(listed.contains("trabajo propio"));
        assertTrue(listed.contains("gen_sprites.py"));
    }

    private static java.io.InputStream failing(String name) throws FileNotFoundException {
        throw new FileNotFoundException(name);
    }

    private static MapView soilMap() {
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 16; col++) {
                plots.add(new PlotView(col, row, "SOIL", null, null, false, 0));
            }
        }
        return new MapView(16, 10, plots, List.of());
    }

    private static BufferedImage paint(MapRenderer renderer, MapView map) {
        BufferedImage image = new BufferedImage(1024, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        renderer.render(g, map, 1024, 640, null);
        g.dispose();
        return image;
    }
}
