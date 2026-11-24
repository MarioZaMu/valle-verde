package edu.ucc.farmsim.ui.components;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Los avisos de la pantalla: como máximo {@link #MAX_VISIBLE} a la vez, el resto esperando en orden (HU-39 · CA-3). Un
 * aviso se queda {@link ToastKind#durationMs()} totalmente visible y luego se desvanece durante {@link #FADE_MS}; el
 * tiempo cuenta desde el momento en que se vuelve visible, no desde que se publicó. No tiene Swing y toma su reloj de
 * afuera, así que se prueba sin esperar. Es seguro publicar desde cualquier hilo.
 *
 * <p>Stories: HU-39.
 */
public final class ToastQueue {

    /** Máximo de avisos en pantalla al mismo tiempo. */
    public static final int MAX_VISIBLE = 4;

    /** Milisegundos del desvanecimiento. */
    public static final int FADE_MS = 400;

    private record Shown(String text, ToastKind kind, long since) { }

    private record Pending(String text, ToastKind kind) { }

    private final LongSupplier clock;
    private final List<Shown> shown = new ArrayList<>();
    private final Deque<Pending> waiting = new ArrayDeque<>();

    /**
     * Crea una cola vacía.
     *
     * @param clock hora actual en milisegundos
     */
    public ToastQueue(LongSupplier clock) {
        this.clock = clock;
    }

    /** Agrega un aviso; se muestra de inmediato si hay lugar, y si no espera su turno. */
    public synchronized void post(ToastKind kind, String text) {
        waiting.add(new Pending(text, kind));
        refresh();
    }

    /** Los avisos que se dibujan ahora, del más antiguo al más nuevo. */
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

    /** Cuántos avisos esperan lugar. */
    public synchronized int waiting() {
        refresh();
        return waiting.size();
    }

    /** Verdadero cuando no se muestra nada y nada espera. */
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
