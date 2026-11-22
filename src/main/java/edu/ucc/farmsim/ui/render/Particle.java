package edu.ucc.farmsim.ui.render;

/**
 * A drop of rain or a snowflake that falls over the map. Its position is a fraction of the map area, so it does
 * not depend on the size of the window. It only exists in the view: nothing in the game knows about it.
 *
 * <p>Stories: HU-40.
 */
public final class Particle {

    /** What falls. */
    public enum Kind {
        /** A drop of rain: fast and slanted. */
        RAIN,
        /** A snowflake: slow and drifting. */
        SNOW
    }

    private final Kind kind;
    private final double speed;
    private final double drift;
    private double x;
    private double y;

    Particle(Kind kind, double x, double y, double speed, double drift) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.drift = drift;
    }

    /** What this particle is. */
    public Kind kind() {
        return kind;
    }

    /** Horizontal position, from 0 (left) to 1 (right). */
    public double x() {
        return x;
    }

    /** Vertical position, from 0 (top) to 1 (bottom). */
    public double y() {
        return y;
    }

    void advance(double seconds) {
        y += speed * seconds;
        x += drift * seconds;
        y -= Math.floor(y);
        x -= Math.floor(x);
    }
}
