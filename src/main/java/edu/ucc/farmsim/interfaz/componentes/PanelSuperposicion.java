package edu.ucc.farmsim.interfaz.componentes;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Un panel que apila componentes uno encima de otro, cada uno cubriendo toda el área. El primero que se agrega es el de
 * arriba; su tamaño es el del componente base (el último agregado).
 *
 * <p>Stories: HU-39.
 */
public final class PanelSuperposicion extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient JComponent base;

    /**
     * Crea el panel alrededor de su componente base.
     *
     * @param base el componente que fija el tamaño y queda al fondo
     */
    public PanelSuperposicion(JComponent base) {
        super(null);
        this.base = base;
        add(base);
    }

    /** Pone un componente encima de todo lo agregado antes. */
    public void agregarOnArriba(JComponent superposicion) {
        add(superposicion, 0);
    }

    /** Las capas se superponen, así que Swing debe pintar todas, no solo la que lo pidió. */
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    @Override
    public Dimension getPreferredSize() {
        return base.getPreferredSize();
    }

    @Override
    public void doLayout() {
        for (Component hijo : getComponents()) {
            hijo.setBounds(0, 0, getWidth(), getHeight());
        }
    }
}
