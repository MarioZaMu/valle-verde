package edu.ucc.farmsim.ui.theme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;

/**
 * El aspecto de los paneles y botones del juego: un marco de madera cálida con un contorno oscuro alrededor de una hoja
 * de pergamino, y botones de madera que se iluminan y suben bajo el cursor. Todo se pinta con Java2D, así que no
 * necesita imágenes y se ve igual con o sin el paquete de sprites.
 *
 * <p>Stories: HU-43, HU-48.
 */
public final class WoodStyle {

    /** Contorno oscuro de cada marco. */
    public static final Color OUTLINE = new Color(0x2E1A0C);
    /** Madera en sombra. */
    public static final Color WOOD_DARK = new Color(0x7A4A22);
    /** Madera. */
    public static final Color WOOD = new Color(0xA8692F);
    /** Madera a la luz. */
    public static final Color WOOD_LIGHT = new Color(0xD08E48);
    /** Pergamino dentro de un marco. */
    public static final Color PARCHMENT = new Color(0xF6E3B4);
    /** Pergamino en sombra, a lo largo del borde interior de un marco. */
    public static final Color PARCHMENT_SHADOW = new Color(0xE2C487);
    /** Texto sobre pergamino. */
    public static final Color INK = new Color(0x3B2412);
    /** Texto sobre madera. */
    public static final Color CREAM = new Color(0xFFF4D6);
    /** Marco de la casilla elegida. */
    public static final Color SELECTED = new Color(0xE0502A);
    /** Resplandor alrededor de la casilla elegida. */
    public static final Color GLOW = new Color(0xFFD23F);

    /** Grosor del marco de madera, en píxeles. */
    public static final int FRAME = 5;

    private static final int ARC = 10;
    private static final Color HOVER_LIGHT = new Color(255, 255, 255, 60);

    private WoodStyle() {
                // solo métodos de utilidad estáticos
    }

    /** Pinta un marco de madera alrededor de pergamino que cubre el rectángulo dado. */
    public static void paintFrame(Graphics2D g, int x, int y, int w, int h) {
        if (w <= 2 * FRAME || h <= 2 * FRAME) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(x + 2, y + 3, w - 2, h - 2, ARC, ARC);
        g.setColor(OUTLINE);
        g.fillRoundRect(x, y, w - 2, h - 3, ARC, ARC);
        g.setPaint(new GradientPaint(x, y, WOOD_LIGHT, x, y + h, WOOD_DARK));
        g.fillRoundRect(x + 2, y + 2, w - 6, h - 7, ARC - 2, ARC - 2);
        g.setColor(WOOD_LIGHT.brighter());
        g.drawLine(x + ARC / 2 + 2, y + 3, x + w - ARC / 2 - 6, y + 3);
        int ix = x + FRAME;
        int iy = y + FRAME;
        int iw = w - 2 * FRAME - 2;
        int ih = h - 2 * FRAME - 3;
        g.setColor(WOOD_DARK.darker());
        g.fillRect(ix - 1, iy - 1, iw + 2, ih + 2);
        g.setColor(PARCHMENT);
        g.fillRect(ix, iy, iw, ih);
        g.setColor(PARCHMENT_SHADOW);
        g.fillRect(ix, iy, iw, 2);
        g.fillRect(ix, iy, 2, ih);
        g.setColor(WOOD_LIGHT.brighter());
        for (int[] nail : new int[][] {{x + 3, y + 3}, {x + w - 7, y + 3}, {x + 3, y + h - 8}, {x + w - 7, y + h - 8}}) {
            g.fillRect(nail[0], nail[1], 2, 2);
        }
    }

    /**
     * Pinta el cuerpo de un botón. Un botón simple es una tabla de madera; una casilla es un cuadrado de pergamino en
     * un marco, como en una barra de herramientas, y recibe un marco brillante cuando es la elegida.
     *
     * @param g        dónde pintar
     * @param b        el botón
     * @param hover    qué tan iluminado está, de 0 (cursor lejos) a 1 (cursor encima)
     * @param slot     verdadero para una casilla de la barra de herramientas
     */
    public static void paintButton(Graphics2D g, AbstractButton b, float hover, boolean slot) {
        int w = b.getWidth();
        int h = b.getHeight();
        boolean pressed = b.getModel().isArmed() && b.getModel().isPressed();
        boolean chosen = b.isSelected();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (slot) {
            paintFrame(g, 0, 0, w, h);
            if (chosen) {
                g.setColor(GLOW);
                g.fillRoundRect(FRAME - 2, FRAME - 2, w - 2 * FRAME, h - 2 * FRAME - 1, 6, 6);
                g.setColor(PARCHMENT);
                g.fillRect(FRAME + 2, FRAME + 2, w - 2 * FRAME - 6, h - 2 * FRAME - 7);
                g.setColor(SELECTED);
                g.drawRoundRect(FRAME - 2, FRAME - 2, w - 2 * FRAME, h - 2 * FRAME - 1, 6, 6);
            }
        } else {
            int drop = pressed ? 1 : 3;
            int top = pressed ? 2 : 0;
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(1, 3, w - 2, h - 3, ARC, ARC);
            g.setColor(OUTLINE);
            g.fillRoundRect(0, top, w - 1, h - 1 - top, ARC, ARC);
            g.setColor(WOOD_DARK);
            g.fillRoundRect(2, top + 2, w - 5, h - 5 - top, ARC - 2, ARC - 2);
            Color light = b.isEnabled() ? WOOD_LIGHT : WOOD;
            g.setPaint(new GradientPaint(0, top, light, 0, h - drop, WOOD));
            g.fillRoundRect(2, top + 2, w - 5, h - 5 - top - drop, ARC - 2, ARC - 2);
            g.setColor(new Color(255, 230, 180, 120));
            g.drawLine(6, top + 3, w - 8, top + 3);
            g.setColor(new Color(90, 50, 20, 70));
            for (int y = top + 9; y < h - drop - 4; y += 7) {
                g.drawLine(6, y, w / 3, y);
                g.drawLine(w / 2, y + 3, w - 9, y + 3);
            }
        }
        if (hover > 0f && b.isEnabled()) {
            g.setColor(new Color(255, 255, 255, Math.round(HOVER_LIGHT.getAlpha() * hover)));
            g.fillRoundRect(2, 2, w - 5, h - 6, ARC - 2, ARC - 2);
        }
    }

    /** Deja un botón de Swing listo para pintarse con {@link #paintButton}. */
    public static void prepare(AbstractButton b, boolean slot) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFocusable(false);
        b.setForeground(slot ? INK : CREAM);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(slot ? BorderFactory.createEmptyBorder(FRAME + 4, FRAME + 8, FRAME + 6, FRAME + 8)
                : BorderFactory.createEmptyBorder(8, 18, 11, 18));
    }
}
