package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
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
public class CajaMadera extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int RELLENO = 6;

    /**
     * Crea la caja.
     *
     * @param disposicion cómo se distribuye su contenido
     */
    public CajaMadera(LayoutManager disposicion) {
        super(disposicion);
        setOpaque(false);
        int borde = EstiloMadera.MARCO + RELLENO;
        setBorder(BorderFactory.createEmptyBorder(borde, borde + 2, borde + 3, borde + 4));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        EstiloMadera.marcoPintar(g2, 0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
