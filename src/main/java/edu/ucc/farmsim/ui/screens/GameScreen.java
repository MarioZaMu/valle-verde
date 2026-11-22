package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.controller.GameController;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.ui.components.HudPanel;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * The main game screen: the top bar (HUD), the farm map, and the buttons
 * "End day" and "Menu". The map and the tools are added by the following stories.
 *
 * <p>Stories: HU-33, HU-34, HU-35.
 */
public final class GameScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * Builds the screen.
     *
     * @param controller entry point to the game
     * @param bus        where game events are published (the top bar listens to it)
     * @param navigator  switches screens and shows messages
     */
    public GameScreen(GameController controller, EventBus bus, ScreenNavigator navigator) {
        super(new BorderLayout());
        GameScreenPresenter presenter = new GameScreenPresenter(controller, navigator::showMessage);

        add(new HudPanel(bus), BorderLayout.NORTH);
        add(new MapPanel(controller, bus, (col, row) -> { }), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton endDay = new JButton(Texts.END_DAY);
        endDay.addActionListener(event -> presenter.onEndDay());
        JButton menu = new JButton(Texts.MENU);
        menu.addActionListener(event -> navigator.showStart());
        bottom.add(endDay);
        bottom.add(menu);
        add(bottom, BorderLayout.SOUTH);
    }
}
