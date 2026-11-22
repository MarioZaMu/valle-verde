package edu.ucc.farmsim.ui.render;

import java.awt.Rectangle;

/**
 * Where each plot is drawn: square cells as large as the area allows, centered. Also answers which plot lies
 * under a pixel. No Swing component involved, so it is tested without a display.
 *
 * <p>Stories: HU-35.
 *
 * @param cols     number of columns
 * @param rows     number of rows
 * @param cellSize side of a cell in pixels
 * @param originX  x of the top-left corner of the map
 * @param originY  y of the top-left corner of the map
 */
public record MapGeometry(int cols, int rows, int cellSize, int originX, int originY) {

    /** Fits the map into an area, with the largest square cells that fit, centered. */
    public static MapGeometry fit(int cols, int rows, int width, int height) {
        int size = Math.max(1, Math.min(width / cols, height / rows));
        int originX = (width - size * cols) / 2;
        int originY = (height - size * rows) / 2;
        return new MapGeometry(cols, rows, size, originX, originY);
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
