package edu.ucc.farmsim.interfaz.componentes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Las teclas de caminar que se mantienen pulsadas, y cuándo da el avatar su siguiente paso (RN-40). Pulsar una tecla
 * estando quieto da un paso de inmediato; mientras las teclas siguen pulsadas el avatar vuelve a dar un paso cada
 * {@code msPaso}, justo cuando termina el paso anterior, de modo que la caminata nunca se detiene ni da tirones. Una
 * tecla pulsada a mitad de un paso espera a que ese paso termine. Se ignora la repetición de teclas del sistema. Con
 * varias teclas pulsadas lidera la última presionada; con dos teclas perpendiculares (como W y D) los pasos se alternan
 * entre ellas, lo que camina en diagonal. Soltar una tecla continúa con las que siguen pulsadas. No lleva reloj propio:
 * quien llama pasa la hora.
 *
 * <p>Stories: HU-50.
 */
public final class TeclasCaminar {

    private final long msPaso;
    private final Deque<String> sostenido = new ArrayDeque<>();
    private long next;
    private String ultimo;

    /**
     * Crea las teclas.
     *
     * @param msPaso cuánto dura un paso, en milisegundos
     */
    public TeclasCaminar(long msPaso) {
        this.msPaso = msPaso;
    }

    /**
     * Una tecla de caminar se pulsó.
     *
     * @param direccion la dirección de la tecla
     * @param now       la hora, en milisegundos
     * @return la dirección a la que dar el paso ahora, o {@code null} cuando la tecla ya estaba pulsada (una
     *         repetición) o un paso sigue en curso (la tecla cuenta entonces desde el siguiente paso)
     */
    public String pulsar(String direccion, long now) {
        if (sostenido.contains(direccion)) {
            return null;
        }
        boolean parado = sostenido.isEmpty();
        sostenido.addLast(direccion);
        if (!parado && now < next) {
            return null;
        }
        next = now + msPaso;
        ultimo = direccion;
        return direccion;
    }

    /** Una tecla de caminar se soltó. */
    public void liberar(String direccion) {
        sostenido.remove(direccion);
    }

    /** Olvida todas las teclas, para cuando la ventana pierde el teclado y nunca oiría que se sueltan. */
    public void clear() {
        sostenido.clear();
    }

    /**
     * Se llama a menudo mientras se camina.
     *
     * @param now la hora, en milisegundos
     * @return la dirección del siguiente paso cuando llega su hora, o {@code null}
     */
    public String tic(long now) {
        if (sostenido.isEmpty() || now < next) {
            return null;
        }
        next = now - next >= msPaso ? now + msPaso : next + msPaso;
        String lider = sostenido.peekLast();
        String companero = alternativa(lider);
        ultimo = companero != null && lider.equals(ultimo) ? companero : lider;
        return ultimo;
    }

    /**
     * La tecla pulsada que se prueba cuando un paso hacia {@code direccion} está bloqueado: la más reciente en ángulo
     * recto con él, o {@code null}. El paso que se da en su lugar cuenta para el zigzag.
     */
    public String alternativa(String direccion) {
        for (Iterator<String> it = sostenido.descendingIterator(); it.hasNext();) {
            String otro = it.next();
            if (vertical(otro) != vertical(direccion)) {
                return otro;
            }
        }
        return null;
    }

    /** Avisa que el paso fue hacia {@code direccion} en su lugar (el planeado estaba bloqueado). */
    public void enLugarPisado(String direccion) {
        ultimo = direccion;
    }

    /** Verdadero mientras hay una tecla de caminar pulsada. */
    public boolean esCaminando() {
        return !sostenido.isEmpty();
    }

    private static boolean vertical(String direccion) {
        return "NORTH".equals(direccion) || "SOUTH".equals(direccion);
    }
}
