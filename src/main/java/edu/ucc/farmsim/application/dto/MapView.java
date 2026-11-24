package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * Todo el mapa de la granja tal como lo dibuja la pantalla: su tamaño y un {@link PlotView} por parcela en orden de
 * fila. Inmutable.
 *
 * <p>Stories: HU-35, HU-45, HU-46, HU-50.
 *
 * @param cols      número de columnas
 * @param rows      número de filas
 * @param plots     todas las parcelas, fila por fila
 * @param buildings los edificios dibujados sobre las parcelas, con sus animales
 * @param avatar    dónde está el avatar y hacia dónde mira, o {@code null} si no se dibuja
 * @param npcs      las personas del valle, cada una en su parcela
 * @param houses    las casas del valle, dibujadas sobre sus parcelas
 */
public record MapView(int cols, int rows, List<PlotView> plots, List<BuildingView> buildings, AvatarView avatar,
                      List<NpcView> npcs, List<HouseView> houses) {

    /** Un mapa con el avatar y las personas, sin casas. */
    public MapView(int cols, int rows, List<PlotView> plots, List<BuildingView> buildings, AvatarView avatar,
                   List<NpcView> npcs) {
        this(cols, rows, plots, buildings, avatar, npcs, List.of());
    }

    /** Un mapa con el avatar y sin personas. */
    public MapView(int cols, int rows, List<PlotView> plots, List<BuildingView> buildings, AvatarView avatar) {
        this(cols, rows, plots, buildings, avatar, List.of());
    }

    /** Un mapa sin edificios y sin avatar. */
    public MapView(int cols, int rows, List<PlotView> plots) {
        this(cols, rows, plots, List.of());
    }

    /** Un mapa sin avatar. */
    public MapView(int cols, int rows, List<PlotView> plots, List<BuildingView> buildings) {
        this(cols, rows, plots, buildings, null);
    }

    /** El mismo mapa con el avatar en otro lugar o mirando a otro lado (un paso no cambia nada más). */
    public MapView withAvatar(AvatarView moved) {
        return new MapView(cols, rows, plots, buildings, moved, npcs, houses);
    }

    /** El edificio que cubre la parcela, o {@code null}. */
    public BuildingView buildingAt(int col, int row) {
        for (BuildingView building : buildings) {
            if (building.covers(col, row)) {
                return building;
            }
        }
        return null;
    }

    /** La persona que está sobre la parcela, o {@code null}. */
    public NpcView npcAt(int col, int row) {
        for (NpcView npc : npcs == null ? List.<NpcView>of() : npcs) {
            if (npc.col() == col && npc.row() == row) {
                return npc;
            }
        }
        return null;
    }

    /**
     * La parcela en las coordenadas dadas.
     *
     * @throws IllegalArgumentException si las coordenadas están fuera del mapa
     */
    public PlotView plotAt(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            throw new IllegalArgumentException("outside the map: (" + col + ", " + row + ")");
        }
        return plots.get(row * cols + col);
    }
}
