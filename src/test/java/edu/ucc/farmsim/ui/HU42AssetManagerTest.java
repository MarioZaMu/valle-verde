package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
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
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** HU-42: sprites are loaded once through the {@link AssetManager}, and a missing one falls back to vectors. */
class HU42AssetManagerTest {

    private static final int RED = 0xFFFF0000;

    private static byte[] png(int color) throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                image.setRGB(x, y, color);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static MapView farm() {
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 16; col++) {
                plots.add(col == 5 && row == 3
                        ? new PlotView(col, row, "SOIL", "CARROT", "MATURE", false, 0)
                        : new PlotView(col, row, "SOIL", null, null, false, 0));
            }
        }
        return new MapView(16, 10, plots, List.<BuildingView>of());
    }

    private static BufferedImage paint(MapRenderer renderer) {
        BufferedImage image = new BufferedImage(1024, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        renderer.render(g, farm(), 1024, 600, null);
        g.dispose();
        return image;
    }

    @Test
    void HU42_ca1_theSameSpriteAskedTwiceIsReadOnceAndIsTheSameObject() throws IOException {
        int[] reads = {0};
        byte[] data = png(RED);
        AssetManager assets = new AssetManager(path -> {
            reads[0]++;
            return new ByteArrayInputStream(data);
        });

        Optional<BufferedImage> first = assets.sprite("crops/CARROT_MATURE.png");
        Optional<BufferedImage> second = assets.sprite("crops/CARROT_MATURE.png");

        assertTrue(first.isPresent());
        assertSame(first.get(), second.get());
        assertEquals(1, reads[0]);
        assertEquals(1, assets.reads());
    }

    @Test
    void HU42_ca2_aMissingFileGivesNoSpriteNoExceptionAndOneWarning() {
        List<LogRecord> logged = new ArrayList<>();
        Logger logger = Logger.getLogger(AssetManager.class.getName());
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() { }

            @Override
            public void close() { }
        };
        logger.addHandler(handler);
        try {
            AssetManager assets = new AssetManager(path -> {
                throw new FileNotFoundException(path);
            });

            assertTrue(assets.sprite("animals/HEN_0.png").isEmpty());
            assertTrue(assets.sprite("animals/HEN_0.png").isEmpty());

            assertEquals(1, logged.stream().filter(r -> r.getLevel() == Level.WARNING).count());
            assertEquals(1, assets.reads());
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void HU42_ca2_anUnreadableImageAlsoFallsBack() {
        AssetManager assets = new AssetManager(path -> new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertTrue(assets.sprite("crops/broken.png").isEmpty());
    }

    @Test
    void HU42_ca2_aMissingSpriteDrawsTheVectorGraphicInstead() {
        AssetManager nothing = new AssetManager(path -> {
            throw new FileNotFoundException(path);
        });

        BufferedImage withFallback = paint(new MapRenderer(nothing));
        BufferedImage vectors = paint(new MapRenderer(AssetManager.vectorOnly()));

        assertEquals(pixelsOf(vectors), pixelsOf(withFallback));
    }

    @Test
    void HU42_ca3_withoutAnyImageTheGameIsDrawnCompletelyWithVectors() {
        BufferedImage image = paint(new MapRenderer(AssetManager.vectorOnly()));

        var geometry = MapGeometry.fit(16, 10, 1024, 600);
        var cell = geometry.cellBounds(5, 3);
        int[] soil = new int[cell.width * cell.height];
        image.getRGB(cell.x, cell.y, cell.width, cell.height, soil, 0, cell.width);
        boolean hasFruit = false;
        for (int pixel : soil) {
            hasFruit |= pixel == (0xFF000000 | 0xF28C28);
        }
        assertTrue(hasFruit, "the carrot is drawn with vectors");
    }

    @Test
    void HU42_aSpriteThatExistsReplacesTheVectorGraphic() throws IOException {
        byte[] data = png(RED);
        AssetManager assets = new AssetManager(path -> {
            if (!path.equals("crops/CARROT_MATURE.png")) {
                throw new FileNotFoundException(path);
            }
            return new ByteArrayInputStream(data);
        });

        BufferedImage image = paint(new MapRenderer(assets));

        var cell = MapGeometry.fit(16, 10, 1024, 600).cellBounds(5, 3);
        assertEquals(RED, image.getRGB(cell.x + cell.width / 2, cell.y + cell.height / 2));
    }

    @Test
    void HU42_theClasspathLoaderReadsTheResourcesFolderAndSurvivesWhenItIsEmpty() {
        AssetManager assets = AssetManager.fromClasspath();

        assertTrue(assets.sprite("does/not/exist.png").isEmpty());
        assertEquals(1, assets.reads());
    }

    @Test
    void HU42_ca4_everyImagePackageInTheResourcesIsListedInAssetsMd() throws IOException {
        Path assetsDir = Path.of("src/main/resources/assets");
        String listed = Files.readString(Path.of("docs/ASSETS.md"));

        assertTrue(listed.contains("Licencia"), "ASSETS.md explains the licences");
        if (!Files.isDirectory(assetsDir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(assetsDir)) {
            List<Path> images = files.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .toList();
            for (Path image : images) {
                String relative = assetsDir.relativize(image).toString().replace('\\', '/');
                assertTrue(listed.contains(relative) || listed.contains(relative.split("/")[0] + "/"),
                        "not listed in docs/ASSETS.md: " + relative);
            }
            assertFalse(images.isEmpty() && !listed.contains("Ningún"), "ASSETS.md says when there are none");
        }
    }

    private static List<Integer> pixelsOf(BufferedImage image) {
        List<Integer> pixels = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y += 7) {
            for (int x = 0; x < image.getWidth(); x += 7) {
                pixels.add(image.getRGB(x, y));
            }
        }
        return pixels;
    }
}
