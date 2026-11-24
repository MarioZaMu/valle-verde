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
import edu.ucc.farmsim.ui.components.WalkKeys;
import edu.ucc.farmsim.ui.components.WoodBox;
import edu.ucc.farmsim.ui.components.WoodButton;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.render.VisualEffects;
import edu.ucc.farmsim.ui.theme.Texts;
import edu.ucc.farmsim.ui.theme.WoodStyle;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
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
import javax.swing.Timer;

/**
 * La pantalla principal del juego: la granja llena la ventana y las barras flotan sobre ella en cajas de madera, la
 * fecha, el clima, el dinero y la energía en la esquina superior derecha (HUD), y abajo la barra de herramientas, una
 * línea de estado para mensajes y los botones «Terminar día» y «Menú». Las teclas 1 a 5 eligen una herramienta, Escape
 * la deselecciona, y los atajos de RN-34 funcionan en cualquier parte de la pantalla. Mientras corre el tutorial de
 * inicio, el cuadro del avatar flota en la esquina superior izquierda (HU-49).
 *
 * <p>Stories: HU-17, HU-18, HU-20, HU-22, HU-23, HU-26, HU-29, HU-30, HU-33, HU-34, HU-35, HU-36, HU-37, HU-38, HU-39, HU-40, HU-41, HU-45, HU-46, HU-48, HU-49, HU-50.
 */
public final class GameScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String DESELECT = "deselect-tool";
    private static final String USE_TOOL = "use-tool";
    private static final String TALK = "talk";
    private static final int GAP = 6;
    private static final int MARGIN = 8;
    private static final int WALK_TICK_MS = 15;

    /**
     * Construye la pantalla.
     *
     * @param controller punto de entrada al juego
     * @param bus        donde se publican los eventos del juego (la barra superior y el mapa lo escuchan)
     * @param navigator  cambia las pantallas
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
        JButton save = new WoodButton(Texts.SAVE);
        save.setToolTipText(Texts.SAVE_HINT);
        save.addActionListener(event -> presenter.onSave());
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
        buttons.add(save);
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

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    /**
     * Un paso. Cuando está bloqueado y otra tecla pulsada apunta en ángulo recto (caminando en diagonal a lo largo de
     * una cerca de árboles), se prueba ese camino en su lugar; un paso que sigue bloqueado solo gira al avatar, lo que
     * ningún evento anuncia, así que el mapa que se muestra lo gira sin pedirle al servidor todo el valle.
     */
    private static void step(GameScreenPresenter presenter, MapPanel map, WalkKeys walking, String direction) {
        if (presenter.onMove(direction)) {
            return;
        }
        String instead = walking.alternative(direction);
        if (instead != null && presenter.onMove(instead)) {
            walking.steppedInstead(instead);
            return;
        }
        map.face(direction);
    }

    private static JPanel clear(JPanel panel) {
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Las flechas y W A S D hacen caminar al avatar: una pulsación da un paso de inmediato y las teclas mantenidas
     * siguen caminando, una parcela cada {@link VisualEffects#STEP_MS}, sin pausa después del primer paso; dos teclas
     * perpendiculares (W y D) caminan en diagonal (RN-40). Espacio usa la herramienta seleccionada en la parcela de
     * enfrente (HU-45) y E habla con la persona junto al avatar (HU-46).
     */
    private void bindWalking(GameScreenPresenter presenter, MapPanel map) {
        InputMap keys = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        WalkKeys walking = new WalkKeys(VisualEffects.STEP_MS);
        Timer pace = new Timer(WALK_TICK_MS, null);
        pace.addActionListener(event -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window == null || !window.isFocused()) {
                walking.clear();
            }
            String direction = walking.tick(now());
            if (direction != null) {
                step(presenter, map, walking, direction);
            }
            if (!walking.isWalking()) {
                pace.stop();
            }
        });
        Object[][] walks = {
            {"NORTH", KeyEvent.VK_UP, KeyEvent.VK_W}, {"SOUTH", KeyEvent.VK_DOWN, KeyEvent.VK_S},
            {"EAST", KeyEvent.VK_RIGHT, KeyEvent.VK_D}, {"WEST", KeyEvent.VK_LEFT, KeyEvent.VK_A},
        };
        for (Object[] walk : walks) {
            String direction = (String) walk[0];
            String down = "walk-" + direction;
            String up = "stop-" + direction;
            for (int i = 1; i <= 2; i++) {
                keys.put(KeyStroke.getKeyStroke((Integer) walk[i], 0, false), down);
                keys.put(KeyStroke.getKeyStroke((Integer) walk[i], 0, true), up);
            }
            getActionMap().put(down, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    String first = walking.press(direction, now());
                    if (first != null) {
                        step(presenter, map, walking, first);
                        pace.start();
                    }
                }
            });
            getActionMap().put(up, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    walking.release(direction);
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
