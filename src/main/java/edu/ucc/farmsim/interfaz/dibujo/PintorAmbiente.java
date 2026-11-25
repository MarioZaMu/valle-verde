package edu.ucc.farmsim.interfaz.dibujo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * La vida del valle que no es el juego: sombras de nubes que se deslizan sobre los campos, mariposas en primavera y
 * verano, hojas que caen en otoño, y un borde oscuro suave alrededor de la vista. Toda posición es una función pura del
 * reloj, así que no se guarda nada, nada es aleatorio y dos cuadros a la misma hora son iguales.
 *
 * <p>Stories: HU-41, HU-48.
 */
final class PintorAmbiente {

    private static final Color SOMBRA_NUBE = new Color(20, 30, 50, 26);
    private static final Color CUERPO = new Color(0x3A2A1A);
    private static final Color[] ALAS = {new Color(0xFFFFFF), new Color(0xFFD23F), new Color(0xF29A3A), new Color(0x8EC5FF)};
    private static final Color[] HOJAS = {new Color(0xC2571A), new Color(0xE07B24), new Color(0xD9A033), new Color(0x9E3A1A)};
    private static final int NUBES = 3;
    private static final int MARIPOSAS = 6;
    private static final int HOJAS_CAYENDO = 12;

    private PintorAmbiente() {
                // solo métodos de utilidad estáticos
    }

    /**
     * Pinta la vida ambiental sobre el mapa.
     *
     * @param g       dónde pintar
     * @param area    el mapa, en píxeles lógicos
     * @param celda    lado de una baldosa
     * @param tiempo    el reloj, en milisegundos
     * @param estacion  id de la estación
     * @param clima id del clima
     */
    static void paint(Graphics2D g, Rectangle area, int celda, long tiempo, String estacion, String clima) {
        boolean mojado = "RAINY".equals(clima) || "SNOWY".equals(clima);
        if (!mojado) {
            nubes(g, area, celda, tiempo);
        }
        if (!mojado && ("SPRING".equals(estacion) || "SUMMER".equals(estacion))) {
            mariposas(g, area, celda, tiempo);
        }
        if ("AUTUMN".equals(estacion) && !mojado) {
            hojas(g, area, celda, tiempo);
        }
    }

    /** Un borde oscuro alrededor de toda la vista, para que el ojo descanse en la granja del centro. */
    static void vineta(Graphics2D g, int width, int height) {
        float radio = (float) Math.hypot(width, height) / 2f;
        if (radio <= 0) {
            return;
        }
        g.setPaint(new RadialGradientPaint(new Point2D.Float(width / 2f, height / 2f), radio, new float[] {0.55f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(10, 8, 20, 90)}, MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillRect(0, 0, width, height);
    }

    private static void nubes(Graphics2D g, Rectangle area, int celda, long tiempo) {
        g.setColor(SOMBRA_NUBE);
        int tramo = area.width + 8 * celda;
        for (int k = 0; k < NUBES; k++) {
            double velocidad = celda / 90.0 * (1 + k * 0.35);
            int x = area.x - 4 * celda + (int) Math.floorMod((long) (tiempo / 1000.0 * velocidad + k * tramo / (double) NUBES), (long) tramo);
            int y = area.y + (int) (area.height * (0.12 + 0.3 * k));
            int w = celda * (4 + k);
            int h = celda * 2;
            g.fillOval(x, y, w, h);
            g.fillOval(x + w / 3, y - h / 4, w * 2 / 3, h);
        }
    }

    private static void mariposas(Graphics2D g, Rectangle area, int celda, long tiempo) {
        int p = Math.max(1, celda / 16);
        for (int i = 0; i < MARIPOSAS; i++) {
            double t = tiempo / 1000.0;
            double fx = frac(i * 0.37 + t * 0.012 * (1 + i % 3));
            double fy = frac(i * 0.61 + 0.07 * Math.sin(t * 0.7 + i));
            int x = area.x + (int) (fx * area.width);
            int y = area.y + (int) (fy * area.height + Math.sin(t * 3 + i * 2) * celda * 0.25);
            boolean abrir = ((tiempo / 140) + i) % 2 == 0;
            g.setColor(ALAS[i % ALAS.length]);
            if (abrir) {
                g.fillRect(x - 2 * p, y - p, 2 * p, 2 * p);
                g.fillRect(x + p, y - p, 2 * p, 2 * p);
            } else {
                g.fillRect(x - p, y - p, p, 2 * p);
                g.fillRect(x + p, y - p, p, 2 * p);
            }
            g.setColor(CUERPO);
            g.fillRect(x, y - p, p, 2 * p);
        }
    }

    private static void hojas(Graphics2D g, Rectangle area, int celda, long tiempo) {
        int p = Math.max(1, celda / 16);
        for (int i = 0; i < HOJAS_CAYENDO; i++) {
            double t = tiempo / 1000.0;
            double fy = frac(i * 0.29 + t * 0.05 * (1 + i % 2));
            double deriva = frac(i * 0.53 + t * 0.02) + Math.sin(t * 1.5 + i) * 0.02;
            int x = area.x + (int) (frac(deriva) * area.width);
            int y = area.y + (int) (fy * area.height);
            g.setColor(HOJAS[i % HOJAS.length]);
            boolean plano = ((tiempo / 300) + i) % 2 == 0;
            g.fillRect(x, y, plano ? 3 * p : 2 * p, plano ? p : 2 * p);
        }
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }
}
