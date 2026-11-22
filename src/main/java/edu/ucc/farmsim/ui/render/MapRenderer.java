package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.application.dto.AnimalView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.theme.Palette;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Draws the farm map with Java2D. Each terrain and each crop stage has its own look, drawn with vector shapes;
 * if a sprite exists in the {@link AssetManager} it is used instead, and when it does not the vector graphic
 * is the fallback, so the game needs no image at all (HU-42). With a {@link VisualEffects} it also paints the
 * palette of the season, the weather, the night tint and the animations (HU-40, HU-41). It works on any
 * {@link Graphics2D}, including one of an image, so it is tested without a display.
 *
 * <p>Stories: HU-18, HU-19, HU-25, HU-35, HU-40, HU-41, HU-42, HU-44.
 */
public final class MapRenderer {

    private static final float STEM = 0.30f;
    private static final float LEAF_W = 0.22f;
    private static final float LEAF_H = 0.12f;
    private static final float FRUIT = 0.16f;
    private static final int HOVER_WIDTH = 3;
    private static final int NIGHT_MAX_ALPHA = 150;
    private static final Color NIGHT = new Color(10, 20, 60);
    private static final Color SUN = new Color(0xFFC21A);
    private static final Color SNOWFLAKE = new Color(0xFFFFFF);
    private static final Color RAIN = new Color(0xBFD9FF);

    private final AssetManager assets;

    /** Creates a renderer that looks for sprites in the {@code assets} folder of the class path. */
    public MapRenderer() {
        this(AssetManager.shared());
    }

    /**
     * Creates a renderer.
     *
     * @param assets where the sprites come from
     */
    public MapRenderer(AssetManager assets) {
        this.assets = assets;
    }

    /**
     * Paints the whole map into the given area, without effects.
     *
     * @param g      where to paint
     * @param map    the map to draw
     * @param width  width of the area
     * @param height height of the area
     * @param hover  plot under the cursor, or {@code null}
     */
    public void render(Graphics2D g, MapView map, int width, int height, CellPos hover) {
        render(g, map, width, height, hover, null);
    }

    /**
     * Paints the whole map into the given area.
     *
     * @param g       where to paint
     * @param map     the map to draw
     * @param width   width of the area
     * @param height  height of the area
     * @param hover   plot under the cursor, or {@code null}
     * @param effects the season, weather and animations to show, or {@code null} for the plain look
     */
    public void render(Graphics2D g, MapView map, int width, int height, CellPos hover, VisualEffects effects) {
        VisualEffects fx = effects != null && effects.isEnabled() ? effects : null;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(Palette.BACKGROUND);
        g.fillRect(0, 0, width, height);
        MapGeometry geometry = MapGeometry.fit(map.cols(), map.rows(), width, height);
        Color grass = fx == null ? Palette.GRASS : SeasonPalette.grass(fx.season());
        String season = fx == null ? "SPRING" : fx.season();
        for (PlotView plot : map.plots()) {
            Rectangle cell = geometry.cellBounds(plot.col(), plot.row());
            drawTerrain(g, plot, cell, grass, season);
            if (plot.crop() != null && !plot.sheltered()) {
                drawCrop(g, plot, cell, fx);
            }
            g.setColor(Palette.GRID);
            g.drawRect(cell.x, cell.y, cell.width - 1, cell.height - 1);
        }
        for (BuildingView building : map.buildings()) {
            drawBuilding(g, building, geometry, fx);
        }
        for (PlotView plot : map.plots()) {
            if (plot.crop() != null && plot.sheltered()) {
                drawCrop(g, plot, geometry.cellBounds(plot.col(), plot.row()), fx);
            }
        }
        if (fx != null) {
            drawAmbience(g, fx, geometry.area(), width, height);
        }
        if (hover != null) {
            Rectangle cell = geometry.cellBounds(hover.col(), hover.row());
            g.setColor(Palette.HOVER);
            g.setStroke(new BasicStroke(HOVER_WIDTH));
            g.drawRect(cell.x + 1, cell.y + 1, cell.width - 3, cell.height - 3);
            g.setStroke(new BasicStroke(1f));
        }
    }

