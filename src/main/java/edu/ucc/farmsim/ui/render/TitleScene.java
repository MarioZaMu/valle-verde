package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.ui.assets.AssetManager;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * The living picture behind the start screen: a spring morning over the valley, with a sky that fades to the
 * horizon, clouds and two rows of hills that drift at different speeds, birds, and at the front a strip of
 * farm with its fence, trees, barn, coop and crops, where hens peck and the farmer walks by. Every position is
 * a function of the time, so it needs no state and is drawn the same way on any {@link Graphics2D}.
 *
 * <p>Stories: HU-33, HU-48.
 */
public final class TitleScene {

    private static final Color SKY_TOP = new Color(0x4FA8E8);
    private static final Color SKY_LOW = new Color(0xD4F0FF);
    private static final Color SUN = new Color(0xFFF2B0);
    private static final Color CLOUD = new Color(0xFFFFFF);
    private static final Color CLOUD_SHADE = new Color(0xD6E8F5);
    private static final Color HILL_FAR = new Color(0x9CCB7A);
    private static final Color HILL_NEAR = new Color(0x6DAE4E);
    private static final Color BIRD = new Color(0x2E3A4A);
    private static final Color FADE = new Color(0x1A1008);
    private static final int FADE_MS = 900;
    private static final int CLOUDS = 6;
    private static final int BIRDS = 4;
    private static final int HENS = 3;

    private final AssetManager assets;
    private final PixelScaler scaler = new PixelScaler();

    /**
     * Creates the scene.
     *
     * @param assets where the sprites come from; without them the land is drawn with plain shapes
     */
    public TitleScene(AssetManager assets) {
        this.assets = assets;
    }

