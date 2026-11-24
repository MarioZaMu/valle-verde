package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.ui.theme.Texts;
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
public final class DaySummaryDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int PADDING = 12;

    /**
     * Construye el diálogo.
     *
     * @param owner   la ventana a la que pertenece
     * @param summary lo que pasó durante el día
     */
    public DaySummaryDialog(Window owner, DaySummaryView summary) {
        super(owner, Texts.SUMMARY_TITLE, ModalityType.MODELESS);
        JPanel lines = new JPanel();
        lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
        lines.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        for (String line : DaySummaryText.lines(summary)) {
            JLabel label = new JLabel(line);
            label.setName("summary-line");
            lines.add(label);
        }
        JButton close = new JButton(Texts.CLOSE);
        close.addActionListener(event -> dispose());
        setLayout(new BorderLayout());
        add(lines, BorderLayout.CENTER);
        add(close, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }
}
