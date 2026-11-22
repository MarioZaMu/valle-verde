package edu.ucc.farmsim.ui.render;

/**
 * Told when the player clicks a plot of the map.
 *
 * <p>Stories: HU-35.
 */
@FunctionalInterface
public interface PlotClickListener {

    /** Called with the coordinates of the clicked plot. */
    void plotClicked(int col, int row);
}
