package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.HoverAnimator;
import edu.ucc.farmsim.ui.theme.WoodStyle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * Una casilla de la barra de herramientas: un cuadrado de pergamino en un marco de madera con el ícono sobre el texto.
 * La casilla elegida recibe un marco brillante, y una casilla sube un poco bajo el cursor.
 *
 * <p>Stories: HU-36, HU-48.
 */
public class WoodSlot extends JToggleButton {

    private static final long serialVersionUID = 1L;
    private static final int LIFT = 3;

    private final transient HoverAnimator hover = new HoverAnimator(this);

    /**
     * Crea la casilla.
     *
     * @param text lo que dice bajo su ícono
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
