package edu.ucc.farmsim.ui.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * The life of the valley that is not the game: shadows of clouds sliding over the fields, butterflies in
 * spring and summer, leaves falling in autumn, and a soft dark edge around the view. Every position is a pure
 * function of the clock, so nothing is stored, nothing is random and two frames at the same time are equal.
 *
 * <p>Stories: HU-41, HU-48.
 */
final class AmbientPainter {

    private static final Color CLOUD_SHADOW = new Color(20, 30, 50, 26);
    private static final Color BODY = new Color(0x3A2A1A);
    private static final Color[] WINGS = {new Color(0xFFFFFF), new Color(0xFFD23F), new Color(0xF29A3A), new Color(0x8EC5FF)};
    private static final Color[] LEAVES = {new Color(0xC2571A), new Color(0xE07B24), new Color(0xD9A033), new Color(0x9E3A1A)};
    private static final int CLOUDS = 3;
    private static final int BUTTERFLIES = 6;
    private static final int FALLING_LEAVES = 12;

    private AmbientPainter() {
        // static helpers only
    }

    /**
     * Paints the ambient life over the map.
     *
     * @param g       where to paint
     * @param area    the map, in logical pixels
     * @param cell    side of a tile
     * @param time    the clock, in milliseconds
     * @param season  season id
     * @param weather weather id
     */
    static void paint(Graphics2D g, Rectangle area, int cell, long time, String season, String weather) {
        boolean wet = "RAINY".equals(weather) || "SNOWY".equals(weather);
        if (!wet) {
            clouds(g, area, cell, time);
        }
        if (!wet && ("SPRING".equals(season) || "SUMMER".equals(season))) {
            butterflies(g, area, cell, time);
        }
        if ("AUTUMN".equals(season) && !wet) {
            leaves(g, area, cell, time);
        }
    }

    /** A dark edge around the whole view, so the eye rests on the farm in the middle. */
    static void vignette(Graphics2D g, int width, int height) {
        float radius = (float) Math.hypot(width, height) / 2f;
        if (radius <= 0) {
            return;
        }
        g.setPaint(new RadialGradientPaint(new Point2D.Float(width / 2f, height / 2f), radius, new float[] {0.55f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(10, 8, 20, 90)}, MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillRect(0, 0, width, height);
    }

    private static void clouds(Graphics2D g, Rectangle area, int cell, long time) {
        g.setColor(CLOUD_SHADOW);
        int span = area.width + 8 * cell;
        for (int k = 0; k < CLOUDS; k++) {
            double speed = cell / 90.0 * (1 + k * 0.35);
            int x = area.x - 4 * cell + (int) Math.floorMod((long) (time / 1000.0 * speed + k * span / (double) CLOUDS), (long) span);
            int y = area.y + (int) (area.height * (0.12 + 0.3 * k));
            int w = cell * (4 + k);
            int h = cell * 2;
            g.fillOval(x, y, w, h);
            g.fillOval(x + w / 3, y - h / 4, w * 2 / 3, h);
        }
    }

    private static void butterflies(Graphics2D g, Rectangle area, int cell, long time) {
        int p = Math.max(1, cell / 16);
        for (int i = 0; i < BUTTERFLIES; i++) {
            double t = time / 1000.0;
            double fx = frac(i * 0.37 + t * 0.012 * (1 + i % 3));
            double fy = frac(i * 0.61 + 0.07 * Math.sin(t * 0.7 + i));
            int x = area.x + (int) (fx * area.width);
            int y = area.y + (int) (fy * area.height + Math.sin(t * 3 + i * 2) * cell * 0.25);
            boolean open = ((time / 140) + i) % 2 == 0;
            g.setColor(WINGS[i % WINGS.length]);
            if (open) {
                g.fillRect(x - 2 * p, y - p, 2 * p, 2 * p);
                g.fillRect(x + p, y - p, 2 * p, 2 * p);
            } else {
                g.fillRect(x - p, y - p, p, 2 * p);
                g.fillRect(x + p, y - p, p, 2 * p);
            }
            g.setColor(BODY);
            g.fillRect(x, y - p, p, 2 * p);
        }
    }

    private static void leaves(Graphics2D g, Rectangle area, int cell, long time) {
        int p = Math.max(1, cell / 16);
        for (int i = 0; i < FALLING_LEAVES; i++) {
            double t = time / 1000.0;
            double fy = frac(i * 0.29 + t * 0.05 * (1 + i % 2));
            double drift = frac(i * 0.53 + t * 0.02) + Math.sin(t * 1.5 + i) * 0.02;
            int x = area.x + (int) (frac(drift) * area.width);
            int y = area.y + (int) (fy * area.height);
            g.setColor(LEAVES[i % LEAVES.length]);
            boolean flat = ((time / 300) + i) % 2 == 0;
            g.fillRect(x, y, flat ? 3 * p : 2 * p, flat ? p : 2 * p);
        }
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }
}
