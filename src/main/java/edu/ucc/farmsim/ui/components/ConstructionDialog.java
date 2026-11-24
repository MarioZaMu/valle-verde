package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.dto.BuildingOfferView;
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
 * El diálogo de construcción que se abre al hacer clic en una parcela con la herramienta Construir (HU-18): el catálogo
 * con tamaños, costos y lo bloqueado, y un botón que construye el edificio seleccionado con su esquina en esa parcela.
 * Es modal; {@code Esc} lo cierra. Toda la lógica está en {@link ConstructionPresenter}.
 *
 * <p>Stories: HU-18.
 */
public final class ConstructionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient ConstructionPresenter presenter;
    private final JLabel money = new JLabel();
    private final JLabel message = new JLabel(" ");
    private final JButton build = new JButton(Texts.BUILD);
    private final DefaultTableModel rows = new DefaultTableModel(
            new String[] {Texts.COL_BUILDING, Texts.COL_SIZE, Texts.COL_COST, Texts.COL_STATE}, 0) {
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
     * @param presenter la lógica de la construcción
     */
    public ConstructionDialog(Window owner, ConstructionPresenter presenter) {
        super(owner, Texts.CONSTRUCTION_TITLE, ModalityType.APPLICATION_MODAL);
        this.presenter = presenter;
        table.setName("building-table");
        build.setName("build-button");
        build.addActionListener(event -> onBuild());
        table.getSelectionModel().addListSelectionListener(event -> refreshButton());
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel(" " + presenter.placeText()), BorderLayout.NORTH);
        header.add(money, BorderLayout.SOUTH);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(build);
        south.add(message);
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        reload();
        setSize(620, 300);
        setLocationRelativeTo(owner);
    }

    private BuildingOfferView selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : presenter.catalog().get(row);
    }

    private void onBuild() {
        BuildingOfferView offer = selected();
        if (offer == null) {
            return;
        }
        message.setText(presenter.build(offer));
        if (presenter.lastBuilt().isPresent()) {
            dispose();
            return;
        }
        reload();
    }

    private void reload() {
        int selected = table.getSelectedRow();
        money.setText(" " + presenter.moneyText());
        rows.setRowCount(0);
        presenter.catalog().forEach(offer -> rows.addRow(presenter.row(offer)));
        if (selected >= 0 && selected < rows.getRowCount()) {
            table.setRowSelectionInterval(selected, selected);
        }
        refreshButton();
    }

    private void refreshButton() {
        BuildingOfferView offer = selected();
        build.setEnabled(offer != null && presenter.canBuild(offer));
        if (offer != null && !presenter.canBuild(offer)) {
            message.setText(presenter.block(offer));
        }
    }
}