    /**
     * Paints the scene.
     *
     * @param g      where to paint
     * @param width  width of the area
     * @param height height of the area
     * @param time   milliseconds since the scene appeared
     */
    public void paint(Graphics2D g, int width, int height, long time) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int horizon = height * 53 / 100;
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, horizon, SKY_LOW));
        g.fillRect(0, 0, width, height);
        sun(g, width, height);
        clouds(g, width, height, time);
        birds(g, width, height, time);
        hill(g, width, height, horizon - height / 9, HILL_FAR, 210.0, height / 26.0, time / 90.0);
        hill(g, width, height, horizon, HILL_NEAR, 140.0, height / 30.0, time / 45.0);
        land(g, width, height, horizon, time);
        if (time < FADE_MS) {
            int alpha = (int) Math.round(255 * (1 - time / (double) FADE_MS));
            g.setColor(new Color(FADE.getRed(), FADE.getGreen(), FADE.getBlue(), Math.max(0, Math.min(255, alpha))));
            g.fillRect(0, 0, width, height);
        }
    }

    private static void sun(Graphics2D g, int width, int height) {
        int r = Math.max(20, height / 14);
        int cx = width * 80 / 100;
        int cy = height * 16 / 100;
        for (int ring = 3; ring >= 1; ring--) {
            g.setColor(new Color(255, 245, 190, 40));
            int rr = r + ring * r / 2;
            g.fillOval(cx - rr, cy - rr, 2 * rr, 2 * rr);
        }
        g.setColor(SUN);
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    private static void clouds(Graphics2D g, int width, int height, long time) {
        int unit = Math.max(6, height / 90);
        int span = width + 40 * unit;
        for (int i = 0; i < CLOUDS; i++) {
            double speed = unit * (0.6 + (i % 3) * 0.35);
            int x = (int) Math.floorMod((long) (time / 1000.0 * speed + i * span / (double) CLOUDS), (long) span) - 20 * unit;
            int y = height * (6 + (i * 37) % 30) / 100;
            int size = 3 + i % 3;
            g.setColor(CLOUD_SHADE);
            g.fillRoundRect(x, y + unit * 2, unit * size * 5, unit * 3, unit * 2, unit * 2);
            g.setColor(CLOUD);
            g.fillRoundRect(x + unit, y + unit, unit * size * 5 - 2 * unit, unit * 3, unit * 2, unit * 2);
            g.fillRoundRect(x + unit * size, y - unit, unit * size * 2, unit * 3, unit * 2, unit * 2);
            g.fillRoundRect(x + unit * size * 2, y - unit * 2, unit * size * 2, unit * 3, unit * 2, unit * 2);
        }
    }

    private static void birds(Graphics2D g, int width, int height, long time) {
        g.setColor(BIRD);
        int wing = Math.max(4, height / 120);
        for (int i = 0; i < BIRDS; i++) {
            int span = width + 200;
            int x = (int) Math.floorMod(time / 18 + i * 140L, (long) span) - 100;
            int y = height * 22 / 100 + i * wing * 3 + (int) (Math.sin(time / 400.0 + i) * wing);
            boolean up = ((time / 180) + i) % 2 == 0;
            int tip = up ? -wing : wing / 2;
            g.drawLine(x - wing * 2, y + tip, x, y);
            g.drawLine(x, y, x + wing * 2, y + tip);
        }
    }

    private static void hill(Graphics2D g, int width, int height, int base, Color color, double wave, double rise,
                             double shift) {
        Polygon hill = new Polygon();
        hill.addPoint(0, height);
        for (int x = 0; x <= width + 8; x += 8) {
            double v = Math.sin((x + shift) / wave) * rise + Math.sin((x + shift) / (wave / 2.3)) * rise / 3;
            hill.addPoint(x, base - (int) Math.round(v));
        }
        hill.addPoint(width, height);
        g.setColor(color);
        g.fillPolygon(hill);
    }

    private void land(Graphics2D g, int width, int height, int horizon, long time) {
        int s = Math.max(32, Math.round(height / 15f / 8f) * 8);
        int top = horizon + s / 4;
        Optional<BufferedImage> grass = assets.sprite("terrain/GRASS_SPRING.png");
        if (grass.isEmpty()) {
            g.setColor(HILL_NEAR.darker());
            g.fillRect(0, top, width, height - top);
            return;
        }
        for (int row = 0; top + row * s < height; row++) {
            for (int col = 0; col * s < width; col++) {
                int variant = GroundLayer.variant(col, row + 50);
                BufferedImage tile = assets.sprite(GroundLayer.grassName("SPRING", variant)).orElse(grass.get());
                scaler.draw(g, tile, col * s, top + row * s, s, s);
            }
        }
        int soilTop = top + s * 2;
        int soilLeft = width * 30 / 100;
        int soilCols = Math.max(3, width * 32 / 100 / s);
        assets.sprite("terrain/SOIL.png").ifPresent(soil -> {
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < soilCols; col++) {
                    scaler.draw(g, soil, soilLeft + col * s, soilTop + row * s, s, s);
                }
            }
        });
        String[] crops = {"CARROT_MATURE", "CORN_MATURE", "PUMPKIN_MATURE", "TOMATO_MATURE", "POTATO_MATURE"};
        assets.sprite("terrain/TREE_SPRING.png").ifPresent(tree -> {
            for (int[] at : new int[][] {{2, -1}, {7, -2}, {85, -1}, {93, -2}}) {
                shadow(g, width * at[0] / 100 + s / 2, top + s + at[1] * s / 3, s);
                scaler.draw(g, tree, width * at[0] / 100 - s / 2, top - s + at[1] * s / 3, 2 * s, 2 * s);
            }
        });
        assets.sprite("terrain/FENCE_H.png").ifPresent(fence -> {
            for (int x = 0; x < width; x += s) {
                scaler.draw(g, fence, x, top + s / 4, s, s);
            }
        });
        building(g, "buildings/BARN.png", width * 12 / 100, top + s * 3, s);
        building(g, "buildings/COOP.png", width * 70 / 100, top + s * 3, s);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < soilCols; col++) {
                String name = crops[(col + row * 2) % crops.length];
                int x = soilLeft + col * s;
                int y = soilTop + row * s;
                assets.sprite("crops/" + name + ".png").ifPresent(crop -> scaler.draw(g, crop, x, y, s, s));
            }
        }
        for (int i = 0; i < HENS; i++) {
            double phase = time / 2600.0 + i * 2.1;
            int x = width * (64 + i * 9) / 100 + (int) Math.round(Math.sin(phase) * s * 1.2);
            int y = top + s * 3 + s / 4 + i * s / 3;
            int frame = (int) ((time / 250 + i) % 3);
            boolean left = Math.cos(phase) < 0;
            shadow(g, x + s / 2, y + s - s / 8, s * 2 / 3);
            assets.sprite("animals/HEN_" + frame + ".png").ifPresent(hen -> scaler.draw(g, hen, x, y, left ? -s : s, s));
        }
        int walk = (int) Math.floorMod(time / 14, (long) (width + 4 * s)) - 2 * s;
        int step = (int) ((time / 160) % 3);
        int farmerY = top + s * 4 + s / 3;
        shadow(g, walk + s / 2, farmerY + s - s / 8, s * 2 / 3);
        assets.sprite("characters/AVATAR_E_" + step + ".png").ifPresent(avatar -> scaler.draw(g, avatar, walk, farmerY, s, s));
    }

    private void building(Graphics2D g, String name, int x, int bottom, int s) {
        assets.sprite(name).ifPresent(img -> {
            int w = img.getWidth() * s / MapGeometry.TILE;
            int h = img.getHeight() * s / MapGeometry.TILE;
            shadow(g, x + w / 2, bottom - s / 6, w + s / 2);
            scaler.draw(g, img, x, bottom - h, w, h);
        });
    }

    private static void shadow(Graphics2D g, int cx, int cy, int w) {
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(cx - w / 2, cy - w / 8, w, w / 4);
    }
}
