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
 * Aloja las pantallas y alterna entre ellas (un diseño de tarjetas). Es el {@link ScreenNavigator} real: muestra
 * diálogos con Swing y cierra la ventana cuando el jugador sale.
 *
 * <p>La pantalla de inicio no le pide nada al servidor, así que se muestra de inmediato; la pantalla de juego, que lee
 * el mapa y el tutorial al construirse, se construye cuando el servidor está listo, o antes si el jugador pulsa un
 * botón primero (RNF-02).
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
     * Construye el enrutador con sus pantallas, mostrando primero la pantalla de inicio.
     *
     * @param controller punto de entrada al juego
     * @param bus        donde se publican los eventos del juego
     * @param seeds      provee la semilla de cada partida nueva
     */
    public ScreenRouter(GameCommands controller, EventBus bus, LongSupplier seeds) {
        this(controller, bus, seeds, CompletableFuture.completedFuture(null));
    }

    /**
     * Construye el enrutador para un servidor que quizá todavía esté arrancando: la pantalla de inicio se muestra de
     * inmediato y la pantalla de juego se construye cuando se completa {@code serverReady} (o cuando el jugador pulsa
     * un botón por primera vez).
     *
     * @param controller  punto de entrada al juego
     * @param bus         donde se publican los eventos del juego
     * @param seeds       provee la semilla de cada partida nueva
     * @param serverReady se completa cuando el servidor responde
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
