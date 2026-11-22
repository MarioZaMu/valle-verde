package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.GameInfo;
import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.event.EventBus;
import java.awt.CardLayout;
import java.awt.Window;
import java.util.function.LongSupplier;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Hosts the screens and switches between them (a card layout). It is the real {@link ScreenNavigator}:
 * it shows dialogs with Swing and closes the window when the player exits.
 *
 * <p>Stories: HU-33.
 */
public final class ScreenRouter extends JPanel implements ScreenNavigator {

    private static final long serialVersionUID = 1L;
    private static final String START = "start";
    private static final String GAME = "game";

    private final CardLayout cards = new CardLayout();

    /**
     * Builds the router with its screens, showing the start screen first.
     *
     * @param controller entry point to the game
     * @param bus        where game events are published
     * @param seeds      supplies the seed of each new game
     */
    public ScreenRouter(GameCommands controller, EventBus bus, LongSupplier seeds) {
        setLayout(cards);
        add(new StartScreen(new StartScreenPresenter(controller, this, seeds)), START);
        add(new GameScreen(controller, bus, this), GAME);
        showStart();
    }

    @Override
    public void showStart() {
        cards.show(this, START);
    }

    @Override
    public void showGame() {
        cards.show(this, GAME);
    }

    @Override
    public void showMessage(String text) {
        JOptionPane.showMessageDialog(this, text, GameInfo.NAME, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public boolean confirm(String text) {
        return JOptionPane.showConfirmDialog(this, text, GameInfo.NAME, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    @Override
    public void exit() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        System.exit(0);
    }
}