    private void drawAmbience(Graphics2D g, VisualEffects fx, Rectangle area, int width, int height) {
        Color tint = SeasonPalette.tint(fx.weather());
        if (tint != null) {
            g.setColor(tint);
            g.fillRect(area.x, area.y, area.width, area.height);
        }
        for (Particle particle : fx.particles()) {
            int x = area.x + (int) Math.round(particle.x() * area.width);
            int y = area.y + (int) Math.round(particle.y() * area.height);
            if (particle.kind() == Particle.Kind.RAIN) {
                g.setColor(RAIN);
                g.drawLine(x, y, x - 3, y + 10);
            } else {
                g.setColor(SNOWFLAKE);
                g.fillOval(x - 2, y - 2, 5, 5);
            }
        }
        drawIndicator(g, fx.weather(), area);
        float night = fx.nightAlpha();
        if (night > 0f) {
            g.setColor(new Color(NIGHT.getRed(), NIGHT.getGreen(), NIGHT.getBlue(), Math.round(night * NIGHT_MAX_ALPHA)));
            g.fillRect(0, 0, width, height);
        }
    }

    private void drawIndicator(Graphics2D g, String weather, Rectangle area) {
        int r = Math.max(8, area.height / 36);
        int cx = area.x + area.width - 2 * r;
        int cy = area.y + 2 * r;
        g.setStroke(new BasicStroke(Math.max(2f, r / 4f)));
        if ("DROUGHT".equals(weather)) {
            g.setColor(SUN);
            g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * i / 4;
                g.drawLine(cx + (int) Math.round(Math.cos(angle) * r * 1.3), cy + (int) Math.round(Math.sin(angle) * r * 1.3),
                        cx + (int) Math.round(Math.cos(angle) * r * 1.9), cy + (int) Math.round(Math.sin(angle) * r * 1.9));
            }
        } else if ("FROST".equals(weather)) {
            g.setColor(SNOWFLAKE);
            for (int i = 0; i < 3; i++) {
                double angle = Math.PI * i / 3;
                g.drawLine(cx - (int) Math.round(Math.cos(angle) * r * 1.6), cy - (int) Math.round(Math.sin(angle) * r * 1.6),
                        cx + (int) Math.round(Math.cos(angle) * r * 1.6), cy + (int) Math.round(Math.sin(angle) * r * 1.6));
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawBuilding(Graphics2D g, BuildingView building, MapGeometry geometry, VisualEffects fx) {
        Rectangle first = geometry.cellBounds(building.col(), building.row());
        Rectangle last = geometry.cellBounds(building.col() + building.width() - 1, building.row() + building.height() - 1);
        Rectangle area = first.union(last);
        int inset = Math.max(2, area.height / 16);
        Rectangle body = new Rectangle(area.x + inset, area.y + inset, area.width - 2 * inset, area.height - 2 * inset);
        Optional<BufferedImage> sheet = assets.sprite("buildings/" + building.type() + ".png");
        if (sheet.isPresent()) {
            g.drawImage(sheet.get(), area.x, area.y, area.width, area.height, null);
            drawAnimalSprites(g, building, area, fx);
            drawPending(g, building, area);
            return;
        }
        if ("GREENHOUSE".equals(building.type())) {
            g.setColor(Palette.GREENHOUSE_GLASS);
            g.fillRect(body.x, body.y, body.width, body.height);
            g.setColor(Palette.BUILDING_OUTLINE);
            for (int i = 1; i < building.width(); i++) {
                int x = body.x + body.width * i / building.width();
                g.drawLine(x, body.y, x, body.y + body.height);
            }
            for (int i = 1; i < building.height(); i++) {
                int y = body.y + body.height * i / building.height();
                g.drawLine(body.x, y, body.x + body.width, y);
            }
        } else {
            g.setColor(wallColor(building.type()));
            g.fillRect(body.x, body.y, body.width, body.height);
            int roofHeight = body.height / 3;
            g.setColor(Palette.ROOF);
            g.fillPolygon(new int[] {body.x - inset, body.x + body.width / 2, body.x + body.width + inset},
                    new int[] {body.y + roofHeight, body.y - inset, body.y + roofHeight}, 3);
        }
        g.setColor(Palette.BUILDING_OUTLINE);
        g.drawRect(body.x, body.y, body.width, body.height);
        drawAnimals(g, building, body, fx);
        drawPending(g, building, area);
    }

    private void drawPending(Graphics2D g, BuildingView building, Rectangle area) {
        if (building.pendingProducts() > 0) {
            int r = Math.max(4, area.width / 12);
            g.setColor(Palette.PENDING);
            g.fillOval(area.x + area.width - 2 * r, area.y, 2 * r, 2 * r);
            g.setColor(Palette.BUILDING_OUTLINE);
            g.drawOval(area.x + area.width - 2 * r, area.y, 2 * r, 2 * r);
        }
    }

    /** Animals in front of a building drawn with a sprite: one tile-sized sprite each, with a badge if not well. */
    private void drawAnimalSprites(Graphics2D g, BuildingView building, Rectangle area, VisualEffects fx) {
        int slots = Math.max(1, building.capacity());
        int d = Math.max(8, Math.min(area.width / slots, area.height / 2));
        for (AnimalView animal : building.animals()) {
            int frame = fx == null ? 0 : fx.animalFrame(animal.index());
            Optional<BufferedImage> sprite = assets.sprite("animals/" + animal.type() + "_" + frame + ".png");
            int x = area.x + animal.index() * area.width / slots + (area.width / slots - d) / 2;
            int y = area.y + area.height - d - Math.max(1, area.height / 32);
            if (sprite.isPresent()) {
                g.drawImage(sprite.get(), x, y, d, d, null);
            } else {
                drawAnimalVector(g, animal, x, y, d, frame);
            }
            drawBadge(g, animal, x + d, y, d);
        }
    }

    private static void drawBadge(Graphics2D g, AnimalView animal, int right, int top, int d) {
        boolean sick = "SICK".equals(animal.condition());
        if (!sick && !"HUNGRY".equals(animal.condition())) {
            return;
        }
        int r = Math.max(3, d / 5);
        int x = right - 2 * r;
        g.setColor(sick ? Palette.ANIMAL_SICK : Palette.ANIMAL_HUNGRY);
        g.fillOval(x, top, 2 * r, 2 * r);
        g.setColor(Palette.BUILDING_OUTLINE);
        g.drawOval(x, top, 2 * r, 2 * r);
        if (sick) {
            g.drawLine(x + r, top + r / 2, x + r, top + 2 * r - r / 2);
            g.drawLine(x + r / 2, top + r, x + 2 * r - r / 2, top + r);
        }
    }

    private static Color wallColor(String type) {
        return switch (type) {
            case "BARN" -> Palette.BARN_WALL;
            case "WAREHOUSE" -> Palette.WAREHOUSE_WALL;
            default -> Palette.COOP_WALL;
        };
    }

    private void drawAnimals(Graphics2D g, BuildingView building, Rectangle body, VisualEffects fx) {
        int slots = Math.max(1, building.capacity());
        int d = Math.max(4, body.width / (slots + 1));
        for (AnimalView animal : building.animals()) {
            int frame = fx == null ? 0 : fx.animalFrame(animal.index());
            int x = body.x + body.width * (2 * animal.index() + 1) / (2 * slots) - d / 2;
            int y = body.y + body.height * 2 / 3 - d / 2 - bob(frame, d);
            Optional<BufferedImage> sprite = assets.sprite("animals/" + animal.type() + "_" + frame + ".png");
            if (sprite.isPresent()) {
                g.drawImage(sprite.get(), x, y, d, d, null);
                continue;
            }
            g.setColor(switch (animal.condition()) {
                case "SICK" -> Palette.ANIMAL_SICK;
                case "HUNGRY" -> Palette.ANIMAL_HUNGRY;
                default -> Palette.ANIMAL_HEALTHY;
            });
            paintVectorAnimal(g, animal, x, y, d);
        }
    }

    private static void drawAnimalVector(Graphics2D g, AnimalView animal, int x, int y, int d, int frame) {
        g.setColor(switch (animal.condition()) {
            case "SICK" -> Palette.ANIMAL_SICK;
            case "HUNGRY" -> Palette.ANIMAL_HUNGRY;
            default -> Palette.ANIMAL_HEALTHY;
        });
        paintVectorAnimal(g, animal, x, y - bob(frame, d), d);
    }

    private static void paintVectorAnimal(Graphics2D g, AnimalView animal, int x, int y, int d) {
        if ("COW".equals(animal.type())) {
            g.fillOval(x - d / 4, y, d + d / 2, d);
            g.setColor(Palette.BUILDING_OUTLINE);
            g.fillOval(x + d / 4, y + d / 4, d / 3, d / 3);
        } else {
            g.fillOval(x, y, d, d);
            g.setColor(Palette.BARN_WALL);
            g.fillOval(x + d / 3, y - d / 5, d / 3, d / 3);
        }
    }

    /** How many pixels the animal rises on each frame of its little hop: down, up, halfway. */
    private static int bob(int frame, int d) {
        return switch (frame) {
            case 1 -> Math.max(1, d / 6);
            case 2 -> Math.max(1, d / 12);
            default -> 0;
        };
    }

    private void drawTerrain(Graphics2D g, PlotView plot, Rectangle c, Color grass, String season) {
        String name = switch (plot.terrain()) {
            case "SOIL" -> plot.watered() ? "terrain/SOIL_WATERED.png" : "terrain/SOIL.png";
            case "WATER" -> "terrain/WATER.png";
            case "OBSTACLE" -> "terrain/OBSTACLE.png";
            default -> "terrain/GRASS_" + season + ".png";
        };
        Optional<BufferedImage> sprite = assets.sprite(name);
        if (sprite.isPresent()) {
            g.drawImage(sprite.get(), c.x, c.y, c.width, c.height, null);
            return;
        }
        switch (plot.terrain()) {
            case "SOIL" -> {
                g.setColor(plot.watered() ? Palette.SOIL_WATERED : Palette.SOIL);
                g.fillRect(c.x, c.y, c.width, c.height);
                g.setColor(Palette.FURROW);
                for (int i = 1; i <= 2; i++) {
                    int y = c.y + c.height * i / 3;
                    g.drawLine(c.x + c.width / 8, y, c.x + c.width * 7 / 8, y);
                }
            }
            case "WATER" -> {
                g.setColor(Palette.WATER);
                g.fillRect(c.x, c.y, c.width, c.height);
                g.setColor(Palette.WAVE);
                int y = c.y + c.height / 2;
                g.drawArc(c.x + c.width / 6, y - c.height / 10, c.width / 3, c.height / 5, 0, 180);
                g.drawArc(c.x + c.width / 2, y - c.height / 10, c.width / 3, c.height / 5, 0, 180);
            }
            case "OBSTACLE" -> {
                g.setColor(Palette.OBSTACLE);
                g.fillRect(c.x, c.y, c.width, c.height);
                g.setColor(Palette.TRUNK);
                g.fillRect(c.x + c.width * 9 / 20, c.y + c.height / 2, c.width / 10, c.height * 2 / 5);
                g.setColor(Palette.LEAF);
                g.fillOval(c.x + c.width / 5, c.y + c.height / 8, c.width * 3 / 5, c.height * 3 / 5);
            }
            default -> {
                g.setColor(grass);
                g.fillRect(c.x, c.y, c.width, c.height);
            }
        }
    }

    private void drawCrop(Graphics2D g, PlotView plot, Rectangle c, VisualEffects fx) {
        float scale = fx == null ? 1f : fx.cropScale(plot.col(), plot.row());
        AffineTransform saved = g.getTransform();
        if (scale < 1f) {
            double cx = c.x + c.width / 2.0;
            double cy = c.y + c.height / 2.0;
            g.translate(cx, cy);
            g.scale(scale, scale);
            g.translate(-cx, -cy);
        }
        Optional<BufferedImage> sprite = assets.sprite("crops/" + plot.crop() + "_" + plot.stage() + ".png");
        if (sprite.isPresent()) {
            g.drawImage(sprite.get(), c.x, c.y, c.width, c.height, null);
        } else {
            drawCropVector(g, plot, c);
        }
        g.setTransform(saved);
    }

    private void drawCropVector(Graphics2D g, PlotView plot, Rectangle c) {
        int s = c.width;
        int cx = c.x + s / 2;
        int cy = c.y + s / 2;
        g.setStroke(new BasicStroke(Math.max(1f, s / 24f)));
        switch (plot.stage()) {
            case "SEED" -> {
                g.setColor(Palette.SEED);
                int r = Math.max(2, Math.round(s * 0.08f));
                g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            }
            case "SPROUT" -> {
                g.setColor(Palette.LEAF);
                g.drawLine(cx, cy + s / 6, cx - Math.round(s * 0.12f), cy - s / 20);
                g.drawLine(cx, cy + s / 6, cx + Math.round(s * 0.12f), cy - s / 20);
            }
            case "GROWING" -> drawPlant(g, cx, cy, s, false, plot);
            case "MATURE" -> drawPlant(g, cx, cy, s, true, plot);
            default -> {
                g.setColor(Palette.WITHERED);
                int d = Math.round(s * 0.18f);
                g.drawLine(cx - d, cy - d, cx + d, cy + d);
                g.drawLine(cx - d, cy + d, cx + d, cy - d);
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawPlant(Graphics2D g, int cx, int cy, int s, boolean withFruit, PlotView plot) {
        int top = cy - Math.round(s * STEM);
        g.setColor(Palette.LEAF);
        g.drawLine(cx, cy + Math.round(s * STEM), cx, top);
        int lw = Math.round(s * LEAF_W);
        int lh = Math.round(s * LEAF_H);
        g.fillOval(cx - lw, cy - lh, lw, lh);
        g.fillOval(cx, cy, lw, lh);
        g.fillOval(cx - lw, cy + lh, lw, lh);
        if (withFruit) {
            int r = Math.round(s * FRUIT);
            g.setColor(Palette.crop(plot.crop()));
            g.fillOval(cx - r, top - r, 2 * r, 2 * r);
        }
    }
}
