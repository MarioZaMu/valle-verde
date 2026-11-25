package edu.ucc.farmsim.interfaz.dibujo;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.evento.OyenteJuego;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.componentes.TextoParcela;
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
 * <p>El panel guarda el último {@link VistaMapa}. Pide uno nuevo al controlador solo cuando un evento dice que el mapa
 * cambió (empezó la partida, terminó el día, cambió el mapa): nunca sondea el juego. Su propio {@link Timer} solo
 * dibuja cuadros de los {@link EfectosVisuales} (lluvia, nieve, tinte de noche, animales, cultivos que crecen y la vida
 * ambiental del valle) mientras el panel está en pantalla y algo se mueve, y se detiene en caso contrario; los efectos
 * se pueden apagar en cualquier momento y nunca tocan el juego (HU-40, HU-41). Las barras que flotan sobre la vista se
 * registran con {@link #agregarFlotante}. Las parcelas miden 48 píxeles de pantalla y una cámara sigue al avatar por el
 * valle, manteniéndolo siempre en la parte de la vista que las barras dejan libre (HU-48, HU-50). Pasar el cursor sobre
 * una parcela muestra su tooltip tras {@link #MS_RETRASO_AYUDA}, y hacer clic avisa al {@link OyenteClicParcela}.
 *
 * <p>Stories: HU-07, HU-25, HU-35, HU-40, HU-41, HU-45, HU-46, HU-48, HU-50.
 */
public final class PanelMapa extends JPanel implements OyenteJuego {

    /** Retraso antes de que aparezca el tooltip de la parcela (HU-07). */
    public static final int MS_RETRASO_AYUDA = 300;

    /** Milisegundos entre dos cuadros de los efectos: unos 30 cuadros por segundo. */
    public static final int MS_RETRASO_MARCO = 33;

    /**
     * Milisegundos entre dos cuadros mientras el avatar camina: unos 60 cuadros por segundo, para que la cámara se
     * deslice.
     */
    public static final int MS_RETRASO_MARCO_CAMINAR = 16;

    private static final long serialVersionUID = 1L;
    private static final Dimension PREFERIDO = new Dimension(1024, 640);
    private static final String GREENHOUSE = "GREENHOUSE";
    private static final long SEMILLA_PARTICULA = 20_261_122L;
    private static final int HUECO_RESERVA = 6;
    private static final int COLS_PREDETERMINADOS = 48;
    private static final int FILAS_PREDETERMINADOS = 24;

    private final transient ComandosJuego controlador;
    private final transient OyenteClicParcela clics;
    private final transient DibujanteMapa dibujante = new DibujanteMapa();
    private final transient EfectosVisuales efectos = new EfectosVisuales(() -> System.nanoTime() / 1_000_000L, new Random(SEMILLA_PARTICULA));
    private final transient Timer marcos = new Timer(MS_RETRASO_MARCO, evento -> marcoSiguiente());
    private final transient List<JComponent> flotante = new ArrayList<>();
    private transient VistaMapa vista;
    private transient PosCelda sobrevolar;

    /**
     * Construye el panel y lo suscribe al bus de eventos.
     *
     * @param controlador punto de entrada al juego
     * @param bus        donde se publican los eventos del juego
     * @param clics     se le avisa cuando se hace clic en una parcela
     */
    public PanelMapa(ComandosJuego controlador, BusEvento bus, OyenteClicParcela clics) {
        this.controlador = controlador;
        this.clics = clics;
        setPreferredSize(PREFERIDO);
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(MS_RETRASO_AYUDA);
        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                PosCelda pos = celdaEn(e.getX(), e.getY());
                if (pos == null ? sobrevolar != null : !pos.equals(sobrevolar)) {
                    sobrevolar = pos;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sobrevolar = null;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                clicManejar(e.getX(), e.getY());
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
        efectos.ambienteFijar(true);
        addHierarchyListener(evento -> {
            if ((evento.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                iniciarOMarcosDetener();
            }
        });
        bus.suscribir(this);
        Resultado<VistaHud> hud = controlador.hud();
        if (hud.isOk()) {
            efectos.onMundo(hud.value().estacion(), hud.value().clima());
        }
        refrescar();
    }

    /**
     * Le pide otra vez el mapa a la partida y lo redibuja; se usa después de un paso bloqueado, porque entonces el
     * avatar solo giró y ningún evento lo anuncia.
     */
    public void recargar() {
        refrescar();
    }

    /** El mapa que se muestra ahora, o {@code null} antes de que empiece una partida. */
    public VistaMapa vistaActual() {
        return vista;
    }

    /** Los efectos que dibuja este panel; la pantalla los enciende y apaga por aquí. */
    public EfectosVisuales efectos() {
        return efectos;
    }

    /** Enciende o apaga los efectos visuales y las animaciones; no se toca la partida. */
    public void efectosFijarHabilitados(boolean on) {
        efectos.setEnabled(on);
        iniciarOMarcosDetener();
        repaint();
    }

    /**
     * Registra una barra que flota sobre la vista: las parcelas se colocan de modo que ninguna quede debajo de ella,
     * mientras la tierra alrededor de la granja se sigue dibujando bajo ella.
     */
    public void agregarFlotante(JComponent barra) {
        flotante.add(barra);
    }

    /**
     * Dónde se dibuja ahora cada parcela: parcelas de {@link GeometriaMapa#CELDA_PANTALLA} píxeles de pantalla con la cámara
     * sobre el avatar (en el punto donde se dibuja mientras camina), mantenidas dentro de la parte del panel que dejan
     * libre las barras flotantes y sin mostrar nunca más allá de los bordes del valle (RN-40).
     */
    public GeometriaMapa geometria() {
        int cols = vista == null ? COLS_PREDETERMINADOS : vista.cols();
        int filas = vista == null ? FILAS_PREDETERMINADOS : vista.filas();
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        int celda = GeometriaMapa.celdaPara(width, height, escalaDispositivo());
        double[] foco = efectos.posicionAvatar();
        double col = foco == null ? cols / 2.0 : foco[0];
        double fila = foco == null ? filas / 2.0 : foco[1];
        return GeometriaMapa.seguir(cols, filas, celda, areaLibre(width, height), col, fila);
    }

    /** La parte del panel no cubierta por las barras que flotan sobre él, arriba y abajo. */
    private Rectangle areaLibre(int width, int height) {
        int arriba = 0;
        int inferior = 0;
        for (JComponent barra : flotante) {
            if (!barra.isVisible() || barra.getParent() == null || barra.getWidth() == 0) {
                continue;
            }
            Rectangle caja = SwingUtilities.convertRectangle(barra.getParent(), barra.getBounds(), this);
            if (caja.y + caja.height / 2 < height / 2) {
                arriba = Math.max(arriba, caja.y + caja.height + HUECO_RESERVA);
            } else {
                inferior = Math.max(inferior, height - caja.y + HUECO_RESERVA);
            }
        }
        if (arriba + inferior >= height) {
            return new Rectangle(0, 0, width, height);
        }
        return new Rectangle(0, arriba, width, height - arriba - inferior);
    }

    private double escalaDispositivo() {
        GraphicsConfiguration configuracion = getGraphicsConfiguration();
        if (configuracion == null) {
            return 1.0;
        }
        double scale = configuracion.getDefaultTransform().getScaleX();
        return scale > 0 ? scale : 1.0;
    }

    /** Verdadero mientras corre el temporizador de cuadros, es decir, mientras algo se mueve en el mapa. */
    public boolean esAnimando() {
        return marcos.isRunning();
    }

    /** Le dice al oyente de clics qué parcela se pulsó; un clic fuera del mapa no hace nada. */
    public void clicManejar(int x, int y) {
        PosCelda pos = celdaEn(x, y);
        if (pos != null) {
            clics.parcelaClicada(pos.col(), pos.fila());
        }
    }

    @Override
    public void onEvento(EventoJuego evento) {
        if (evento instanceof EventoJuego.JuegoIniciado || evento instanceof EventoJuego.DiaTerminado
                || evento instanceof EventoJuego.MapaCambiado || evento instanceof EventoJuego.AvatarMovido
                || evento instanceof EventoJuego.EncargoCompletado) {
            if (SwingUtilities.isEventDispatchThread()) {
                reaccionar(evento);
            } else {
                SwingUtilities.invokeLater(() -> reaccionar(evento));
            }
        }
    }

    private void reaccionar(EventoJuego evento) {
        if (evento instanceof EventoJuego.JuegoIniciado iniciado) {
            efectos.reiniciar();
            efectos.onMundo(iniciado.hud().estacion(), iniciado.hud().clima());
        } else if (evento instanceof EventoJuego.DiaTerminado terminado) {
            efectos.onMundo(terminado.hud().estacion(), terminado.hud().clima());
            efectos.onDiaTerminado();
        } else if (evento instanceof EventoJuego.AvatarMovido movido && vista != null) {
            show(vista.conAvatar(movido.avatar()));
            return;
        }
        refrescar();
    }

    /**
     * Un paso bloqueado solo giró al avatar: el mapa que se muestra también lo gira, sin llamar al servidor (pedir todo
     * el valle en cada paso hacía que la caminata diera tirones).
     *
     * @param direccion hacia dónde mira ahora el avatar
     */
    public void cara(String direccion) {
        if (vista != null && vista.avatar() != null) {
            VistaAvatar avatar = vista.avatar();
            show(vista.conAvatar(new VistaAvatar(avatar.col(), avatar.fila(), direccion)));
        }
    }

    private void show(VistaMapa next) {
        vista = next;
        efectos.onMapa(vista);
        iniciarOMarcosDetener();
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent evento) {
        PosCelda pos = celdaEn(evento.getX(), evento.getY());
        if (pos == null) {
            return null;
        }
        VistaNpc npc = vista == null ? null : vista.npcEn(pos.col(), pos.fila());
        if (npc != null) {
            return npc.name() + " · " + npc.rol();
        }
        VistaEdificio edificio = vista == null ? null : vista.edificioEn(pos.col(), pos.fila());
        if (edificio != null && !GREENHOUSE.equals(edificio.tipo())) {
            return TextoParcela.describir(edificio);
        }
        Resultado<VistaParcela> parcela = controlador.inspeccionar(pos.col(), pos.fila());
        return parcela.isOk() ? TextoParcela.describir(parcela.value()) : null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vista != null) {
            dibujante.renderizar((Graphics2D) g, vista, geometria(), getWidth(), getHeight(), sobrevolar, efectos);
        }
    }

    private void refrescar() {
        Resultado<VistaMapa> resultado = controlador.mapa();
        if (resultado.isOk()) {
            show(resultado.value());
        }
    }

    /**
     * Milisegundos entre los cuadros que se dibujan ahora: menos mientras se camina, para que la caminata se vea fluida
     * (HU-50 · CA-5).
     */
    public int retrasoMarco() {
        return marcos.getDelay();
    }

    private void marcoSiguiente() {
        marcos.setDelay(efectos.esCaminando() ? MS_RETRASO_MARCO_CAMINAR : MS_RETRASO_MARCO);
        efectos.paso();
        repaint();
        if (!efectos.marcosNecesita() || !isShowing()) {
            marcos.stop();
        }
    }

    private void iniciarOMarcosDetener() {
        marcos.setDelay(efectos.esCaminando() ? MS_RETRASO_MARCO_CAMINAR : MS_RETRASO_MARCO);
        boolean ambienteSolo = efectos.esAmbiente() && !isShowing();
        if (efectos.marcosNecesita() && !ambienteSolo) {
            if (!marcos.isRunning()) {
                marcos.start();
            }
        } else {
            marcos.stop();
        }
    }

    private PosCelda celdaEn(int x, int y) {
        if (vista == null) {
            return null;
        }
        return geometria().celdaEn(x, y);
    }
}
