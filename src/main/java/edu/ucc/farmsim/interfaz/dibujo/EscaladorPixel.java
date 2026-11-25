package edu.ucc.farmsim.interfaz.dibujo;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Escala el pixel art para que se mantenga nítido a cualquier tamaño. La imagen primero se agranda por un factor entero
 * con vecino más cercano (cada píxel del arte se vuelve un cuadrado exacto) y solo después se lleva al tamaño final con
 * filtrado bilineal, que como mucho difumina las costuras de un píxel entre cuadrados. En una escala entera el
 * resultado es la propia imagen de vecino más cercano, sin ningún color distinto de los originales (HU-44 · CA-3). Las
 * copias escaladas se conservan, así que un cuadro solo dibuja imágenes listas.
 *
 * <p>Stories: HU-44, HU-48.
 */
final class EscaladorPixel {

    private static final int ENTRADAS_MAXIMOS = 600;

    private final int entradasMaximos;

    private record Clave(BufferedImage imagen, int width, int height) {
    }

    private final Map<Clave, BufferedImage> cache = new LinkedHashMap<>(64, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Clave, BufferedImage> masAntiguo) {
            return size() > entradasMaximos;
        }
    };

    /** Un escalador que conserva hasta 600 copias escaladas: suficiente para cada sprite en unos pocos tamaños. */
    EscaladorPixel() {
        this(ENTRADAS_MAXIMOS);
    }

    /**
     * Un escalador que conserva como mucho {@code entradasMaximos} copias escaladas; pocas para imágenes grandes como el
     * suelo.
     */
    EscaladorPixel(int entradasMaximos) {
        this.entradasMaximos = entradasMaximos;
    }

    /** La imagen al tamaño dado en píxeles de dispositivo. */
    BufferedImage escalado(BufferedImage fuente, int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (w == fuente.getWidth() && h == fuente.getHeight()) {
            return fuente;
        }
        return cache.computeIfAbsent(new Clave(fuente, w, h), clave -> scale(fuente, w, h));
    }

    /**
     * Dibuja una imagen de modo que cubra el rectángulo lógico dado, escalada para el dispositivo de {@code g}.
     *
     * @param g      dónde dibujar
     * @param fuente el arte, a su propio tamaño
     * @param x      izquierda, en píxeles lógicos
     * @param y      arriba, en píxeles lógicos
     * @param width  ancho, en píxeles lógicos (negativo refleja la imagen)
     * @param height alto, en píxeles lógicos
     */
    void draw(Graphics2D g, BufferedImage fuente, int x, int y, int width, int height) {
        double dispositivo = escalaDispositivo(g);
        int w = Math.abs(width);
        BufferedImage listo = escalado(fuente, (int) Math.round(w * dispositivo), (int) Math.round(height * dispositivo));
        if (width < 0) {
            g.drawImage(listo, x + w, y, -w, height, null);
        } else {
            g.drawImage(listo, x, y, w, height, null);
        }
    }

    /** Cuántos píxeles de dispositivo cubre un píxel lógico de {@code g}. */
    static double escalaDispositivo(Graphics2D g) {
        double scale = g.getTransform().getScaleX();
        return scale > 0 ? scale : 1.0;
    }

    private static BufferedImage scale(BufferedImage fuente, int w, int h) {
        int factor = (int) Math.max(1, Math.ceil(Math.max(w / (double) fuente.getWidth(), h / (double) fuente.getHeight())));
        BufferedImage grande = new BufferedImage(fuente.getWidth() * factor, fuente.getHeight() * factor,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = grande.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(fuente, 0, 0, grande.getWidth(), grande.getHeight(), null);
        g.dispose();
        if (grande.getWidth() == w && grande.getHeight() == h) {
            return grande;
        }
        BufferedImage fuera = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D o = fuera.createGraphics();
        o.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        o.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        o.drawImage(grande, 0, 0, w, h, null);
        o.dispose();
        return fuera;
    }
}
