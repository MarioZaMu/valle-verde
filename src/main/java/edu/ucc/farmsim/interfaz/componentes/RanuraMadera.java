package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.tema.AnimadorHover;
import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
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
public class RanuraMadera extends JToggleButton {

    private static final long serialVersionUID = 1L;
    private static final int LEVANTAR = 3;

    private final transient AnimadorHover sobrevolar = new AnimadorHover(this);

    /**
     * Crea la casilla.
     *
     * @param texto lo que dice bajo su ícono
     */
    public RanuraMadera(String texto) {
        super(texto);
        EstiloMadera.preparar(this, true);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setIconTextGap(2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(0, -(isSelected() ? LEVANTAR : sobrevolar.levantar(LEVANTAR)));
        EstiloMadera.botonPintar(g2, this, sobrevolar.value(), true);
        super.paintComponent(g2);
        g2.dispose();
    }
}
