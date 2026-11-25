package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.dto.VistaOfertaEdificio;
import edu.ucc.farmsim.interfaz.tema.Textos;
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
 * Es modal; {@code Esc} lo cierra. Toda la lógica está en {@link PresentadorConstruccion}.
 *
 * <p>Stories: HU-18.
 */
public final class DialogoConstruccion extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient PresentadorConstruccion presentador;
    private final JLabel dinero = new JLabel();
    private final JLabel mensaje = new JLabel(" ");
    private final JButton build = new JButton(Textos.BUILD);
    private final DefaultTableModel filas = new DefaultTableModel(
            new String[] {Textos.EDIFICIO_COL, Textos.TAMANIO_COL, Textos.COSTO_COL, Textos.ESTADO_COL}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(filas);

    /**
     * Construye el diálogo.
     *
     * @param dueno     la ventana a la que pertenece
     * @param presentador la lógica de la construcción
     */
    public DialogoConstruccion(Window dueno, PresentadorConstruccion presentador) {
        super(dueno, Textos.TITULO_CONSTRUCCION, ModalityType.APPLICATION_MODAL);
        this.presentador = presentador;
        tabla.setName("building-table");
        build.setName("build-button");
        build.addActionListener(evento -> onConstruir());
        tabla.getSelectionModel().addListSelectionListener(evento -> botonRefrescar());
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel(" " + presentador.textoColocar()), BorderLayout.NORTH);
        header.add(dinero, BorderLayout.SOUTH);
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sur.add(build);
        sur.add(mensaje);
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(evento -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        recargar();
        setSize(620, 300);
        setLocationRelativeTo(dueno);
    }

    private VistaOfertaEdificio seleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : presentador.catalogo().get(fila);
    }

    private void onConstruir() {
        VistaOfertaEdificio oferta = seleccionado();
        if (oferta == null) {
            return;
        }
        mensaje.setText(presentador.construir(oferta));
        if (presentador.ultimoConstruido().isPresent()) {
            dispose();
            return;
        }
        recargar();
    }

    private void recargar() {
        int seleccionado = tabla.getSelectedRow();
        dinero.setText(" " + presentador.textoDinero());
        filas.setRowCount(0);
        presentador.catalogo().forEach(oferta -> filas.addRow(presentador.fila(oferta)));
        if (seleccionado >= 0 && seleccionado < filas.getRowCount()) {
            tabla.setRowSelectionInterval(seleccionado, seleccionado);
        }
        botonRefrescar();
    }

    private void botonRefrescar() {
        VistaOfertaEdificio oferta = seleccionado();
        build.setEnabled(oferta != null && presentador.puedeConstruir(oferta));
        if (oferta != null && !presentador.puedeConstruir(oferta)) {
            mensaje.setText(presentador.bloquear(oferta));
        }
    }
}
