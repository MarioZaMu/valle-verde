package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JProgressBar;

/**
 * La energía del granjero como una barra en un marco de madera: verde cuando hay de sobra, amarilla cuando escasea y
 * roja cuando queda poca, con la cantidad escrita encima.
 *
 * <p>Stories: HU-34, HU-48.
 */
public class BarraEnergia extends JProgressBar {

    private static final long serialVersionUID = 1L;
    private static final Color PISTA = new Color(0x3B2412);
    private static final Color MUCHO = new Color(0x5FB83A);
    private static final Color BAJO = new Color(0xF2C14E);
    private static final Color POCO = new Color(0xE0502A);
    private static final Color SOMBRA = new Color(0, 0, 0, 150);
    private static final double MITAD = 0.5;
    private static final double CUARTO = 0.25;

    /**
     * Crea la barra.
     *
     * @param max el valor más grande
     */
    public BarraEnergia(int max) {
        super(0, max);
        setStringPainted(true);
        setOpaque(false);
        setBorderPainted(false);
        setFont(getFont().deriveFont(Font.BOLD));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metricas = getFontMetrics(getFont());
        return new Dimension(Math.max(170, metricas.stringWidth("100/100") + 40), metricas.getHeight() + 12);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(EstiloMadera.CONTORNO);
        g2.fillRoundRect(0, 0, w, h, 8, 8);
        g2.setColor(EstiloMadera.MADERA);
        g2.fillRoundRect(1, 1, w - 2, h - 2, 7, 7);
        g2.setColor(PISTA);
        g2.fillRect(4, 4, w - 8, h - 8);
        double fraccion = getMaximum() == getMinimum() ? 0
                : (getValue() - getMinimum()) / (double) (getMaximum() - getMinimum());
        Color fill = fraccion > MITAD ? MUCHO : (fraccion > CUARTO ? BAJO : POCO);
        int colmado = (int) Math.round((w - 8) * fraccion);
        if (colmado > 0) {
            g2.setPaint(new GradientPaint(0, 4, fill.brighter(), 0, h - 4, fill.darker()));
            g2.fillRect(4, 4, colmado, h - 8);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillRect(4, 5, colmado, 2);
        }
        String texto = getString();
        if (texto != null) {
            FontMetrics metricas = g2.getFontMetrics(getFont());
            g2.setFont(getFont());
            int x = (w - metricas.stringWidth(texto)) / 2;
            int y = (h - metricas.getHeight()) / 2 + metricas.getAscent();
            g2.setColor(SOMBRA);
            g2.drawString(texto, x + 1, y + 1);
            g2.setColor(EstiloMadera.CREMA);
            g2.drawString(texto, x, y);
        }
        g2.dispose();
    }
}
