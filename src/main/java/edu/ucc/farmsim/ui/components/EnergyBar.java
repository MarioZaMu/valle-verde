package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.WoodStyle;
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
public class EnergyBar extends JProgressBar {

    private static final long serialVersionUID = 1L;
    private static final Color TRACK = new Color(0x3B2412);
    private static final Color PLENTY = new Color(0x5FB83A);
    private static final Color LOW = new Color(0xF2C14E);
    private static final Color LITTLE = new Color(0xE0502A);
    private static final Color SHADOW = new Color(0, 0, 0, 150);
    private static final double HALF = 0.5;
    private static final double QUARTER = 0.25;

    /**
     * Crea la barra.
     *
     * @param max el valor más grande
     */
    public EnergyBar(int max) {
        super(0, max);
        setStringPainted(true);
        setOpaque(false);
        setBorderPainted(false);
        setFont(getFont().deriveFont(Font.BOLD));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        return new Dimension(Math.max(170, metrics.stringWidth("100/100") + 40), metrics.getHeight() + 12);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(WoodStyle.OUTLINE);
        g2.fillRoundRect(0, 0, w, h, 8, 8);
        g2.setColor(WoodStyle.WOOD);
        g2.fillRoundRect(1, 1, w - 2, h - 2, 7, 7);
        g2.setColor(TRACK);
        g2.fillRect(4, 4, w - 8, h - 8);
        double fraction = getMaximum() == getMinimum() ? 0
                : (getValue() - getMinimum()) / (double) (getMaximum() - getMinimum());
        Color fill = fraction > HALF ? PLENTY : (fraction > QUARTER ? LOW : LITTLE);
        int filled = (int) Math.round((w - 8) * fraction);
        if (filled > 0) {
            g2.setPaint(new GradientPaint(0, 4, fill.brighter(), 0, h - 4, fill.darker()));
            g2.fillRect(4, 4, filled, h - 8);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillRect(4, 5, filled, 2);
        }
        String text = getString();
        if (text != null) {
            FontMetrics metrics = g2.getFontMetrics(getFont());
            g2.setFont(getFont());
            int x = (w - metrics.stringWidth(text)) / 2;
            int y = (h - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.setColor(SHADOW);
            g2.drawString(text, x + 1, y + 1);
            g2.setColor(WoodStyle.CREAM);
            g2.drawString(text, x, y);
        }
        g2.dispose();
    }
}
