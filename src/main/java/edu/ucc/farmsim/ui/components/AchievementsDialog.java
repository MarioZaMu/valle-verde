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
 * The achievements dialog opened with {@code L} (HU-26): what each one asks for, its reward and the progress.
 * It is modal; {@code Esc} or {@code L} close it. All the logic is in {@link AchievementsPresenter}.
 *
 * <p>Stories: HU-26.
 */
public final class AchievementsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Builds the dialog.
     *
     * @param owner     the window it belongs to
     * @param presenter the logic of the dialog
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
