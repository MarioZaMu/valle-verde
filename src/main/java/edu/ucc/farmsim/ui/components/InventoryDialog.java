package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.BorderLayout;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

/**
 * The inventory dialog opened with the {@code I} key (HU-37). {@code Esc} or {@code I} closes it.
 *
 * <p>Stories: HU-37.
 */
public final class InventoryDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Builds the dialog.
     *
     * @param owner the window it belongs to
     * @param model what the grid shows
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
