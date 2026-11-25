package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.tema.Textos;
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
public final class DialogoInventario extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Construye el diálogo.
     *
     * @param dueno la ventana a la que pertenece
     * @param modelo lo que muestra la grilla
     */
    public DialogoInventario(Window dueno, ModeloInventario modelo) {
        super(dueno, Textos.TITULO_INVENTARIO, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        add(new GrillaInventario(modelo), BorderLayout.CENTER);
        for (String clave : new String[] {"ESCAPE", "I"}) {
            getRootPane().registerKeyboardAction(evento -> dispose(), KeyStroke.getKeyStroke(clave),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        pack();
        setLocationRelativeTo(dueno);
    }
}
