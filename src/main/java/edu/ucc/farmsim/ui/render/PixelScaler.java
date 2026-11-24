package edu.ucc.farmsim.ui.render;

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
final class PixelScaler {

    private static final int MAX_ENTRIES = 600;

    private final int maxEntries;

    private record Key(BufferedImage image, int width, int height) {
    }

    private final Map<Key, BufferedImage> cache = new LinkedHashMap<>(64, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> eldest) {
            return size() > maxEntries;
        }
    };

    /** Un escalador que conserva hasta 600 copias escaladas: suficiente para cada sprite en unos pocos tamaños. */
    PixelScaler() {
        this(MAX_ENTRIES);
    }

    /**
     * Un escalador que conserva como mucho {@code maxEntries} copias escaladas; pocas para imágenes grandes como el
     * suelo.
     */
    PixelScaler(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /** La imagen al tamaño dado en píxeles de dispositivo. */
    BufferedImage scaled(BufferedImage source, int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (w == source.getWidth() && h == source.getHeight()) {
            return source;
        }
        return cache.computeIfAbsent(new Key(source, w, h), key -> scale(source, w, h));
    }

    /**
     * Dibuja una imagen de modo que cubra el rectángulo lógico dado, escalada para el dispositivo de {@code g}.
     *
     * @param g      dónde dibujar
     * @param source el arte, a su propio tamaño
     * @param x      izquierda, en píxeles lógicos
     * @param y      arriba, en píxeles lógicos
     * @param width  ancho, en píxeles lógicos (negativo refleja la imagen)
     * @param height alto, en píxeles lógicos
     */
    void draw(Graphics2D g, BufferedImage source, int x, int y, int width, int height) {
        double device = deviceScale(g);
        int w = Math.abs(width);
        BufferedImage ready = scaled(source, (int) Math.round(w * device), (int) Math.round(height * device));
        if (width < 0) {
            g.drawImage(ready, x + w, y, -w, height, null);
        } else {
            g.drawImage(ready, x, y, w, height, null);
        }
    }

    /** Cuántos píxeles de dispositivo cubre un píxel lógico de {@code g}. */
    static double deviceScale(Graphics2D g) {
        double scale = g.getTransform().getScaleX();
        return scale > 0 ? scale : 1.0;
    }

    private static BufferedImage scale(BufferedImage source, int w, int h) {
        int factor = (int) Math.max(1, Math.ceil(Math.max(w / (double) source.getWidth(), h / (double) source.getHeight())));
        BufferedImage big = new BufferedImage(source.getWidth() * factor, source.getHeight() * factor,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = big.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source, 0, 0, big.getWidth(), big.getHeight(), null);
        g.dispose();
        if (big.getWidth() == w && big.getHeight() == h) {
            return big;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D o = out.createGraphics();
        o.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        o.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        o.drawImage(big, 0, 0, w, h, null);
        o.dispose();
        return out;
    }
}
