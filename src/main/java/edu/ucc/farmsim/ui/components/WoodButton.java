package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.HoverAnimator;
import edu.ucc.farmsim.ui.theme.WoodStyle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;

/**
 * A wooden button: it lights up and rises a little when the cursor is on it, and sinks when pressed.
 *
 * <p>Stories: HU-33, HU-48.
 */
public class WoodButton extends JButton {

    private static final long serialVersionUID = 1L;
    private static final int LIFT = 2;

    private final transient HoverAnimator hover = new HoverAnimator(this);

    /**
     * Creates the button.
     *
     * @param text what it says
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
