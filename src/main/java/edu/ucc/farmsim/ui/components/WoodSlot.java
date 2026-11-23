package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.HoverAnimator;
import edu.ucc.farmsim.ui.theme.WoodStyle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * A slot of the tool bar: a parchment square in a wooden frame with the icon above the text. The chosen slot
 * gets a bright frame, and a slot rises a little under the cursor.
 *
 * <p>Stories: HU-36, HU-48.
 */
public class WoodSlot extends JToggleButton {

    private static final long serialVersionUID = 1L;
    private static final int LIFT = 3;

    private final transient HoverAnimator hover = new HoverAnimator(this);

    /**
     * Creates the slot.
     *
     * @param text what it says under its icon
     */
    public WoodSlot(String text) {
        super(text);
        WoodStyle.prepare(this, true);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setIconTextGap(2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(0, -(isSelected() ? LIFT : hover.lift(LIFT)));
        WoodStyle.paintButton(g2, this, hover.value(), true);
        super.paintComponent(g2);
        g2.dispose();
    }
}
