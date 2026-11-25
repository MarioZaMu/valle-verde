package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * El inventario como una grilla de casillas (HU-37): el nombre y la cantidad de cada casilla ocupada (la cantidad de
 * una pila llena es 99) y la semilla elegida marcada. Un clic en una semilla la elige para la herramienta Sembrar. Cada
 * casilla ocupada muestra el sprite de su objeto sobre el texto; sin sprites muestra solo texto.
 *
 * <p>Stories: HU-37, HU-48.
 */
public final class GrillaInventario extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int COLUMNAS = 6;

    private final transient ModeloInventario modelo;
    private final transient GestorRecurso recursos;

    /**
     * Construye la grilla y la mantiene al día con el modelo.
     *
     * @param modelo lo que contienen las casillas
     */
    public GrillaInventario(ModeloInventario modelo) {
        this(modelo, GestorRecurso.compartido());
    }

    /**
     * Construye la grilla con íconos de objetos de los recursos dados (solo texto si faltan).
     *
     * @param modelo  lo que contienen las casillas
     * @param recursos de donde vienen los íconos de objetos
     */
    public GrillaInventario(ModeloInventario modelo, GestorRecurso recursos) {
        super(new GridLayout(0, COLUMNAS, 4, 4));
        this.modelo = modelo;
        this.recursos = recursos;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        modelo.addChangeListener(this::reconstruir);
        reconstruir();
    }

    private void reconstruir() {
        removeAll();
        for (int i = 0; i < modelo.conteoRanura(); i++) {
            ModeloInventario.Celda celda = modelo.celda(i);
            JToggleButton ranura = new JToggleButton(celda.isEmpty() ? " "
                    : "<html><center>" + celda.name() + "<br><b>" + celda.monto() + "</b></center></html>");
            ranura.setName("slot-" + i);
            if (!celda.isEmpty()) {
                recursos.icono("items/" + celda.idArticulo() + ".png").ifPresent(ranura::setIcon);
                ranura.setVerticalTextPosition(SwingConstants.BOTTOM);
                ranura.setHorizontalTextPosition(SwingConstants.CENTER);
            }
            ranura.setFocusable(false);
            ranura.setSelected(modelo.isSelected(i));
            ranura.setEnabled(!celda.isEmpty());
            int indice = i;
            ranura.addActionListener(evento -> modelo.seleccionar(indice));
            add(ranura);
        }
        revalidate();
        repaint();
    }
}
