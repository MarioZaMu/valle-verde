package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.components.PlotText;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

/**
 * Pattern: Observer — Role: ConcreteObserver (the farm map, repainted when a map event arrives).
 *
 * <p>The panel keeps the last {@link MapView}. It asks the controller for a new one only when an event says
 * the map changed (game started, day ended, map changed): it never polls the game. Its own {@link Timer}
 * only draws frames of the {@link VisualEffects} (rain, snow, night tint, animals, growing crops and the
 * ambient life of the valley) while the panel is on screen and something moves, and stops otherwise; the
 * effects can be switched off at any time and never touch the game (HU-40, HU-41). Bars that float over the
 * view are registered with {@link #addFloating}, and the plots are placed clear of them (HU-48). Hovering a plot
 * shows its tooltip after {@link #TOOLTIP_DELAY_MS}, and clicking tells the {@link PlotClickListener}.
 *
 * <p>Stories: HU-07, HU-25, HU-35, HU-40, HU-41, HU-45, HU-46, HU-48.
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
    private static final int RESERVE_GAP = 6;

    private final transient GameCommands controller;
    private final transient PlotClickListener clicks;
    private final transient MapRenderer renderer = new MapRenderer();
    private final transient VisualEffects effects = new VisualEffects(() -> System.nanoTime() / 1_000_000L, new Random(PARTICLE_SEED));
    private final transient Timer frames = new Timer(FRAME_DELAY_MS, event -> nextFrame());
    private final transient List<JComponent> floating = new ArrayList<>();
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
        effects.setAmbient(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                startOrStopFrames();
            }
        });
        bus.subscribe(this);
        Result<HudView> hud = controller.hud();
        if (hud.isOk()) {
            effects.onWorld(hud.value().season(), hud.value().weather());
        }
        refresh();
    }

    /**
     * Asks the game for the map again and redraws it; used after a step was blocked, because then the avatar
     * only turned and no event announces it.
     */
    public void reload() {
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

    /**
     * Registers a bar that floats over the view: the plots are placed so that none ends up under it, while the
     * land around the farm is still drawn below it.
     */
    public void addFloating(JComponent bar) {
        floating.add(bar);
    }

    /** Where each plot is drawn now, at the current size of the panel. */
    public MapGeometry geometry() {
        int cols = view == null ? 16 : view.cols();
        int rows = view == null ? 10 : view.rows();
        int top = 0;
        int bottom = 0;
        MapGeometry geometry = MapGeometry.fit(cols, rows, getWidth(), getHeight());
        for (int pass = 0; pass < 3; pass++) {
            Rectangle area = geometry.area();
            boolean changed = false;
            for (JComponent bar : floating) {
                if (!bar.isVisible() || bar.getParent() == null || bar.getWidth() == 0) {
                    continue;
                }
                Rectangle box = SwingUtilities.convertRectangle(bar.getParent(), bar.getBounds(), this);
                if (!box.intersects(area)) {
                    continue;
                }
                if (box.y + box.height / 2 < getHeight() / 2) {
                    int needed = box.y + box.height + RESERVE_GAP;
                    changed |= needed > top;
                    top = Math.max(top, needed);
                } else {
                    int needed = getHeight() - box.y + RESERVE_GAP;
                    changed |= needed > bottom;
                    bottom = Math.max(bottom, needed);
                }
            }
            if (!changed) {
                break;
            }
            geometry = MapGeometry.fit(cols, rows, getWidth(), getHeight(), top, bottom);
        }
        return geometry;
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
                || event instanceof GameEvent.MapChanged || event instanceof GameEvent.AvatarMoved
                || event instanceof GameEvent.ErrandCompleted) {
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
        NpcView npc = view == null ? null : view.npcAt(pos.col(), pos.row());
        if (npc != null) {
            return npc.name() + " · " + npc.role();
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
            renderer.render((Graphics2D) g, view, geometry(), getWidth(), getHeight(), hover, effects);
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
        if (!effects.needsFrames() || !isShowing()) {
            frames.stop();
        }
    }

    private void startOrStopFrames() {
        boolean onlyAmbient = effects.isAmbient() && !isShowing();
        if (effects.needsFrames() && !onlyAmbient) {
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
        return geometry().cellAt(x, y);
    }
}
