package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * A building on the map with the animals inside it.
 *
 * <p>Stories: HU-18, HU-19, HU-35.
 *
 * @param id              position in the order the buildings were built; it identifies the building
 * @param type            building id, for example {@code COOP}
 * @param name            Spanish name
 * @param col             column of its top-left plot
 * @param row             row of its top-left plot
 * @param width           columns it covers
 * @param height          rows it covers
 * @param capacity        animals it can house (0 if it houses none)
 * @param productName     Spanish name of what its animals produce, or {@code null}
 * @param pendingProducts units waiting to be collected
 * @param animals         the animals inside
 */
public record BuildingView(int id, String type, String name, int col, int row, int width, int height,
                           int capacity, String productName, int pendingProducts, List<AnimalView> animals) {

    /** True if the plot belongs to the building. */
    public boolean covers(int plotCol, int plotRow) {
        return plotCol >= col && plotCol < col + width && plotRow >= row && plotRow < row + height;
    }
}
