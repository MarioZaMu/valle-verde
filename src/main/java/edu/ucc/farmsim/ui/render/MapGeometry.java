package edu.ucc.farmsim.ui.render;

import java.awt.Rectangle;

/**
 * Where each plot is drawn: either the whole map fitted into an area ({@link #fit}), or plots of a fixed size
 * with a camera that follows the avatar ({@link #follow}, RN-40). Also answers which plot lies under a pixel.
 * No Swing component involved, so it is tested without a display.
 *
 * <p>Stories: HU-35, HU-44, HU-50.
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

    /** Side of a plot on the screen, in screen pixels, when the camera follows the avatar (RN-40). */
    public static final int SCREEN_CELL = 48;

    /** Plots across and down that always fit: a smaller window gets smaller plots. */
    private static final int MIN_COLS_SHOWN = 12;
    private static final int MIN_ROWS_SHOWN = 8;

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

    /**
     * Fits the map into an area of which a band at the top and one at the bottom are kept free (for the bars
     * that float over the view); the land around the map is still drawn under them.
     */
    public static MapGeometry fit(int cols, int rows, int width, int height, int top, int bottom) {
        MapGeometry inner = fit(cols, rows, width, Math.max(1, height - top - bottom));
        return new MapGeometry(cols, rows, inner.cellSize(), inner.originX(), inner.originY() + top);
    }

    /**
     * The side of a plot in logical pixels: {@link #SCREEN_CELL} screen pixels (so with Windows at 125 % a
     * plot is 38 logical pixels), but never so big that fewer than 12 x 8 plots fit in the area.
     *
     * @param deviceScale screen pixels per logical pixel
     */
    public static int cellFor(int width, int height, double deviceScale) {
        int wanted = (int) Math.round(SCREEN_CELL / Math.max(deviceScale, 0.1));
        return Math.max(1, Math.min(wanted, Math.min(width / MIN_COLS_SHOWN, height / MIN_ROWS_SHOWN)));
    }

    /**
     * Plots of the given size with a camera on a point of the map: the point goes to the middle of the view,
     * but the camera stops at the edges of the map so nothing outside it is shown; along an axis where the
     * whole map fits, the map is centered in the view.
     *
     * @param cell     side of a plot in pixels
     * @param view     the part of the area left free by the bars, where the point must stay
     * @param focusCol column of the point, in plots; it may be between two plots while the avatar walks
     * @param focusRow row of the point, in plots
     */
    public static MapGeometry follow(int cols, int rows, int cell, Rectangle view, double focusCol, double focusRow) {
        int originX = axis(cols * cell, view.x, view.width, (focusCol + 0.5) * cell);
        int originY = axis(rows * cell, view.y, view.height, (focusRow + 0.5) * cell);
        return new MapGeometry(cols, rows, cell, originX, originY);
    }

    private static int axis(int length, int start, int size, double focus) {
        if (length <= size) {
            return start + (size - length) / 2;
        }
        int origin = (int) Math.round(start + size / 2.0 - focus);
        return Math.max(start + size - length, Math.min(start, origin));
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
