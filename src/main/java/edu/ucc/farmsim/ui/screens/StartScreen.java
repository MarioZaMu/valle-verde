package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.GameInfo;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The start screen: the game name and the buttons "New game", "Load" and "Exit". Every button just calls
 * the {@link StartScreenPresenter}.
 *
 * <p>Stories: HU-33.
 */
public final class StartScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final float TITLE_SIZE = 48f;
    private static final float BUTTON_SIZE = 22f;
    private static final int GAP = 12;
    private static final Dimension BUTTON_SIZE_PX = new Dimension(320, 56);

    /**
     * Builds the screen.
     *
     * @param presenter handles the button presses
     */
    public StartScreen(StartScreenPresenter presenter) {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(GAP, GAP, GAP, GAP);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(GameInfo.NAME, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, TITLE_SIZE));
        add(title, c);
        add(button(Texts.START_NEW_GAME, presenter::onNewGame), c);
        add(button(Texts.START_LOAD, presenter::onLoad), c);
        add(button(Texts.START_EXIT, presenter::onExit), c);
    }

    private static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(BUTTON_SIZE));
        button.setPreferredSize(BUTTON_SIZE_PX);
        button.addActionListener(event -> action.run());
        return button;
    }
}
