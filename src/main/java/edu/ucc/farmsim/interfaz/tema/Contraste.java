package edu.ucc.farmsim.interfaz.tema;

import java.awt.Color;

/**
 * Razón de contraste WCAG entre dos colores, usada para demostrar que el texto de la interfaz es legible (RNF-14: al
 * menos 4,5 : 1).
 *
 * <p>Stories: HU-43.
 */
public final class Contraste {

    private static final double UMBRAL_LINEAL = 0.03928;
    private static final double DESFASE = 0.05;

    private Contraste() {
                // solo métodos de utilidad estáticos
    }

    /** Razón de contraste de 1 (idénticos) a 21 (negro sobre blanco); el orden de los colores no importa. */
    public static double razon(Color first, Color segundo) {
        double a = luminancia(first);
        double b = luminancia(segundo);
        return (Math.max(a, b) + DESFASE) / (Math.min(a, b) + DESFASE);
    }

    private static double luminancia(Color color) {
        return 0.2126 * canal(color.getRed()) + 0.7152 * canal(color.getGreen()) + 0.0722 * canal(color.getBlue());
    }

    private static double canal(int value) {
        double c = value / 255.0;
        return c <= UMBRAL_LINEAL ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
