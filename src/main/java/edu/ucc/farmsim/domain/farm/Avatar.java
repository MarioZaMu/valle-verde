package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * The player's character on the farm (RN-36): it stands on one plot and looks to one of four directions. A step
 * goes to the neighbour plot if it can be walked on; if not, the avatar stays where it is but turns to look
 * there. Walking costs no energy and does not move the calendar, so the avatar knows nothing about either.
 *
 * <p>Stories: HU-45.
 */
public final class Avatar {

    private int col;
    private int row;
    private Direction facing;

    private Avatar(int col, int row, Direction facing) {
        this.col = col;
        this.row = row;
        this.facing = facing;
    }

    /** The avatar of a new game: on the configured grass plot, looking south. */
    public static Avatar start() {
        return new Avatar(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW, Direction.SOUTH);
    }

    /** Column of the plot it stands on. */
    public int col() {
        return col;
    }

    /** Row of the plot it stands on. */
    public int row() {
        return row;
    }

    /** The direction it looks to. */
    public Direction facing() {
        return facing;
    }

    /** Column of the plot in front of it (it may be outside the map). */
    public int frontCol() {
        return col + facing.dCol();
    }

    /** Row of the plot in front of it (it may be outside the map). */
    public int frontRow() {
        return row + facing.dRow();
    }

    /** True if it stands on a plot of the area whose top-left plot and size are given. */
    public boolean isInside(int left, int top, int width, int height) {
        return col >= left && col < left + width && row >= top && row < top + height;
    }

    /**
     * Turns to the direction and steps to the neighbour plot there if the farm lets it walk on it.
     *
     * @return {@code true} if it moved; {@code false} if the way was blocked (it only turned)
     */
    public boolean step(Direction direction, FarmMap farm) {
        facing = direction;
        int toCol = col + direction.dCol();
        int toRow = row + direction.dRow();
        if (!farm.isWalkable(toCol, toRow)) {
            return false;
        }
        col = toCol;
        row = toRow;
        return true;
    }

    /** Copy of the position and the direction, for the memento. */
    public AvatarSnapshot snapshot() {
        return new AvatarSnapshot(col, row, facing);
    }

    /** Puts the avatar back where the snapshot says. */
    public void restoreFrom(AvatarSnapshot snapshot) {
        this.col = snapshot.col();
        this.row = snapshot.row();
        this.facing = snapshot.facing();
    }

    /**
     * Puts the avatar back at its starting place, looking south. If something was built there, it goes to the
     * nearest plot it can walk on (used when a save of version 1, without avatar, is loaded).
     */
    public void returnToStart(FarmMap farm) {
        int[] place = farm.nearestWalkable(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW);
        this.col = place[0];
        this.row = place[1];
        this.facing = Direction.SOUTH;
    }
}
