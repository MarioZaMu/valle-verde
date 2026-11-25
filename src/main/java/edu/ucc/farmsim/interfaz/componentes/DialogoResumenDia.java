package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.BorderLayout;
import java.awt.Window;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * El resumen que se muestra cuando termina un día (HU-17): ingresos, gastos, saldos y lo que pasó. No bloquea el juego;
 * {@code Esc} o el botón lo cierran.
 *
 * <p>Stories: HU-17.
 */
public final class DialogoResumenDia extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int ACOLCHADO = 12;

    /**
     * Construye el diálogo.
     *
     * @param dueno   la ventana a la que pertenece
     * @param resumen lo que pasó durante el día
     */
    public DialogoResumenDia(Window dueno, VistaResumenDia resumen) {
        super(dueno, Textos.TITULO_RESUMEN, ModalityType.MODELESS);
        JPanel lineas = new JPanel();
        lineas.setLayout(new BoxLayout(lineas, BoxLayout.Y_AXIS));
        lineas.setBorder(new EmptyBorder(ACOLCHADO, ACOLCHADO, ACOLCHADO, ACOLCHADO));
        for (String linea : TextoResumenDia.lineas(resumen)) {
            JLabel etiqueta = new JLabel(linea);
            etiqueta.setName("summary-line");
            lineas.add(etiqueta);
        }
        JButton close = new JButton(Textos.CERRAR);
        close.addActionListener(evento -> dispose());
        setLayout(new BorderLayout());
        add(lineas, BorderLayout.CENTER);
        add(close, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(evento -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(dueno);
    }
}
