package edu.ucc.farmsim.ui.components;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * The walking keys that are held down, and when the avatar takes its next step (RN-40). Pressing a key while
 * standing steps at once; while keys stay down the avatar steps again every {@code stepMs}, exactly when the
 * previous step ends, so the walk never pauses and never jerks. A key pressed in the middle of a step waits for
 * that step to end. The key repeat of the system is ignored. With several keys down the last one pressed leads;
 * with two perpendicular keys (such as W and D) the steps alternate between them, which walks in a diagonal.
 * Releasing a key goes on with the ones still held. It keeps no clock of its own: the caller passes the time.
 *
 * <p>Stories: HU-50.
 */
public final class WalkKeys {

    private final long stepMs;
    private final Deque<String> held = new ArrayDeque<>();
    private long next;
    private String last;

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
     * @return the direction to step to now, or {@code null} when the key was already down (a key repeat) or a
     *         step is still under way (the key then counts from the next step)
     */
    public String press(String direction, long now) {
        if (held.contains(direction)) {
            return null;
        }
        boolean standing = held.isEmpty();
        held.addLast(direction);
        if (!standing && now < next) {
            return null;
        }
        next = now + stepMs;
        last = direction;
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
        String leader = held.peekLast();
        String partner = alternative(leader);
        last = partner != null && leader.equals(last) ? partner : leader;
        return last;
    }

    /**
     * The held key to try when a step to {@code direction} is blocked: the most recent one at a right angle to
     * it, or {@code null}. The step taken instead counts for the zigzag.
     */
    public String alternative(String direction) {
        for (Iterator<String> it = held.descendingIterator(); it.hasNext();) {
            String other = it.next();
            if (vertical(other) != vertical(direction)) {
                return other;
            }
        }
        return null;
    }

    /** Tells that the step went to {@code direction} instead (the planned one was blocked). */
    public void steppedInstead(String direction) {
        last = direction;
    }

    /** True while a walking key is held down. */
    public boolean isWalking() {
        return !held.isEmpty();
    }

    private static boolean vertical(String direction) {
        return "NORTH".equals(direction) || "SOUTH".equals(direction);
    }
}
