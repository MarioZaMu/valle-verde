package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.BuildingView;
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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

/**
 * Pattern: Observer — Role: ConcreteObserver (the farm map, repainted when a map event arrives).
 *
 * <p>The panel keeps the last {@link MapView}. It asks the controller for a new one only when an event says
 * the map changed (game started, day ended, map changed): it never polls and has no timer. Hovering a plot
 * shows its tooltip after {@link #TOOLTIP_DELAY_MS}, and clicking tells the {@link PlotClickListener}.
 *
 * <p>Stories: HU-07, HU-35.
 */
public final class MapPanel extends JPanel implements GameListener {

    /** Delay before the plot tooltip appears (HU-07). */
    public static final int TOOLTIP_DELAY_MS = 300;

    private static final long serialVersionUID = 1L;
    private static final Dimension PREFERRED = new Dimension(1024, 600);

    private final transient GameCommands controller;
    private final transient PlotClickListener clicks;
    private final transient MapRenderer renderer = new MapRenderer();
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
        refresh();
    }

    /** The map currently shown, or {@code null} before a game starts. */
    public MapView currentView() {
        return view;
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
                refresh();
            } else {
                SwingUtilities.invokeLater(this::refresh);
            }
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        CellPos pos = cellAt(event.getX(), event.getY());
        if (pos == null) {
            return null;
        }
        BuildingView building = view == null ? null : view.buildingAt(pos.col(), pos.row());
        if (building != null) {
            return PlotText.describe(building);
        }
        Result<PlotView> plot = controller.inspect(pos.col(), pos.row());
        return plot.isOk() ? PlotText.describe(plot.value()) : null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (view != null) {
            renderer.render((Graphics2D) g, view, getWidth(), getHeight(), hover);
        }
    }

    private void refresh() {
        Result<MapView> result = controller.map();
        if (result.isOk()) {
            view = result.value();
            repaint();
        }
    }

    private CellPos cellAt(int x, int y) {
        if (view == null) {
            return null;
        }
        return MapGeometry.fit(view.cols(), view.rows(), getWidth(), getHeight()).cellAt(x, y);
    }
}
