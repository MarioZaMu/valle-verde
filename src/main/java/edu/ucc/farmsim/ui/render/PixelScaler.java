package edu.ucc.farmsim.ui.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scales pixel art so it stays sharp at any size. The image is first enlarged by a whole factor with nearest
 * neighbour (every art pixel becomes an exact square) and only then brought to the final size with bilinear
 * filtering, which blurs at most the one-pixel seams between squares. At a whole scale the result is the
 * nearest-neighbour image itself, with no colour other than the original ones (HU-44 · CA-3). The scaled
 * copies are kept, so a frame only draws ready images.
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

    /** A scaler that keeps up to 600 scaled copies: enough for every sprite at a few sizes. */
    PixelScaler() {
        this(MAX_ENTRIES);
    }

    /** A scaler that keeps at most {@code maxEntries} scaled copies; few for large images such as the ground. */
    PixelScaler(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /** The image at the given size in device pixels. */
    BufferedImage scaled(BufferedImage source, int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (w == source.getWidth() && h == source.getHeight()) {
            return source;
        }
        return cache.computeIfAbsent(new Key(source, w, h), key -> scale(source, w, h));
    }

    /**
     * Draws an image so that it covers the given logical rectangle, scaled for the device of {@code g}.
     *
     * @param g      where to draw
     * @param source the art, at its own size
     * @param x      left, in logical pixels
     * @param y      top, in logical pixels
     * @param width  width, in logical pixels (negative mirrors the image)
     * @param height height, in logical pixels
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

    /** How many device pixels one logical pixel of {@code g} covers. */
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
