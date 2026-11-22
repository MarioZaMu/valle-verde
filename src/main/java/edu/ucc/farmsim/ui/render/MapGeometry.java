package edu.ucc.farmsim.ui.render;

import java.awt.Rectangle;

/**
 * Where each plot is drawn: square cells as large as the area allows, centered. Also answers which plot lies
 * under a pixel. No Swing component involved, so it is tested without a display.
 *
 * <p>Stories: HU-35, HU-44.
 *
 * @param cols     number of columns
 * @param rows     number of rows
 * @param cellSize side of a cell in pixels
 * @param originX  x of the top-left corner of the map
 * @param originY  y of the top-left corner of the map
 */
public record MapGeometry(int cols, int rows, int cellSize, int originX, int originY) {

    /** Side in pixels of one sprite: a cell that is a whole multiple of it keeps the pixel art sharp. */
    public static final int TILE = 32;

    /** A whole scale is used when it fills at least this fraction (in quarters) of what fits. */
    private static final int WHOLE_SCALE_MIN_QUARTERS = 3;

    /**
     * Fits the map into an area, with the largest square cells that fit, centered. The cell is a whole
     * multiple of {@link #TILE} (a whole scale of the sprites) when that wastes at most a quarter of the room;
     * otherwise it is the largest cell that fits, and the sprites are scaled by nearest neighbour.
     */
    public static MapGeometry fit(int cols, int rows, int width, int height) {
        int fitted = Math.max(1, Math.min(width / cols, height / rows));
        int whole = fitted / TILE * TILE;
        int size = whole >= TILE && whole * 4 >= fitted * WHOLE_SCALE_MIN_QUARTERS ? whole : fitted;
        int originX = (width - size * cols) / 2;
        int originY = (height - size * rows) / 2;
        return new MapGeometry(cols, rows, size, originX, originY);
    }

    /** Pixel rectangle of the whole map. */
    public Rectangle area() {
        return new Rectangle(originX, originY, cols * cellSize, rows * cellSize);
    }

    /** Pixel rectangle of a plot. */
    public Rectangle cellBounds(int col, int row) {
        return new Rectangle(originX + col * cellSize, originY + row * cellSize, cellSize, cellSize);
    }

    /** The plot under a pixel, or {@code null} if the pixel is outside the map. */
    public CellPos cellAt(int x, int y) {
        int dx = x - originX;
        int dy = y - originY;
        if (dx < 0 || dy < 0 || dx >= cols * cellSize || dy >= rows * cellSize) {
            return null;
        }
        return new CellPos(dx / cellSize, dy / cellSize);
    }
}
