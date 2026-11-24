package edu.ucc.farmsim.ui.components;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Las teclas de caminar que se mantienen pulsadas, y cuándo da el avatar su siguiente paso (RN-40). Pulsar una tecla
 * estando quieto da un paso de inmediato; mientras las teclas siguen pulsadas el avatar vuelve a dar un paso cada
 * {@code stepMs}, justo cuando termina el paso anterior, de modo que la caminata nunca se detiene ni da tirones. Una
 * tecla pulsada a mitad de un paso espera a que ese paso termine. Se ignora la repetición de teclas del sistema. Con
 * varias teclas pulsadas lidera la última presionada; con dos teclas perpendiculares (como W y D) los pasos se alternan
 * entre ellas, lo que camina en diagonal. Soltar una tecla continúa con las que siguen pulsadas. No lleva reloj propio:
 * quien llama pasa la hora.
 *
 * <p>Stories: HU-50.
 */
public final class WalkKeys {

    private final long stepMs;
    private final Deque<String> held = new ArrayDeque<>();
    private long next;
    private String last;

    /**
     * Crea las teclas.
     *
     * @param stepMs cuánto dura un paso, en milisegundos
     */
    public WalkKeys(long stepMs) {
        this.stepMs = stepMs;
    }

    /**
     * Una tecla de caminar se pulsó.
     *
     * @param direction la dirección de la tecla
     * @param now       la hora, en milisegundos
     * @return la dirección a la que dar el paso ahora, o {@code null} cuando la tecla ya estaba pulsada (una
     *         repetición) o un paso sigue en curso (la tecla cuenta entonces desde el siguiente paso)
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

    /** Una tecla de caminar se soltó. */
    public void release(String direction) {
        held.remove(direction);
    }

    /** Olvida todas las teclas, para cuando la ventana pierde el teclado y nunca oiría que se sueltan. */
    public void clear() {
        held.clear();
    }

    /**
     * Se llama a menudo mientras se camina.
     *
     * @param now la hora, en milisegundos
     * @return la dirección del siguiente paso cuando llega su hora, o {@code null}
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
     * La tecla pulsada que se prueba cuando un paso hacia {@code direction} está bloqueado: la más reciente en ángulo
     * recto con él, o {@code null}. El paso que se da en su lugar cuenta para el zigzag.
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

    /** Avisa que el paso fue hacia {@code direction} en su lugar (el planeado estaba bloqueado). */
    public void steppedInstead(String direction) {
        last = direction;
    }

    /** Verdadero mientras hay una tecla de caminar pulsada. */
    public boolean isWalking() {
        return !held.isEmpty();
    }

    private static boolean vertical(String direction) {
        return "NORTH".equals(direction) || "SOUTH".equals(direction);
    }
}
