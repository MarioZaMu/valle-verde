package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.HoverAnimator;
import edu.ucc.farmsim.ui.theme.WoodStyle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;

/**
 * Un botón de madera: se ilumina y sube un poco cuando el cursor está encima, y se hunde al pulsarlo.
 *
 * <p>Stories: HU-33, HU-48.
 */
public class WoodButton extends JButton {

    private static final long serialVersionUID = 1L;
    private static final int LIFT = 2;

    private final transient HoverAnimator hover = new HoverAnimator(this);

    /**
     * Crea el botón.
     *
     * @param text lo que dice
     */
    public WoodButton(String text) {
        super(text);
        WoodStyle.prepare(this, false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int lift = getModel().isPressed() ? -1 : hover.lift(LIFT);
        g2.translate(0, -lift);
        WoodStyle.paintButton(g2, this, hover.value(), false);
        super.paintComponent(g2);
        g2.dispose();
    }
}
