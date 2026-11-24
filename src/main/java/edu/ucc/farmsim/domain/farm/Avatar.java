package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * El personaje del jugador en la granja (RN-36): está en una parcela y mira hacia una de cuatro direcciones. Un paso va
 * a la parcela vecina si se puede caminar sobre ella; si no, el avatar se queda donde está pero gira para mirar hacia
 * allí. Caminar no cuesta energía ni mueve el calendario, así que el avatar no sabe nada de ninguno de los dos.
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

    /** El avatar de una partida nueva: en la parcela de pasto configurada, mirando al sur. */
    public static Avatar start() {
        return new Avatar(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW, Direction.SOUTH);
    }

    /** Columna de la parcela en que está. */
    public int col() {
        return col;
    }

    /** Fila de la parcela en que está. */
    public int row() {
        return row;
    }

    /** La dirección hacia la que mira. */
    public Direction facing() {
        return facing;
    }

    /** Columna de la parcela que tiene enfrente (puede estar fuera del mapa). */
    public int frontCol() {
        return col + facing.dCol();
    }

    /** Fila de la parcela que tiene enfrente (puede estar fuera del mapa). */
    public int frontRow() {
        return row + facing.dRow();
    }

    /** Verdadero si está en una parcela del área cuya parcela superior izquierda y tamaño se dan. */
    public boolean isInside(int left, int top, int width, int height) {
        return col >= left && col < left + width && row >= top && row < top + height;
    }

    /**
     * Gira hacia la dirección y da un paso a la parcela vecina de ese lado si la granja le deja caminar sobre ella.
     *
     * @return {@code true} si se movió; {@code false} si el camino estaba bloqueado (solo giró)
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

    /** Copia de la posición y la dirección, para el memento. */
    public AvatarSnapshot snapshot() {
        return new AvatarSnapshot(col, row, facing);
    }

    /** Devuelve al avatar donde dice la instantánea. */
    public void restoreFrom(AvatarSnapshot snapshot) {
        this.col = snapshot.col();
        this.row = snapshot.row();
        this.facing = snapshot.facing();
    }

    /**
     * Devuelve al avatar a su lugar de inicio, mirando al sur. Si allí se construyó algo, va a la parcela transitable
     * más cercana (se usa al cargar una partida de versión 1, sin avatar).
     */
    public void returnToStart(FarmMap farm) {
        int[] place = farm.nearestWalkable(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW);
        this.col = place[0];
        this.row = place[1];
        this.facing = Direction.SOUTH;
    }
}
