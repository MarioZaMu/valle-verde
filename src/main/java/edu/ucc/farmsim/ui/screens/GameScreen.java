package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.ui.components.AchievementsDialog;
import edu.ucc.farmsim.ui.components.AchievementsPresenter;
import edu.ucc.farmsim.ui.components.BuildingDialog;
import edu.ucc.farmsim.ui.components.BuildingPresenter;
import edu.ucc.farmsim.ui.components.ConstructionDialog;
import edu.ucc.farmsim.ui.components.ConstructionPresenter;
import edu.ucc.farmsim.ui.components.DaySummaryDialog;
import edu.ucc.farmsim.ui.components.DialogueDialog;
import edu.ucc.farmsim.ui.components.DialoguePresenter;
import edu.ucc.farmsim.ui.components.HudPanel;
import edu.ucc.farmsim.ui.components.InventoryDialog;
import edu.ucc.farmsim.ui.components.InventoryModel;
import edu.ucc.farmsim.ui.components.MarketDialog;
import edu.ucc.farmsim.ui.components.MarketPresenter;
import edu.ucc.farmsim.ui.components.OverlayPanel;
import edu.ucc.farmsim.ui.components.Shortcut;
import edu.ucc.farmsim.ui.components.SlotsDialog;
import edu.ucc.farmsim.ui.components.SlotsPresenter;
import edu.ucc.farmsim.ui.components.ToastLayer;
import edu.ucc.farmsim.ui.components.ToastPresenter;
import edu.ucc.farmsim.ui.components.ToastQueue;
import edu.ucc.farmsim.ui.components.Tool;
import edu.ucc.farmsim.ui.components.ToolBar;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.components.TutorialBox;
import edu.ucc.farmsim.ui.components.TutorialPresenter;
import edu.ucc.farmsim.ui.components.WoodBox;
import edu.ucc.farmsim.ui.components.WoodButton;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.render.VisualEffects;
import edu.ucc.farmsim.ui.theme.Texts;
import edu.ucc.farmsim.ui.theme.WoodStyle;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * The main game screen: the farm fills the window and the bars float over it in wooden boxes, the date,
 * weather, money and energy in the top right corner (HUD), and at the bottom the tool bar, a status line for
 * messages and the buttons "End day" and "Menu". Keys 1 to 5 pick a tool, Escape deselects it, and the
 * shortcuts of RN-34 work anywhere on the screen. While the start tutorial runs, the avatar's box floats in the
 * top left corner (HU-49).
 *
 * <p>Stories: HU-17, HU-18, HU-20, HU-22, HU-23, HU-26, HU-29, HU-30, HU-33, HU-34, HU-35, HU-36, HU-37, HU-38, HU-39, HU-40, HU-41, HU-45, HU-46, HU-48, HU-49.
 */
