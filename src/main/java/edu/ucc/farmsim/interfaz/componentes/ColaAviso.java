package edu.ucc.farmsim.interfaz.componentes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Los avisos de la pantalla: como máximo {@link #MAXIMO_VISIBLE} a la vez, el resto esperando en orden (HU-39 · CA-3). Un
 * aviso se queda {@link CategoriaAviso#msDuracion()} totalmente visible y luego se desvanece durante {@link #MS_DESVANECER}; el
 * tiempo cuenta desde el momento en que se vuelve visible, no desde que se publicó. No tiene Swing y toma su reloj de
 * afuera, así que se prueba sin esperar. Es seguro publicar desde cualquier hilo.
 *
 * <p>Stories: HU-39.
 */
public final class ColaAviso {

    /** Máximo de avisos en pantalla al mismo tiempo. */
    public static final int MAXIMO_VISIBLE = 4;

    /** Milisegundos del desvanecimiento. */
    public static final int MS_DESVANECER = 400;

    private record Mostrado(String texto, CategoriaAviso categoria, long desde) { }

    private record Pendiente(String texto, CategoriaAviso categoria) { }

    private final LongSupplier reloj;
    private final List<Mostrado> mostrado = new ArrayList<>();
    private final Deque<Pendiente> esperando = new ArrayDeque<>();

    /**
     * Crea una cola vacía.
     *
     * @param reloj hora actual en milisegundos
     */
    public ColaAviso(LongSupplier reloj) {
        this.reloj = reloj;
    }

    /** Agrega un aviso; se muestra de inmediato si hay lugar, y si no espera su turno. */
    public synchronized void post(CategoriaAviso categoria, String texto) {
        esperando.add(new Pendiente(texto, categoria));
        refrescar();
    }

    /** Los avisos que se dibujan ahora, del más antiguo al más nuevo. */
    public synchronized List<VistaAviso> visible() {
        refrescar();
        long now = reloj.getAsLong();
        List<VistaAviso> vistas = new ArrayList<>();
        for (Mostrado aviso : mostrado) {
            long edad = now - aviso.desde();
            float opacidad = edad <= aviso.categoria().msDuracion()
                    ? 1f
                    : Math.max(0f, 1f - (float) (edad - aviso.categoria().msDuracion()) / MS_DESVANECER);
            vistas.add(new VistaAviso(aviso.texto(), aviso.categoria(), opacidad));
        }
        return vistas;
    }

    /** Cuántos avisos esperan lugar. */
    public synchronized int esperando() {
        refrescar();
        return esperando.size();
    }

    /** Verdadero cuando no se muestra nada y nada espera. */
    public synchronized boolean esOcioso() {
        refrescar();
        return mostrado.isEmpty() && esperando.isEmpty();
    }

    private void refrescar() {
        long now = reloj.getAsLong();
        Iterator<Mostrado> iterator = mostrado.iterator();
        while (iterator.hasNext()) {
            Mostrado aviso = iterator.next();
            if (now - aviso.desde() >= aviso.categoria().msDuracion() + MS_DESVANECER) {
                iterator.remove();
            }
        }
        while (mostrado.size() < MAXIMO_VISIBLE && !esperando.isEmpty()) {
            Pendiente next = esperando.poll();
            mostrado.add(new Mostrado(next.texto(), next.categoria(), now));
        }
    }
}
