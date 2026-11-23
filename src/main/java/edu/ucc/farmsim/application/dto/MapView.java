package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * The whole farm map as the screen draws it: its size and one {@link PlotView} per plot in row-major order.
 * Immutable.
 *
 * <p>Stories: HU-35, HU-45, HU-46, HU-50.
 *
 * @param cols      number of columns
 * @param rows      number of rows
 * @param plots     every plot, row by row
 * @param buildings the buildings drawn over the plots, with their animals
 * @param avatar    where the avatar stands and looks, or {@code null} if it is not drawn
 * @param npcs      the people of the valley, each on its plot
 * @param houses    the houses of the valley, drawn over their plots
 */
public record MapView(int cols, int rows, List<PlotView> plots, List<BuildingView> buildings, AvatarView avatar,
                      List<NpcView> npcs, List<HouseView> houses) {

    /** A map with the avatar and the people, without houses. */
    public MapView(int cols, int rows, List<PlotView> plots, List<BuildingView> buildings, AvatarView avatar,
                   List<NpcView> npcs) {
        this(cols, rows, plots, buildings, avatar, npcs, List.of());
    }

    /** A map with the avatar and without people. */
    public MapView(int cols, int rows, List<PlotView> plots, List<BuildingView> buildings, AvatarView avatar) {
        this(cols, rows, plots, buildings, avatar, List.of());
    }

    /** A map without buildings and without avatar. */
    public MapView(int cols, int rows, List<PlotView> plots) {
        this(cols, rows, plots, List.of());
    }

    /** A map without avatar. */
    public MapView(int cols, int rows, List<PlotView> plots, List<BuildingView> buildings) {
        this(cols, rows, plots, buildings, null);
    }

    /** The building that covers the plot, or {@code null}. */
    public BuildingView buildingAt(int col, int row) {
        for (BuildingView building : buildings) {
            if (building.covers(col, row)) {
                return building;
            }
        }
        return null;
    }

    /** The person standing on the plot, or {@code null}. */
    public NpcView npcAt(int col, int row) {
        for (NpcView npc : npcs == null ? List.<NpcView>of() : npcs) {
            if (npc.col() == col && npc.row() == row) {
                return npc;
            }
        }
        return null;
    }

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
