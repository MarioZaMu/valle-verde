package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.components.PlotText;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

/**
 * Pattern: Observer — Role: ConcreteObserver (the farm map, repainted when a map event arrives).
 *
 * <p>The panel keeps the last {@link MapView}. It asks the controller for a new one only when an event says
 * the map changed (game started, day ended, map changed): it never polls the game. Its own {@link Timer}
 * only draws frames of the {@link VisualEffects} (rain, snow, night tint, animals, growing crops) while
 * something moves, and stops when nothing does; the effects can be switched off at any time and never touch
 * the game (HU-40, HU-41). Hovering a plot shows its tooltip after {@link #TOOLTIP_DELAY_MS}, and clicking
 * tells the {@link PlotClickListener}.
 *
 * <p>Stories: HU-07, HU-25, HU-35, HU-40, HU-41.
 */
public final class MapPanel extends JPanel implements GameListener {

    /** Delay before the plot tooltip appears (HU-07). */
    public static final int TOOLTIP_DELAY_MS = 300;

    /** Milliseconds between two frames of the effects: about 30 frames per second. */
    public static final int FRAME_DELAY_MS = 33;

    private static final long serialVersionUID = 1L;
    private static final Dimension PREFERRED = new Dimension(1024, 640);
    private static final String GREENHOUSE = "GREENHOUSE";
    private static final long PARTICLE_SEED = 20_261_122L;

    private final transient GameCommands controller;
    private final transient PlotClickListener clicks;
    private final transient MapRenderer renderer = new MapRenderer();
    private final transient VisualEffects effects = new VisualEffects(() -> System.nanoTime() / 1_000_000L, new Random(PARTICLE_SEED));
    private final transient Timer frames = new Timer(FRAME_DELAY_MS, event -> nextFrame());
    private transient MapView view;
    private transient CellPos hover;

    /**
     * Builds the panel and subscribes it to the event bus.
     *
     * @param controller entry point to the game
     * @param bus        where game events are published
     * @param clicks     told when a plot is clicked
     */
    public MapPanel(GameCommands controller, EventBus bus, PlotClickListener clicks) {
        this.controller = controller;
        this.clicks = clicks;
        setPreferredSize(PREFERRED);
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(TOOLTIP_DELAY_MS);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                CellPos pos = cellAt(e.getX(), e.getY());
                if (pos == null ? hover != null : !pos.equals(hover)) {
                    hover = pos;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = null;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        bus.subscribe(this);
        Result<HudView> hud = controller.hud();
        if (hud.isOk()) {
            effects.onWorld(hud.value().season(), hud.value().weather());
        }
        refresh();
    }

    /** The map currently shown, or {@code null} before a game starts. */
    public MapView currentView() {
        return view;
    }

    /** The effects this panel draws; the screen switches them on and off through here. */
    public VisualEffects effects() {
        return effects;
    }

    /** Switches the visual effects and the animations on or off; the game is not touched. */
    public void setEffectsEnabled(boolean on) {
        effects.setEnabled(on);
        startOrStopFrames();
        repaint();
    }

    /** True while the frame timer is running, that is, while something is moving on the map. */
    public boolean isAnimating() {
        return frames.isRunning();
    }

    /** Tells the click listener which plot was clicked; a click outside the map does nothing. */
    public void handleClick(int x, int y) {
        CellPos pos = cellAt(x, y);
        if (pos != null) {
            clicks.plotClicked(pos.col(), pos.row());
        }
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof GameEvent.GameStarted || event instanceof GameEvent.DayEnded
                || event instanceof GameEvent.MapChanged) {
            if (SwingUtilities.isEventDispatchThread()) {
                react(event);
            } else {
                SwingUtilities.invokeLater(() -> react(event));
            }
        }
    }

    private void react(GameEvent event) {
        if (event instanceof GameEvent.GameStarted started) {
            effects.reset();
            effects.onWorld(started.hud().season(), started.hud().weather());
        } else if (event instanceof GameEvent.DayEnded ended) {
            effects.onWorld(ended.hud().season(), ended.hud().weather());
            effects.onDayEnded();
        }
        refresh();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        CellPos pos = cellAt(event.getX(), event.getY());
        if (pos == null) {
            return null;
        }
        BuildingView building = view == null ? null : view.buildingAt(pos.col(), pos.row());
        if (building != null && !GREENHOUSE.equals(building.type())) {
            return PlotText.describe(building);
        }
        Result<PlotView> plot = controller.inspect(pos.col(), pos.row());
        return plot.isOk() ? PlotText.describe(plot.value()) : null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (view != null) {
            renderer.render((Graphics2D) g, view, getWidth(), getHeight(), hover, effects);
        }
    }

    private void refresh() {
        Result<MapView> result = controller.map();
        if (result.isOk()) {
            view = result.value();
            effects.onMap(view);
            startOrStopFrames();
            repaint();
        }
    }

    private void nextFrame() {
        effects.step();
        repaint();
        if (!effects.needsFrames()) {
            frames.stop();
        }
    }

    private void startOrStopFrames() {
        if (effects.needsFrames()) {
            if (!frames.isRunning()) {
                frames.start();
            }
        } else {
            frames.stop();
        }
    }

    private CellPos cellAt(int x, int y) {
        if (view == null) {
            return null;
        }
        return MapGeometry.fit(view.cols(), view.rows(), getWidth(), getHeight()).cellAt(x, y);
    }
}
