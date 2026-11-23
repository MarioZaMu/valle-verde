package edu.ucc.farmsim.application.dto;

/**
 * A house of the valley as the map draws it (RN-04). Immutable.
 *
 * <p>Stories: HU-50.
 *
 * @param id     {@code HOME}, {@code SHOP}, {@code FARMHOUSE}, {@code CLINIC} or {@code COTTAGE}
 * @param col    column of its top-left plot
 * @param row    row of its top-left plot
 * @param width  width in plots
 * @param height height in plots
 */
public record HouseView(String id, int col, int row, int width, int height) { }
