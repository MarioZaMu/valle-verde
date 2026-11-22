package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * The whole farm map as the screen draws it: its size and one {@link PlotView} per plot in row-major order.
 * Immutable.
 *
 * <p>Stories: HU-35.
 *
 * @param cols  number of columns
 * @param rows  number of rows
 * @param plots every plot, row by row
 */
public record MapView(int cols, int rows, List<PlotView> plots) {

    /**
     * The plot at the given coordinates.
     *
     * @throws IllegalArgumentException if the coordinates are outside the map
     */
    public PlotView plotAt(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            throw new IllegalArgumentException("outside the map: (" + col + ", " + row + ")");
        }
        return plots.get(row * cols + col);
    }
}
