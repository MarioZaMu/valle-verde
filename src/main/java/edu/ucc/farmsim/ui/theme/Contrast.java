package edu.ucc.farmsim.ui.theme;

import java.awt.Color;

/**
 * WCAG contrast ratio between two colours, used to prove that the text of the interface is readable
 * (RNF-14: at least 4.5 : 1).
 *
 * <p>Stories: HU-43.
 */
public final class Contrast {

    private static final double LINEAR_THRESHOLD = 0.03928;
    private static final double OFFSET = 0.05;

    private Contrast() {
        // static helpers only
    }

    /** Contrast ratio from 1 (identical) to 21 (black on white); the order of the colours does not matter. */
    public static double ratio(Color first, Color second) {
        double a = luminance(first);
        double b = luminance(second);
        return (Math.max(a, b) + OFFSET) / (Math.min(a, b) + OFFSET);
    }

    private static double luminance(Color color) {
        return 0.2126 * channel(color.getRed()) + 0.7152 * channel(color.getGreen()) + 0.0722 * channel(color.getBlue());
    }

    private static double channel(int value) {
        double c = value / 255.0;
        return c <= LINEAR_THRESHOLD ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
