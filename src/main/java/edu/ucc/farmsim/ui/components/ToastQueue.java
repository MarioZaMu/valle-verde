package edu.ucc.farmsim.ui.components;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * The notices of the screen: at most {@link #MAX_VISIBLE} at once, the rest waiting in order (HU-39 · CA-3).
 * A notice stays {@link ToastKind#durationMs()} fully visible and then fades for {@link #FADE_MS}; the time
 * counts from the moment it becomes visible, not from the moment it was posted. It has no Swing and takes its
 * clock from outside, so it is tested without waiting. It is safe to post from any thread.
 *
 * <p>Stories: HU-39.
 */
public final class ToastQueue {

    /** Most notices on screen at the same time. */
    public static final int MAX_VISIBLE = 4;

    /** Milliseconds of the fade-out. */
    public static final int FADE_MS = 400;

    private record Shown(String text, ToastKind kind, long since) { }

    private record Pending(String text, ToastKind kind) { }

    private final LongSupplier clock;
    private final List<Shown> shown = new ArrayList<>();
    private final Deque<Pending> waiting = new ArrayDeque<>();

    /**
     * Creates an empty queue.
     *
     * @param clock current time in milliseconds
     */
    public ToastQueue(LongSupplier clock) {
        this.clock = clock;
    }

    /** Adds a notice; it is shown at once if there is room, otherwise it waits its turn. */
    public synchronized void post(ToastKind kind, String text) {
        waiting.add(new Pending(text, kind));
        refresh();
    }

    /** The notices to draw now, oldest first. */
    public synchronized List<ToastView> visible() {
        refresh();
        long now = clock.getAsLong();
        List<ToastView> views = new ArrayList<>();
        for (Shown toast : shown) {
            long age = now - toast.since();
            float opacity = age <= toast.kind().durationMs()
                    ? 1f
                    : Math.max(0f, 1f - (float) (age - toast.kind().durationMs()) / FADE_MS);
            views.add(new ToastView(toast.text(), toast.kind(), opacity));
        }
        return views;
    }

    /** How many notices are waiting for room. */
    public synchronized int waiting() {
        refresh();
        return waiting.size();
    }

    /** True when nothing is shown and nothing is waiting. */
    public synchronized boolean isIdle() {
        refresh();
        return shown.isEmpty() && waiting.isEmpty();
    }

    private void refresh() {
        long now = clock.getAsLong();
        Iterator<Shown> iterator = shown.iterator();
        while (iterator.hasNext()) {
            Shown toast = iterator.next();
            if (now - toast.since() >= toast.kind().durationMs() + FADE_MS) {
                iterator.remove();
            }
        }
        while (shown.size() < MAX_VISIBLE && !waiting.isEmpty()) {
            Pending next = waiting.poll();
            shown.add(new Shown(next.text(), next.kind(), now));
        }
    }
}
