package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.BorderLayout;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

/**
 * El diálogo de logros que se abre con {@code L} (HU-26): lo que pide cada uno, su recompensa y el progreso. Es modal;
 * {@code Esc} o {@code L} lo cierran. Toda la lógica está en {@link PresentadorLogros}.
 *
 * <p>Stories: HU-26.
 */
public final class DialogoLogros extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Construye el diálogo.
     *
     * @param dueno     la ventana a la que pertenece
     * @param presentador la lógica del diálogo
     */
    public DialogoLogros(Window dueno, PresentadorLogros presentador) {
        super(dueno, Textos.TITULO_LOGROS, ModalityType.APPLICATION_MODAL);
        DefaultTableModel filas = new DefaultTableModel(
                new String[] {Textos.LOGRO_COL, Textos.CONDICION_COL, Textos.RECOMPENSA_COL, Textos.ESTADO_COL}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        presentador.logros().forEach(a -> filas.addRow(presentador.fila(a)));
        JTable tabla = new JTable(filas);
        tabla.setName("achievement-table");
        setLayout(new BorderLayout());
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        for (String clave : new String[] {"ESCAPE", "L"}) {
            getRootPane().registerKeyboardAction(evento -> dispose(), KeyStroke.getKeyStroke(clave),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        setSize(640, 330);
        setLocationRelativeTo(dueno);
    }
}
