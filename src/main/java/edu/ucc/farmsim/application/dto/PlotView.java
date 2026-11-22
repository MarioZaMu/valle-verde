package edu.ucc.farmsim.application.dto;

/**
 * What the screen needs to draw and describe one plot. Immutable.
 *
 * <p>Stories: HU-01, HU-07, HU-25.
 *
 * @param col          column of the plot
 * @param row          row of the plot
 * @param terrain      terrain id ({@code SOIL}, {@code GRASS}, {@code WATER}, {@code OBSTACLE})
 * @param crop         crop id (for example {@code CARROT}), or {@code null} if the plot has no crop
 * @param stage        growth stage id ({@code SEED} to {@code WITHERED}), or {@code null} without a crop
 * @param watered      whether the crop was watered today
 * @param daysToMature days still needed to mature (0 when mature, withered or without a crop)
 * @param sheltered    whether a greenhouse covers the plot, so its crop is protected (RN-22)
 */
public record PlotView(int col, int row, String terrain, String crop, String stage,
                       boolean watered, int daysToMature, boolean sheltered) {

    /** A plot in the open: the common case. */
    public PlotView(int col, int row, String terrain, String crop, String stage, boolean watered, int daysToMature) {
        this(col, row, terrain, crop, stage, watered, daysToMature, false);
    }
}
