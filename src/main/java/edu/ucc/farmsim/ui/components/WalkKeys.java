package edu.ucc.farmsim.ui.components;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The walking keys that are held down, and when the avatar takes its next step (RN-40). Pressing a key steps at
 * once; while it stays down the avatar steps again every {@code stepMs}, exactly when the previous step ends, so
 * the walk never pauses. The key repeat of the system is ignored. With several keys down, the last one pressed
 * leads, and releasing it goes back to one still held. It keeps no clock of its own: the caller passes the time.
 *
 * <p>Stories: HU-50.
 */
public final class WalkKeys {

    private final long stepMs;
    private final Deque<String> held = new ArrayDeque<>();
    private long next;

    /**
     * Creates the keys.
     *
     * @param stepMs how long one step takes, in milliseconds
     */
    public WalkKeys(long stepMs) {
        this.stepMs = stepMs;
    }

    /**
     * A walking key went down.
     *
     * @param direction the direction of the key
     * @param now       the time, in milliseconds
     * @return the direction to step to now, or {@code null} when the key was already down (a key repeat)
     */
    public String press(String direction, long now) {
        if (held.contains(direction)) {
            return null;
        }
        held.addLast(direction);
        next = now + stepMs;
        return direction;
    }

    /** A walking key went up. */
    public void release(String direction) {
        held.remove(direction);
    }

    /** Forgets every key, for when the window loses the keyboard and would never hear them go up. */
    public void clear() {
        held.clear();
    }

    /**
     * Called often while walking.
     *
     * @param now the time, in milliseconds
     * @return the direction of the next step when its time has come, or {@code null}
     */
    public String tick(long now) {
        if (held.isEmpty() || now < next) {
            return null;
        }
        next = now - next >= stepMs ? now + stepMs : next + stepMs;
        return held.peekLast();
    }

    /** True while a walking key is held down. */
    public boolean isWalking() {
        return !held.isEmpty();
    }
}
