package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/**
 * El mercado: un diálogo modal con las pestañas «Comprar» y «Vender» (HU-38). Al ser modal, el mapa no responde a los
 * clics mientras está abierto; {@code Esc} lo cierra. El selector de cantidad permite de 1 a 99 al comprar y de 1 hasta
 * las unidades que se tienen al vender, la venta muestra su total previsto y el botón de comprar se deshabilita
 * mientras el jugador no pueda pagar. Toda la lógica está en {@link MarketPresenter}.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-38.
 */
public final class MarketDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient MarketPresenter presenter;
    private final JLabel money = new JLabel();
    private final JLabel message = new JLabel(" ");
    private final Tab buyTab;
    private final Tab sellTab;

    /**
     * Construye el diálogo y carga el mercado.
     *
     * @param owner     la ventana a la que pertenece
     * @param presenter la lógica del mercado
     */
    public MarketDialog(Window owner, MarketPresenter presenter) {
        super(owner, Texts.MARKET_TITLE, ModalityType.APPLICATION_MODAL);
        this.presenter = presenter;
        presenter.refresh();
        buyTab = new Tab(true);
        sellTab = new Tab(false);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(Texts.TAB_BUY, buyTab.panel);
        tabs.addTab(Texts.TAB_SELL, sellTab.panel);
        setLayout(new BorderLayout());
        add(money, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        reload();
        setSize(560, 380);
        setLocationRelativeTo(owner);
    }

    /** La pestaña con el título dado ({@code Comprar} o {@code Vender}) como componente, para las pruebas. */
    JTabbedPane tabs() {
        return (JTabbedPane) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
    }

    private void reload() {
        money.setText(" " + presenter.moneyText());
        buyTab.reload();
        sellTab.reload();
    }

    /** Una pestaña: la tabla, el selector de cantidad, la previsión y el botón de acción. */
    private final class Tab {
        private final boolean buying;
        private final DefaultTableModel rows;
        private final JTable table;
        private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
        private final JLabel total = new JLabel(" ");
        private final JButton action = new JButton(); // lo habilita refreshTotal
        private final JPanel panel = new JPanel(new BorderLayout());

        Tab(boolean buying) {
            this.buying = buying;
            String[] columns = buying
                    ? new String[] {Texts.COL_PRODUCT, Texts.COL_PRICE}
                    : new String[] {Texts.COL_PRODUCT, Texts.COL_OWNED, Texts.COL_PRICE + " / " + Texts.COL_TREND};
            rows = new DefaultTableModel(columns, 0) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(rows);
            table.setName(buying ? "buy-table" : "sell-table");
            table.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    syncQuantity();
                }
            });
            quantity.addChangeListener(event -> refreshTotal());
            action.setText(buying ? Texts.BUY : Texts.SELL);
            action.setName(buying ? "buy-button" : "sell-button");
            action.addActionListener(event -> {
                String id = selectedId();
                if (id != null) {
                    message.setText(" " + (buying
                            ? presenter.buyUnits(id, quantity())
                            : presenter.sellUnits(id, quantity())));
                    reload();
                }
            });
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controls.add(new JLabel(Texts.QUANTITY));
            controls.add(quantity);
            controls.add(total);
            controls.add(action);
            panel.add(new JScrollPane(table), BorderLayout.CENTER);
            panel.add(controls, BorderLayout.SOUTH);
        }

        private int quantity() {
            return (Integer) quantity.getValue();
        }

        private String selectedId() {
            int row = table.getSelectedRow();
            return row < 0 ? null : ids().get(row);
        }

        private List<String> ids() {
            return buying
                    ? presenter.buys().stream().map(o -> o.itemId()).toList()
                    : presenter.sells().stream().map(o -> o.itemId()).toList();
        }

        void reload() {
            int selected = table.getSelectedRow();
            rows.setRowCount(0);
            if (buying) {
                presenter.buys().forEach(o -> rows.addRow(new Object[] {o.name(), Texts.money(o.price())}));
            } else {
                presenter.sells().forEach(o -> rows.addRow(
                        new Object[] {o.name(), o.owned(), presenter.sellPriceText(o)}));
            }
            if (selected >= 0 && selected < rows.getRowCount()) {
                table.setRowSelectionInterval(selected, selected);
            }
            syncQuantity();
        }

        private void syncQuantity() {
            String id = selectedId();
            int max = id == null ? 1 : Math.max(1, presenter.maxQuantity(buying, id));
            SpinnerNumberModel model = (SpinnerNumberModel) quantity.getModel();
            model.setMaximum(max);
            if (quantity() > max) {
                quantity.setValue(max);
            }
            refreshTotal();
        }

        private void refreshTotal() {
            String id = selectedId();
            if (id == null) {
                total.setText(" ");
                action.setEnabled(false);
                return;
            }
            if (buying) {
                String block = presenter.purchaseBlock(id, quantity());
                total.setText(block.isEmpty() ? presenter.previewPurchase(id, quantity()) : block);
                action.setEnabled(presenter.canBuy(id, quantity()));
            } else {
                boolean any = presenter.maxQuantity(false, id) >= 1;
                total.setText(any ? presenter.previewSale(id, quantity()) : " ");
                action.setEnabled(any);
            }
        }
    }
}
