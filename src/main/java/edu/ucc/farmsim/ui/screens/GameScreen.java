package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.ui.components.HudPanel;
import edu.ucc.farmsim.ui.components.Shortcut;
import edu.ucc.farmsim.ui.components.Tool;
import edu.ucc.farmsim.ui.components.ToolBar;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * The main game screen: the top bar (HUD), the farm map, the tool bar, a status line for messages and the
 * buttons "End day" and "Menu". Keys 1 to 5 pick a tool, Escape deselects it, and the shortcuts of RN-34 work
 * anywhere on the screen.
 *
 * <p>Stories: HU-33, HU-34, HU-35, HU-36.
 */
public final class GameScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String DESELECT = "deselect-tool";

    /**
     * Builds the screen.
     *
     * @param controller entry point to the game
     * @param bus        where game events are published (the top bar and the map listen to it)
     * @param navigator  switches screens
     */
    public GameScreen(GameCommands controller, EventBus bus, ScreenNavigator navigator) {
        super(new BorderLayout());
        ToolModel tools = new ToolModel();
        JLabel status = new JLabel(" ");
        GameScreenPresenter presenter = new GameScreenPresenter(controller, tools, status::setText);

        add(new HudPanel(bus), BorderLayout.NORTH);
        add(new MapPanel(controller, bus, presenter::onPlotClicked), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(new ToolBar(tools));
        bottom.add(status);
        JPanel buttons = new JPanel();
        JButton endDay = new JButton(Texts.END_DAY);
        endDay.addActionListener(event -> presenter.onEndDay());
        JButton menu = new JButton(Texts.MENU);
        menu.addActionListener(event -> navigator.showStart());
        buttons.add(endDay);
        buttons.add(menu);
        bottom.add(buttons);
        add(bottom, BorderLayout.SOUTH);

        bindKeys(tools, presenter);
    }

    private void bindKeys(ToolModel tools, GameScreenPresenter presenter) {
        InputMap keys = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        for (Tool tool : Tool.values()) {
            String name = "tool-" + tool.name();
            keys.put(KeyStroke.getKeyStroke(tool.keyCode(), 0), name);
            getActionMap().put(name, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    tools.select(tool);
                }
            });
        }
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), DESELECT);
        getActionMap().put(DESELECT, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                tools.deselect();
            }
        });
        for (Shortcut shortcut : Shortcut.values()) {
            String name = "shortcut-" + shortcut.name();
            keys.put(KeyStroke.getKeyStroke(shortcut.keyCode(), 0), name);
            getActionMap().put(name, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    presenter.onShortcut(shortcut);
                }
            });
        }
    }
}
