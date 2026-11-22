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
 * The summary shown when a day ends (HU-17): income, expenses, balances and what happened. It does not block
 * the game; {@code Esc} or the button close it.
 *
 * <p>Stories: HU-17.
 */
public final class DaySummaryDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int PADDING = 12;

    /**
     * Builds the dialog.
     *
     * @param owner   the window it belongs to
     * @param summary what happened during the day
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
