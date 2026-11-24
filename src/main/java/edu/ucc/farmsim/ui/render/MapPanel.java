package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.AvatarView;
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
import java.awt.GraphicsConfiguration;
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
 * <p>El panel guarda el último {@link MapView}. Pide uno nuevo al controlador solo cuando un evento dice que el mapa
 * cambió (empezó la partida, terminó el día, cambió el mapa): nunca sondea el juego. Su propio {@link Timer} solo
 * dibuja cuadros de los {@link VisualEffects} (lluvia, nieve, tinte de noche, animales, cultivos que crecen y la vida
 * ambiental del valle) mientras el panel está en pantalla y algo se mueve, y se detiene en caso contrario; los efectos
 * se pueden apagar en cualquier momento y nunca tocan el juego (HU-40, HU-41). Las barras que flotan sobre la vista se
 * registran con {@link #addFloating}. Las parcelas miden 48 píxeles de pantalla y una cámara sigue al avatar por el
 * valle, manteniéndolo siempre en la parte de la vista que las barras dejan libre (HU-48, HU-50). Pasar el cursor sobre
 * una parcela muestra su tooltip tras {@link #TOOLTIP_DELAY_MS}, y hacer clic avisa al {@link PlotClickListener}.
 *
 * <p>Stories: HU-07, HU-25, HU-35, HU-40, HU-41, HU-45, HU-46, HU-48, HU-50.
 */
public final class MapPanel extends JPanel implements GameListener {

    /** Retraso antes de que aparezca el tooltip de la parcela (HU-07). */
    public static final int TOOLTIP_DELAY_MS = 300;

    /** Milisegundos entre dos cuadros de los efectos: unos 30 cuadros por segundo. */
    public static final int FRAME_DELAY_MS = 33;

    /**
     * Milisegundos entre dos cuadros mientras el avatar camina: unos 60 cuadros por segundo, para que la cámara se
     * deslice.
     */
    public static final int WALK_FRAME_DELAY_MS = 16;

    private static final long serialVersionUID = 1L;
    private static final Dimension PREFERRED = new Dimension(1024, 640);
    private static final String GREENHOUSE = "GREENHOUSE";
    private static final long PARTICLE_SEED = 20_261_122L;
    private static final int RESERVE_GAP = 6;
    private static final int DEFAULT_COLS = 48;
    private static final int DEFAULT_ROWS = 24;

    private final transient GameCommands controller;
    private final transient PlotClickListener clicks;
    private final transient MapRenderer renderer = new MapRenderer();
    private final transient VisualEffects effects = new VisualEffects(() -> System.nanoTime() / 1_000_000L, new Random(PARTICLE_SEED));
    private final transient Timer frames = new Timer(FRAME_DELAY_MS, event -> nextFrame());
    private final transient List<JComponent> floating = new ArrayList<>();
    private transient MapView view;
    private transient CellPos hover;

    /**
     * Construye el panel y lo suscribe al bus de eventos.
     *
     * @param controller punto de entrada al juego
     * @param bus        donde se publican los eventos del juego
     * @param clicks     se le avisa cuando se hace clic en una parcela
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
     * Le pide otra vez el mapa a la partida y lo redibuja; se usa después de un paso bloqueado, porque entonces el
     * avatar solo giró y ningún evento lo anuncia.
     */
    public void reload() {
        refresh();
    }

    /** El mapa que se muestra ahora, o {@code null} antes de que empiece una partida. */
    public MapView currentView() {
        return view;
    }

    /** Los efectos que dibuja este panel; la pantalla los enciende y apaga por aquí. */
    public VisualEffects effects() {
        return effects;
    }

    /** Enciende o apaga los efectos visuales y las animaciones; no se toca la partida. */
    public void setEffectsEnabled(boolean on) {
        effects.setEnabled(on);
        startOrStopFrames();
        repaint();
    }

    /**
     * Registra una barra que flota sobre la vista: las parcelas se colocan de modo que ninguna quede debajo de ella,
     * mientras la tierra alrededor de la granja se sigue dibujando bajo ella.
     */
    public void addFloating(JComponent bar) {
        floating.add(bar);
    }

    /**
     * Dónde se dibuja ahora cada parcela: parcelas de {@link MapGeometry#SCREEN_CELL} píxeles de pantalla con la cámara
     * sobre el avatar (en el punto donde se dibuja mientras camina), mantenidas dentro de la parte del panel que dejan
     * libre las barras flotantes y sin mostrar nunca más allá de los bordes del valle (RN-40).
     */
    public MapGeometry geometry() {
        int cols = view == null ? DEFAULT_COLS : view.cols();
        int rows = view == null ? DEFAULT_ROWS : view.rows();
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        int cell = MapGeometry.cellFor(width, height, deviceScale());
        double[] focus = effects.avatarPosition();
        double col = focus == null ? cols / 2.0 : focus[0];
        double row = focus == null ? rows / 2.0 : focus[1];
        return MapGeometry.follow(cols, rows, cell, freeArea(width, height), col, row);
    }

    /** La parte del panel no cubierta por las barras que flotan sobre él, arriba y abajo. */
    private Rectangle freeArea(int width, int height) {
        int top = 0;
        int bottom = 0;
        for (JComponent bar : floating) {
            if (!bar.isVisible() || bar.getParent() == null || bar.getWidth() == 0) {
                continue;
            }
            Rectangle box = SwingUtilities.convertRectangle(bar.getParent(), bar.getBounds(), this);
            if (box.y + box.height / 2 < height / 2) {
                top = Math.max(top, box.y + box.height + RESERVE_GAP);
            } else {
                bottom = Math.max(bottom, height - box.y + RESERVE_GAP);
            }
        }
        if (top + bottom >= height) {
            return new Rectangle(0, 0, width, height);
        }
        return new Rectangle(0, top, width, height - top - bottom);
    }

    private double deviceScale() {
        GraphicsConfiguration config = getGraphicsConfiguration();
        if (config == null) {
            return 1.0;
        }
        double scale = config.getDefaultTransform().getScaleX();
        return scale > 0 ? scale : 1.0;
    }

    /** Verdadero mientras corre el temporizador de cuadros, es decir, mientras algo se mueve en el mapa. */
    public boolean isAnimating() {
        return frames.isRunning();
    }

    /** Le dice al oyente de clics qué parcela se pulsó; un clic fuera del mapa no hace nada. */
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
        } else if (event instanceof GameEvent.AvatarMoved moved && view != null) {
            show(view.withAvatar(moved.avatar()));
            return;
        }
        refresh();
    }

    /**
     * Un paso bloqueado solo giró al avatar: el mapa que se muestra también lo gira, sin llamar al servidor (pedir todo
     * el valle en cada paso hacía que la caminata diera tirones).
     *
     * @param direction hacia dónde mira ahora el avatar
     */
    public void face(String direction) {
        if (view != null && view.avatar() != null) {
            AvatarView avatar = view.avatar();
            show(view.withAvatar(new AvatarView(avatar.col(), avatar.row(), direction)));
        }
    }

    private void show(MapView next) {
        view = next;
        effects.onMap(view);
        startOrStopFrames();
        repaint();
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
            show(result.value());
        }
    }

    /**
     * Milisegundos entre los cuadros que se dibujan ahora: menos mientras se camina, para que la caminata se vea fluida
     * (HU-50 · CA-5).
     */
    public int frameDelay() {
        return frames.getDelay();
    }

    private void nextFrame() {
        frames.setDelay(effects.isWalking() ? WALK_FRAME_DELAY_MS : FRAME_DELAY_MS);
        effects.step();
        repaint();
        if (!effects.needsFrames() || !isShowing()) {
            frames.stop();
        }
    }

    private void startOrStopFrames() {
        frames.setDelay(effects.isWalking() ? WALK_FRAME_DELAY_MS : FRAME_DELAY_MS);
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
