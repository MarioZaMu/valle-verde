package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.Texts;
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
 * {@code Esc} o {@code L} lo cierran. Toda la lógica está en {@link AchievementsPresenter}.
 *
 * <p>Stories: HU-26.
 */
public final class AchievementsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Construye el diálogo.
     *
     * @param owner     la ventana a la que pertenece
     * @param presenter la lógica del diálogo
     */
    public AchievementsDialog(Window owner, AchievementsPresenter presenter) {
        super(owner, Texts.ACHIEVEMENTS_TITLE, ModalityType.APPLICATION_MODAL);
        DefaultTableModel rows = new DefaultTableModel(
                new String[] {Texts.COL_ACHIEVEMENT, Texts.COL_CONDITION, Texts.COL_REWARD, Texts.COL_STATE}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        presenter.achievements().forEach(a -> rows.addRow(presenter.row(a)));
        JTable table = new JTable(rows);
        table.setName("achievement-table");
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        for (String key : new String[] {"ESCAPE", "L"}) {
            getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke(key),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        setSize(640, 330);
        setLocationRelativeTo(owner);
    }
}
