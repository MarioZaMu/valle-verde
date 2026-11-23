package edu.ucc.farmsim.domain.farm;

/**
 * Immutable copy of where the avatar stands and where it looks, for the memento.
 *
 * <p>Stories: HU-45.
 *
 * @param col    column of the plot it stands on
 * @param row    row of the plot it stands on
 * @param facing the direction it looks to
 */
public record AvatarSnapshot(int col, int row, Direction facing) { }
