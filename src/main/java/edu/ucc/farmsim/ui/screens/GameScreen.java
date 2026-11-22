package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * The main game screen. In this phase it only has the way back to the start screen; the top bar, the farm
 * map and the tools are added by the following stories.
 *
 * <p>Stories: HU-33.
 */
public final class GameScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * Builds the screen.
     *
     * @param navigator switches screens
     */
    public GameScreen(ScreenNavigator navigator) {
        super(new BorderLayout());
        JPanel bottom = new JPanel();
        JButton menu = new JButton(Texts.MENU);
        menu.addActionListener(event -> navigator.showStart());
        bottom.add(menu);
        add(bottom, BorderLayout.SOUTH);
    }
}
