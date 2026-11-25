package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.tema.Textos;
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
 * mientras el jugador no pueda pagar. Toda la lógica está en {@link PresentadorMercado}.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-38.
 */
public final class DialogoMercado extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient PresentadorMercado presentador;
    private final JLabel dinero = new JLabel();
    private final JLabel mensaje = new JLabel(" ");
    private final Pestania pestaniaComprar;
    private final Pestania pestaniaVender;

    /**
     * Construye el diálogo y carga el mercado.
     *
     * @param dueno     la ventana a la que pertenece
     * @param presentador la lógica del mercado
     */
    public DialogoMercado(Window dueno, PresentadorMercado presentador) {
        super(dueno, Textos.TITULO_MERCADO, ModalityType.APPLICATION_MODAL);
        this.presentador = presentador;
        presentador.refrescar();
        pestaniaComprar = new Pestania(true);
        pestaniaVender = new Pestania(false);
        JTabbedPane pestanias = new JTabbedPane();
        pestanias.addTab(Textos.COMPRAR_PESTANIA, pestaniaComprar.panel);
        pestanias.addTab(Textos.VENDER_PESTANIA, pestaniaVender.panel);
        setLayout(new BorderLayout());
        add(dinero, BorderLayout.NORTH);
        add(pestanias, BorderLayout.CENTER);
        add(mensaje, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(evento -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        recargar();
        setSize(560, 380);
        setLocationRelativeTo(dueno);
    }

    /** La pestaña con el título dado ({@code Comprar} o {@code Vender}) como componente, para las pruebas. */
    JTabbedPane pestanias() {
        return (JTabbedPane) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
    }

    private void recargar() {
        dinero.setText(" " + presentador.textoDinero());
        pestaniaComprar.recargar();
        pestaniaVender.recargar();
    }

    /** Una pestaña: la tabla, el selector de cantidad, la previsión y el botón de acción. */
    private final class Pestania {
        private final boolean comprando;
        private final DefaultTableModel filas;
        private final JTable tabla;
        private final JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
        private final JLabel total = new JLabel(" ");
        private final JButton accion = new JButton(); // lo habilita refreshTotal
        private final JPanel panel = new JPanel(new BorderLayout());

        Pestania(boolean comprando) {
            this.comprando = comprando;
            String[] columnas = comprando
                    ? new String[] {Textos.PRODUCTO_COL, Textos.PRECIO_COL}
                    : new String[] {Textos.PRODUCTO_COL, Textos.COL_PROPIO, Textos.PRECIO_COL + " / " + Textos.TENDENCIA_COL};
            filas = new DefaultTableModel(columnas, 0) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };
            tabla = new JTable(filas);
            tabla.setName(comprando ? "buy-table" : "sell-table");
            tabla.getSelectionModel().addListSelectionListener(evento -> {
                if (!evento.getValueIsAdjusting()) {
                    cantidadSincronizar();
                }
            });
            cantidad.addChangeListener(evento -> totalRefrescar());
            accion.setText(comprando ? Textos.COMPRAR : Textos.VENDER);
            accion.setName(comprando ? "buy-button" : "sell-button");
            accion.addActionListener(evento -> {
                String id = idSeleccionado();
                if (id != null) {
                    mensaje.setText(" " + (comprando
                            ? presentador.unidadesComprar(id, cantidad())
                            : presentador.unidadesVender(id, cantidad())));
                    recargar();
                }
            });
            JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controles.add(new JLabel(Textos.CANTIDAD));
            controles.add(cantidad);
            controles.add(total);
            controles.add(accion);
            panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
            panel.add(controles, BorderLayout.SOUTH);
        }

        private int cantidad() {
            return (Integer) cantidad.getValue();
        }

        private String idSeleccionado() {
            int fila = tabla.getSelectedRow();
            return fila < 0 ? null : ids().get(fila);
        }

        private List<String> ids() {
            return comprando
                    ? presentador.compra().stream().map(o -> o.idArticulo()).toList()
                    : presentador.vende().stream().map(o -> o.idArticulo()).toList();
        }

        void recargar() {
            int seleccionado = tabla.getSelectedRow();
            filas.setRowCount(0);
            if (comprando) {
                presentador.compra().forEach(o -> filas.addRow(new Object[] {o.name(), Textos.dinero(o.precio())}));
            } else {
                presentador.vende().forEach(o -> filas.addRow(
                        new Object[] {o.name(), o.propio(), presentador.textoPrecioVender(o)}));
            }
            if (seleccionado >= 0 && seleccionado < filas.getRowCount()) {
                tabla.setRowSelectionInterval(seleccionado, seleccionado);
            }
            cantidadSincronizar();
        }

        private void cantidadSincronizar() {
            String id = idSeleccionado();
            int max = id == null ? 1 : Math.max(1, presentador.cantidadMaxima(comprando, id));
            SpinnerNumberModel modelo = (SpinnerNumberModel) cantidad.getModel();
            modelo.setMaximum(max);
            if (cantidad() > max) {
                cantidad.setValue(max);
            }
            totalRefrescar();
        }

        private void totalRefrescar() {
            String id = idSeleccionado();
            if (id == null) {
                total.setText(" ");
                accion.setEnabled(false);
                return;
            }
            if (comprando) {
                String bloquear = presentador.bloquearCompra(id, cantidad());
                total.setText(bloquear.isEmpty() ? presentador.compraVistaPrevia(id, cantidad()) : bloquear);
                accion.setEnabled(presentador.puedeComprar(id, cantidad()));
            } else {
                boolean any = presentador.cantidadMaxima(false, id) >= 1;
                total.setText(any ? presentador.ventaVistaPrevia(id, cantidad()) : " ");
                accion.setEnabled(any);
            }
        }
    }
}
