package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.function.IntConsumer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

/**
 * Las ranuras de guardado: las tres con lo que contienen, y los botones Guardar y Cargar (HU-29, HU-30, HU-32). Guardar
 * en una ranura ocupada pregunta primero. Es modal; {@code Esc} lo cierra. Toda la lógica está en {@link
 * PresentadorRanuras}.
 *
 * <p>Stories: HU-29, HU-30, HU-32.
 */
public final class DialogoRanuras extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient PresentadorRanuras presentador;
    private final transient IntConsumer onUsado;
    private final transient Runnable onCargado;
    private final DefaultListModel<String> articulos = new DefaultListModel<>();
    private final JList<String> lista = new JList<>(articulos);
    private final JButton save = new JButton(Textos.GUARDADO);
    private final JButton cargar = new JButton(Textos.CARGAR);
    private final JLabel mensaje = new JLabel(" ");

    /**
     * Construye el diálogo.
     *
     * @param dueno     la ventana a la que pertenece
     * @param presentador la lógica de las ranuras
     * @param onUsado    se le avisa qué ranura se guardó o se cargó, para que las teclas rápidas lo sepan
     * @param onCargado  se le avisa cuando se cargó una partida (el diálogo se cierra solo después)
     */
    public DialogoRanuras(Window dueno, PresentadorRanuras presentador, IntConsumer onUsado, Runnable onCargado) {
        super(dueno, Textos.TITULO_RANURAS, ModalityType.APPLICATION_MODAL);
        this.presentador = presentador;
        this.onUsado = onUsado;
        this.onCargado = onCargado;
        lista.setName("slot-list");
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.addListSelectionListener(evento -> botonesRefrescar());
        save.setName("save-button");
        cargar.setName("load-button");
        save.addActionListener(evento -> onGuardar());
        cargar.addActionListener(evento -> onCargar());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sur.add(save);
        sur.add(cargar);
        sur.add(mensaje);
        setLayout(new BorderLayout());
        add(lista, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(evento -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        recargar();
        setSize(560, 220);
        setLocationRelativeTo(dueno);
    }

    private int ranuraSeleccionada() {
        return lista.getSelectedIndex() + 1;
    }

    private void onGuardar() {
        int ranura = ranuraSeleccionada();
        if (ranura < 1) {
            return;
        }
        String texto = presentador.save(ranura, () -> JOptionPane.showConfirmDialog(this, Textos.SOBRESCRIBIR_CONFIRMAR,
                Textos.TITULO_RANURAS, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
        if (!texto.isEmpty()) {
            mensaje.setText(texto);
            onUsado.accept(ranura);
        }
        recargar();
    }

    private void onCargar() {
        int ranura = ranuraSeleccionada();
        if (ranura < 1) {
            return;
        }
        var resultado = presentador.cargar(ranura);
        if (resultado.isOk()) {
            onUsado.accept(ranura);
            onCargado.run();
            dispose();
        } else {
            mensaje.setText(resultado.mensaje());
            recargar();
        }
    }

    private void recargar() {
        int seleccionado = lista.getSelectedIndex();
        articulos.clear();
        presentador.lineas().forEach(articulos::addElement);
        if (!presentador.error().isEmpty()) {
            mensaje.setText(presentador.error());
        }
        if (seleccionado >= 0 && seleccionado < articulos.size()) {
            lista.setSelectedIndex(seleccionado);
        }
        botonesRefrescar();
    }

    private void botonesRefrescar() {
        int ranura = ranuraSeleccionada();
        save.setEnabled(ranura >= 1);
        cargar.setEnabled(ranura >= 1 && presentador.puedeCargar(ranura));
    }
}
