package edu.ucc.farmsim.interfaz.componentes;

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
 * El panel de un edificio: sus animales con su estado y los botones Alimentar, Recolectar y Curar (HU-20, HU-22,
 * HU-23). Es modal; {@code Esc} lo cierra. Toda la lógica está en {@link PresentadorEdificio}.
 *
 * <p>Stories: HU-20, HU-22, HU-23.
 */
public final class DialogoEdificio extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient PresentadorEdificio presentador;
    private final JLabel mensaje = new JLabel(" ");
    private final JButton alimentar = new JButton(Textos.EDIFICIO_ALIMENTAR);
    private final JButton collect = new JButton(Textos.RECOLECTAR);
    private final JButton curar = new JButton(Textos.CURAR);
    private final DefaultTableModel filas = new DefaultTableModel(
            new String[] {Textos.COL_ANIMAL, Textos.ESTADO_COL, Textos.DIAS_COL_SIN_ALIMENTAR, Textos.HOY_COL_ALIMENTADO,
                Textos.COL_PENDIENTE}, 0) {
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
     * @param presentador la lógica del panel del edificio
     */
    public DialogoEdificio(Window dueno, PresentadorEdificio presentador) {
        super(dueno, Textos.TITULO_EDIFICIO, ModalityType.APPLICATION_MODAL);
        this.presentador = presentador;
        tabla.setName("animal-table");
        alimentar.setName("feed-button");
        collect.setName("collect-button");
        curar.setName("cure-button");
        alimentar.addActionListener(evento -> actuar(presentador.alimentar()));
        collect.addActionListener(evento -> actuar(presentador.collect()));
        curar.addActionListener(evento -> actuar(presentador.curar(tabla.getSelectedRow())));
        tabla.getSelectionModel().addListSelectionListener(evento -> botonesRefrescar());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sur.add(alimentar);
        sur.add(collect);
        sur.add(curar);
        sur.add(mensaje);
        setLayout(new BorderLayout());
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(evento -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        recargar();
        setSize(620, 280);
        setLocationRelativeTo(dueno);
    }

    private void actuar(String texto) {
        mensaje.setText(texto);
        recargar();
    }

    private void recargar() {
        int seleccionado = tabla.getSelectedRow();
        setTitle(presentador.titulo());
        filas.setRowCount(0);
        presentador.animales().forEach(animal -> filas.addRow(presentador.fila(animal)));
        if (seleccionado >= 0 && seleccionado < filas.getRowCount()) {
            tabla.setRowSelectionInterval(seleccionado, seleccionado);
        }
        botonesRefrescar();
    }

    private void botonesRefrescar() {
        alimentar.setEnabled(presentador.puedeAlimentar());
        collect.setEnabled(presentador.puedeRecolectar());
        curar.setEnabled(presentador.puedeCurar(tabla.getSelectedRow()));
    }
}
