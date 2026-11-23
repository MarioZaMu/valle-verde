package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.GameInfo;
import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.event.EventBus;
import java.awt.CardLayout;
import java.awt.Window;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.LongSupplier;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Hosts the screens and switches between them (a card layout). It is the real {@link ScreenNavigator}:
 * it shows dialogs with Swing and closes the window when the player exits.
 *
 * <p>The start screen asks the server nothing, so it is shown at once; the game screen, which reads the map and
 * the tutorial when it is built, is built once the server is up, or earlier if the player presses a button
 * before (RNF-02).
 *
 * <p>Stories: HU-33.
 */
public final class ScreenRouter extends JPanel implements ScreenNavigator {

    private static final long serialVersionUID = 1L;
    private static final String START = "start";
    private static final String GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final transient GameCommands controller;
    private final transient EventBus bus;
    private boolean gameBuilt;

    /**
     * Builds the router with its screens, showing the start screen first.
     *
     * @param controller entry point to the game
     * @param bus        where game events are published
     * @param seeds      supplies the seed of each new game
     */
    public ScreenRouter(GameCommands controller, EventBus bus, LongSupplier seeds) {
        this(controller, bus, seeds, CompletableFuture.completedFuture(null));
    }

    /**
     * Builds the router for a server that may still be starting: the start screen shows at once and the game
     * screen is built when {@code serverReady} completes (or when the player first presses a button).
     *
     * @param controller  entry point to the game
     * @param bus         where game events are published
     * @param seeds       supplies the seed of each new game
     * @param serverReady completes when the server answers
     */
    public ScreenRouter(GameCommands controller, EventBus bus, LongSupplier seeds, CompletionStage<?> serverReady) {
        this.controller = controller;
        this.bus = bus;
        setLayout(cards);
        add(new StartScreen(new StartScreenPresenter(controller, this, seeds)), START);
        showStart();
        if (serverReady.toCompletableFuture().isDone()) {
            prepareGame();
        } else {
            serverReady.whenComplete((ok, error) -> SwingUtilities.invokeLater(this::prepareGame));
        }
    }

    @Override
    public void prepareGame() {
        if (!gameBuilt) {
            gameBuilt = true;
            add(new GameScreen(controller, bus, this), GAME);
        }
    }

    @Override
    public void showStart() {
        cards.show(this, START);
    }

    @Override
    public void showGame() {
        prepareGame();
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
    public int choose(String title, java.util.List<String> options) {
        Object picked = JOptionPane.showInputDialog(this, title, GameInfo.NAME, JOptionPane.QUESTION_MESSAGE, null,
                options.toArray(), options.isEmpty() ? null : options.get(0));
        return picked == null ? 0 : options.indexOf(picked) + 1;
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
