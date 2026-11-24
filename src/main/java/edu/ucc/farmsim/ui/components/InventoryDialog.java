package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.BorderLayout;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

/**
 * El diálogo del inventario que se abre con la tecla {@code I} (HU-37). {@code Esc} o {@code I} lo cierran.
 *
 * <p>Stories: HU-37.
 */
public final class InventoryDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Construye el diálogo.
     *
     * @param owner la ventana a la que pertenece
     * @param model lo que muestra la grilla
     */
    public InventoryDialog(Window owner, InventoryModel model) {
        super(owner, Texts.INVENTORY_TITLE, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        add(new InventoryGrid(model), BorderLayout.CENTER);
        for (String key : new String[] {"ESCAPE", "I"}) {
            getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke(key),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        pack();
        setLocationRelativeTo(owner);
    }
}
