package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.ui.theme.Palette;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Draws the farm map with Java2D, using only vector shapes (no image files needed). Each terrain and each
 * crop stage has its own look; it works on any {@link Graphics2D}, including one of an image, so it is tested
 * without a display.
 *
 * <p>Stories: HU-35.
 */
public final class MapRenderer {

    private static final float STEM = 0.30f;
    private static final float LEAF_W = 0.22f;
    private static final float LEAF_H = 0.12f;
    private static final float FRUIT = 0.16f;
    private static final int HOVER_WIDTH = 3;

    /**
     * Paints the whole map into the given area.
     *
     * @param g      where to paint
     * @param map    the map to draw
     * @param width  width of the area
     * @param height height of the area
     * @param hover  plot under the cursor, or {@code null}
     */
    public void render(Graphics2D g, MapView map, int width, int height, CellPos hover) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Palette.BACKGROUND);
        g.fillRect(0, 0, width, height);
        MapGeometry geometry = MapGeometry.fit(map.cols(), map.rows(), width, height);
        for (PlotView plot : map.plots()) {
            Rectangle cell = geometry.cellBounds(plot.col(), plot.row());
            drawTerrain(g, plot, cell);
            if (plot.crop() != null) {
                drawCrop(g, plot, cell);
            }
            g.setColor(Palette.GRID);
            g.drawRect(cell.x, cell.y, cell.width - 1, cell.height - 1);
        }
        if (hover != null) {
            Rectangle cell = geometry.cellBounds(hover.col(), hover.row());
            g.setColor(Palette.HOVER);
            g.setStroke(new BasicStroke(HOVER_WIDTH));
            g.drawRect(cell.x + 1, cell.y + 1, cell.width - 3, cell.height - 3);
            g.setStroke(new BasicStroke(1f));
        }
    }

    private void drawTerrain(Graphics2D g, PlotView plot, Rectangle c) {
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
                g.setColor(Palette.GRASS);
                g.fillRect(c.x, c.y, c.width, c.height);
            }
        }
    }

    private void drawCrop(Graphics2D g, PlotView plot, Rectangle c) {
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
