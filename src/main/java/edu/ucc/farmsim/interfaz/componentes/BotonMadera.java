package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.tema.AnimadorHover;
import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;

/**
 * Un botón de madera: se ilumina y sube un poco cuando el cursor está encima, y se hunde al pulsarlo.
 *
 * <p>Stories: HU-33, HU-48.
 */
public class BotonMadera extends JButton {

    private static final long serialVersionUID = 1L;
    private static final int LEVANTAR = 2;

    private final transient AnimadorHover sobrevolar = new AnimadorHover(this);

    /**
     * Crea el botón.
     *
     * @param texto lo que dice
     */
    public BotonMadera(String texto) {
        super(texto);
        EstiloMadera.preparar(this, false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int levantar = getModel().isPressed() ? -1 : sobrevolar.levantar(LEVANTAR);
        g2.translate(0, -levantar);
        EstiloMadera.botonPintar(g2, this, sobrevolar.value(), false);
        super.paintComponent(g2);
        g2.dispose();
    }
}
