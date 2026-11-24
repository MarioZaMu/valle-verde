package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

/**
 * El panel de un edificio: sus animales con su estado y los botones Alimentar, Recolectar y Curar (HU-20, HU-22,
 * HU-23). Es modal; {@code Esc} lo cierra. Toda la lógica está en {@link BuildingPresenter}.
 *
 * <p>Stories: HU-20, HU-22, HU-23.
 */
public final class BuildingDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient BuildingPresenter presenter;
    private final JLabel message = new JLabel(" ");
    private final JButton feed = new JButton(Texts.FEED_BUILDING);
    private final JButton collect = new JButton(Texts.COLLECT);
    private final JButton cure = new JButton(Texts.CURE);
    private final DefaultTableModel rows = new DefaultTableModel(
            new String[] {Texts.COL_ANIMAL, Texts.COL_STATE, Texts.COL_DAYS_WITHOUT_FEED, Texts.COL_FED_TODAY,
                Texts.COL_PENDING}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(rows);

    /**
     * Construye el diálogo.
     *
     * @param owner     la ventana a la que pertenece
     * @param presenter la lógica del panel del edificio
     */
    public BuildingDialog(Window owner, BuildingPresenter presenter) {
        super(owner, Texts.BUILDING_TITLE, ModalityType.APPLICATION_MODAL);
        this.presenter = presenter;
        table.setName("animal-table");
        feed.setName("feed-button");
        collect.setName("collect-button");
        cure.setName("cure-button");
        feed.addActionListener(event -> act(presenter.feed()));
        collect.addActionListener(event -> act(presenter.collect()));
        cure.addActionListener(event -> act(presenter.cure(table.getSelectedRow())));
        table.getSelectionModel().addListSelectionListener(event -> refreshButtons());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(feed);
        south.add(collect);
        south.add(cure);
        south.add(message);
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        reload();
        setSize(620, 280);
        setLocationRelativeTo(owner);
    }

    private void act(String text) {
        message.setText(text);
        reload();
    }

    private void reload() {
        int selected = table.getSelectedRow();
        setTitle(presenter.title());
        rows.setRowCount(0);
        presenter.animals().forEach(animal -> rows.addRow(presenter.row(animal)));
        if (selected >= 0 && selected < rows.getRowCount()) {
            table.setRowSelectionInterval(selected, selected);
        }
        refreshButtons();
    }

    private void refreshButtons() {
        feed.setEnabled(presenter.canFeed());
        collect.setEnabled(presenter.canCollect());
        cure.setEnabled(presenter.canCure(table.getSelectedRow()));
    }
}
