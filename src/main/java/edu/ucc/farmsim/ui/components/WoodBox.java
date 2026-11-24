package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.WoodStyle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Un panel dibujado como un marco de madera alrededor de pergamino; lo que contiene queda sobre el pergamino.
 *
 * <p>Stories: HU-48.
 */
public class WoodBox extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int PAD = 6;

    /**
     * Crea la caja.
     *
     * @param layout cómo se distribuye su contenido
     */
    public WoodBox(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        int edge = WoodStyle.FRAME + PAD;
        setBorder(BorderFactory.createEmptyBorder(edge, edge + 2, edge + 3, edge + 4));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        WoodStyle.paintFrame(g2, 0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