public final class GameScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String DESELECT = "deselect-tool";
    private static final String USE_TOOL = "use-tool";
    private static final String TALK = "talk";
    private static final int GAP = 6;
    private static final int MARGIN = 8;

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
        InventoryModel inventory = new InventoryModel(controller, tools);
        bus.subscribe(inventory);
        inventory.refresh();
        GameScreenPresenter[] presenterRef = new GameScreenPresenter[1];
        GameDialogs dialogs = new GameDialogs() {
            @Override
            public void showMarket() {
                new MarketDialog(SwingUtilities.getWindowAncestor(GameScreen.this), new MarketPresenter(controller))
                        .setVisible(true);
            }

            @Override
            public void showInventory() {
                new InventoryDialog(SwingUtilities.getWindowAncestor(GameScreen.this), inventory).setVisible(true);
            }

            @Override
            public void showConstruction(int col, int row) {
                new ConstructionDialog(SwingUtilities.getWindowAncestor(GameScreen.this),
                        new ConstructionPresenter(controller, col, row)).setVisible(true);
            }

            @Override
            public void showBuilding(int buildingId) {
                new BuildingDialog(SwingUtilities.getWindowAncestor(GameScreen.this),
                        new BuildingPresenter(controller, buildingId)).setVisible(true);
            }

            @Override
            public void showAchievements() {
                new AchievementsDialog(SwingUtilities.getWindowAncestor(GameScreen.this),
                        new AchievementsPresenter(controller)).setVisible(true);
            }

            @Override
            public void showSlots(boolean saving) {
                new SlotsDialog(SwingUtilities.getWindowAncestor(GameScreen.this), new SlotsPresenter(controller),
                        slot -> presenterRef[0].useSlot(slot), () -> status.setText(Texts.LOADED_FROM_SLOT
                        + presenterRef[0].lastSlot() + ".")).setVisible(true);
            }

            @Override
            public void showSummary(DaySummaryView summary) {
                new DaySummaryDialog(SwingUtilities.getWindowAncestor(GameScreen.this), summary).setVisible(true);
            }

            @Override
            public void showDialogue(DialogueView dialogue) {
                new DialogueDialog(SwingUtilities.getWindowAncestor(GameScreen.this),
                        new DialoguePresenter(controller, dialogue)).setVisible(true);
            }
        };
        ToastQueue toastQueue = new ToastQueue(System::currentTimeMillis);
        ToastPresenter toasts = new ToastPresenter(toastQueue);
        bus.subscribe(toasts);
        GameScreenPresenter presenter = new GameScreenPresenter(controller, tools, status::setText, dialogs, toasts);
        presenterRef[0] = presenter;
        bus.subscribe(event -> {
            if (event instanceof GameEvent.DaySummaryReady ready) {
                presenter.onSummary(ready.summary());
            } else if (event instanceof GameEvent.AchievementUnlocked unlocked) {
                status.setText(Texts.achievementUnlocked(unlocked.name(), unlocked.reward()));
            }
        });

        MapPanel map = new MapPanel(controller, bus, presenter::onPlotClicked);
        HudPanel hud = new HudPanel(bus);
        TutorialBox tutorial = new TutorialBox(new TutorialPresenter(controller));
        bus.subscribe(tutorial);
        tutorial.refresh();
        JPanel tutorialRow = clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        tutorialRow.add(tutorial);
        JPanel top = clear(new JPanel(new BorderLayout(GAP, 0)));
        top.add(tutorialRow, BorderLayout.WEST);
        top.add(hud, BorderLayout.CENTER);

        WoodBox toolBox = new WoodBox(new BorderLayout());
        toolBox.add(new ToolBar(tools), BorderLayout.CENTER);
        JPanel toolRow = clear(new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)));
        toolRow.add(toolBox);

        status.setForeground(WoodStyle.INK);
        status.setFont(status.getFont().deriveFont(Font.BOLD));
        WoodBox statusBox = new WoodBox(new BorderLayout());
        statusBox.add(status, BorderLayout.CENTER);
        JButton endDay = new WoodButton(Texts.END_DAY);
        endDay.addActionListener(event -> presenter.onEndDay());
        JButton menu = new WoodButton(Texts.MENU);
        menu.addActionListener(event -> navigator.showStart());
        JCheckBox effects = new JCheckBox(Texts.EFFECTS, true);
        effects.setFocusable(false);
        effects.setOpaque(false);
        effects.setForeground(WoodStyle.INK);
        effects.addActionListener(event -> map.setEffectsEnabled(effects.isSelected()));
        WoodBox effectsBox = new WoodBox(new BorderLayout());
        effectsBox.add(effects, BorderLayout.CENTER);
        JPanel buttons = clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, 0)));
        buttons.add(endDay);
        buttons.add(menu);
        buttons.add(effectsBox);
        JPanel messageRow = clear(new JPanel(new BorderLayout(GAP, 0)));
        messageRow.add(statusBox, BorderLayout.CENTER);
        messageRow.add(buttons, BorderLayout.EAST);

        JPanel bottom = clear(new JPanel());
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(toolRow);
        bottom.add(Box.createVerticalStrut(GAP));
        bottom.add(messageRow);

        JPanel bars = clear(new JPanel(new BorderLayout()));
        bars.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
        bars.add(top, BorderLayout.NORTH);
        bars.add(bottom, BorderLayout.SOUTH);
        map.addFloating(hud.box());
        map.addFloating(tutorial);
        map.addFloating(bottom);

        OverlayPanel stage = new OverlayPanel(map);
        stage.addOnTop(bars);
        stage.addOnTop(new ToastLayer(toastQueue));
        add(stage, BorderLayout.CENTER);

        bindKeys(tools, presenter);
        bindWalking(presenter, map);
    }

    private static JPanel clear(JPanel panel) {
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Arrows and W A S D walk with the avatar, one plot per press; a held key walks at the pace of the walk
     * animation instead of the faster key repeat. Space uses the selected tool on the plot in front (HU-45) and
     * E talks to the person next to the avatar (HU-46).
     */
    private void bindWalking(GameScreenPresenter presenter, MapPanel map) {
        InputMap keys = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        long[] lastStep = {Long.MIN_VALUE / 2};
        Object[][] walks = {
            {"NORTH", KeyEvent.VK_UP, KeyEvent.VK_W}, {"SOUTH", KeyEvent.VK_DOWN, KeyEvent.VK_S},
            {"EAST", KeyEvent.VK_RIGHT, KeyEvent.VK_D}, {"WEST", KeyEvent.VK_LEFT, KeyEvent.VK_A},
        };
        for (Object[] walk : walks) {
            String direction = (String) walk[0];
            String name = "walk-" + direction;
            keys.put(KeyStroke.getKeyStroke((Integer) walk[1], 0), name);
            keys.put(KeyStroke.getKeyStroke((Integer) walk[2], 0), name);
            getActionMap().put(name, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    long now = System.nanoTime() / 1_000_000;
                    if (now - lastStep[0] < VisualEffects.STEP_MS) {
                        return;
                    }
                    lastStep[0] = now;
                    if (!presenter.onMove(direction)) {
                        map.reload();
                    }
                }
            });
        }
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), TALK);
        getActionMap().put(TALK, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                presenter.onTalkKey();
            }
        });
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), USE_TOOL);
        getActionMap().put(USE_TOOL, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                presenter.onUseTool();
            }
        });
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
