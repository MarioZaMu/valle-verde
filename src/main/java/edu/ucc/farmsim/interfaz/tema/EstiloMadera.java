package edu.ucc.farmsim.interfaz.tema;

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
public final class EstiloMadera {

    /** Contorno oscuro de cada marco. */
    public static final Color CONTORNO = new Color(0x2E1A0C);
    /** Madera en sombra. */
    public static final Color MADERA_OSCURA = new Color(0x7A4A22);
    /** Madera. */
    public static final Color MADERA = new Color(0xA8692F);
    /** Madera a la luz. */
    public static final Color LUZ_MADERA = new Color(0xD08E48);
    /** Pergamino dentro de un marco. */
    public static final Color PERGAMINO = new Color(0xF6E3B4);
    /** Pergamino en sombra, a lo largo del borde interior de un marco. */
    public static final Color SOMBRA_PERGAMINO = new Color(0xE2C487);
    /** Texto sobre pergamino. */
    public static final Color TINTA = new Color(0x3B2412);
    /** Texto sobre madera. */
    public static final Color CREMA = new Color(0xFFF4D6);
    /** Marco de la casilla elegida. */
    public static final Color SELECCIONADO = new Color(0xE0502A);
    /** Resplandor alrededor de la casilla elegida. */
    public static final Color BRILLO = new Color(0xFFD23F);

    /** Grosor del marco de madera, en píxeles. */
    public static final int MARCO = 5;

    private static final int ARCO = 10;
    private static final Color LUZ_SOBREVOLAR = new Color(255, 255, 255, 60);

    private EstiloMadera() {
                // solo métodos de utilidad estáticos
    }

    /** Pinta un marco de madera alrededor de pergamino que cubre el rectángulo dado. */
    public static void marcoPintar(Graphics2D g, int x, int y, int w, int h) {
        if (w <= 2 * MARCO || h <= 2 * MARCO) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(x + 2, y + 3, w - 2, h - 2, ARCO, ARCO);
        g.setColor(CONTORNO);
        g.fillRoundRect(x, y, w - 2, h - 3, ARCO, ARCO);
        g.setPaint(new GradientPaint(x, y, LUZ_MADERA, x, y + h, MADERA_OSCURA));
        g.fillRoundRect(x + 2, y + 2, w - 6, h - 7, ARCO - 2, ARCO - 2);
        g.setColor(LUZ_MADERA.brighter());
        g.drawLine(x + ARCO / 2 + 2, y + 3, x + w - ARCO / 2 - 6, y + 3);
        int ix = x + MARCO;
        int iy = y + MARCO;
        int iw = w - 2 * MARCO - 2;
        int ih = h - 2 * MARCO - 3;
        g.setColor(MADERA_OSCURA.darker());
        g.fillRect(ix - 1, iy - 1, iw + 2, ih + 2);
        g.setColor(PERGAMINO);
        g.fillRect(ix, iy, iw, ih);
        g.setColor(SOMBRA_PERGAMINO);
        g.fillRect(ix, iy, iw, 2);
        g.fillRect(ix, iy, 2, ih);
        g.setColor(LUZ_MADERA.brighter());
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
     * @param sobrevolar    qué tan iluminado está, de 0 (cursor lejos) a 1 (cursor encima)
     * @param ranura     verdadero para una casilla de la barra de herramientas
     */
    public static void botonPintar(Graphics2D g, AbstractButton b, float sobrevolar, boolean ranura) {
        int w = b.getWidth();
        int h = b.getHeight();
        boolean pulsado = b.getModel().isArmed() && b.getModel().isPressed();
        boolean elegido = b.isSelected();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (ranura) {
            marcoPintar(g, 0, 0, w, h);
            if (elegido) {
                g.setColor(BRILLO);
                g.fillRoundRect(MARCO - 2, MARCO - 2, w - 2 * MARCO, h - 2 * MARCO - 1, 6, 6);
                g.setColor(PERGAMINO);
                g.fillRect(MARCO + 2, MARCO + 2, w - 2 * MARCO - 6, h - 2 * MARCO - 7);
                g.setColor(SELECCIONADO);
                g.drawRoundRect(MARCO - 2, MARCO - 2, w - 2 * MARCO, h - 2 * MARCO - 1, 6, 6);
            }
        } else {
            int drop = pulsado ? 1 : 3;
            int arriba = pulsado ? 2 : 0;
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(1, 3, w - 2, h - 3, ARCO, ARCO);
            g.setColor(CONTORNO);
            g.fillRoundRect(0, arriba, w - 1, h - 1 - arriba, ARCO, ARCO);
            g.setColor(MADERA_OSCURA);
            g.fillRoundRect(2, arriba + 2, w - 5, h - 5 - arriba, ARCO - 2, ARCO - 2);
            Color luz = b.isEnabled() ? LUZ_MADERA : MADERA;
            g.setPaint(new GradientPaint(0, arriba, luz, 0, h - drop, MADERA));
            g.fillRoundRect(2, arriba + 2, w - 5, h - 5 - arriba - drop, ARCO - 2, ARCO - 2);
            g.setColor(new Color(255, 230, 180, 120));
            g.drawLine(6, arriba + 3, w - 8, arriba + 3);
            g.setColor(new Color(90, 50, 20, 70));
            for (int y = arriba + 9; y < h - drop - 4; y += 7) {
                g.drawLine(6, y, w / 3, y);
                g.drawLine(w / 2, y + 3, w - 9, y + 3);
            }
        }
        if (sobrevolar > 0f && b.isEnabled()) {
            g.setColor(new Color(255, 255, 255, Math.round(LUZ_SOBREVOLAR.getAlpha() * sobrevolar)));
            g.fillRoundRect(2, 2, w - 5, h - 6, ARCO - 2, ARCO - 2);
        }
    }

    /** Deja un botón de Swing listo para pintarse con {@link #botonPintar}. */
    public static void preparar(AbstractButton b, boolean ranura) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFocusable(false);
        b.setForeground(ranura ? TINTA : CREMA);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(ranura ? BorderFactory.createEmptyBorder(MARCO + 4, MARCO + 8, MARCO + 6, MARCO + 8)
                : BorderFactory.createEmptyBorder(8, 18, 11, 18));
    }
}
