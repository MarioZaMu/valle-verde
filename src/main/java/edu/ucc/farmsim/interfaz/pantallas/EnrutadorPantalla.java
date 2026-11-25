package edu.ucc.farmsim.interfaz.pantallas;

import edu.ucc.farmsim.InfoJuego;
import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import java.awt.CardLayout;
import java.awt.Window;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.LongSupplier;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Aloja las pantallas y alterna entre ellas (un diseño de tarjetas). Es el {@link NavegadorPantalla} real: muestra
 * diálogos con Swing y cierra la ventana cuando el jugador sale.
 *
 * <p>La pantalla de inicio no le pide nada al servidor, así que se muestra de inmediato; la pantalla de juego, que lee
 * el mapa y el tutorial al construirse, se construye cuando el servidor está listo, o antes si el jugador pulsa un
 * botón primero (RNF-02).
 *
 * <p>Stories: HU-33.
 */
public final class EnrutadorPantalla extends JPanel implements NavegadorPantalla {

    private static final long serialVersionUID = 1L;
    private static final String INICIO = "start";
    private static final String JUEGO = "game";

    private final CardLayout tarjetas = new CardLayout();
    private final transient ComandosJuego controlador;
    private final transient BusEvento bus;
    private boolean juegoConstruido;

    /**
     * Construye el enrutador con sus pantallas, mostrando primero la pantalla de inicio.
     *
     * @param controlador punto de entrada al juego
     * @param bus        donde se publican los eventos del juego
     * @param semillas      provee la semilla de cada partida nueva
     */
    public EnrutadorPantalla(ComandosJuego controlador, BusEvento bus, LongSupplier semillas) {
        this(controlador, bus, semillas, CompletableFuture.completedFuture(null));
    }

    /**
     * Construye el enrutador para un servidor que quizá todavía esté arrancando: la pantalla de inicio se muestra de
     * inmediato y la pantalla de juego se construye cuando se completa {@code servidorListo} (o cuando el jugador pulsa
     * un botón por primera vez).
     *
     * @param controlador  punto de entrada al juego
     * @param bus         donde se publican los eventos del juego
     * @param semillas       provee la semilla de cada partida nueva
     * @param servidorListo se completa cuando el servidor responde
     */
    public EnrutadorPantalla(ComandosJuego controlador, BusEvento bus, LongSupplier semillas, CompletionStage<?> servidorListo) {
        this.controlador = controlador;
        this.bus = bus;
        setLayout(tarjetas);
        add(new PantallaInicio(new PresentadorPantallaInicio(controlador, this, semillas)), INICIO);
        inicioMostrar();
        if (servidorListo.toCompletableFuture().isDone()) {
            juegoPreparar();
        } else {
            servidorListo.whenComplete((ok, error) -> SwingUtilities.invokeLater(this::juegoPreparar));
        }
    }

    @Override
    public void juegoPreparar() {
        if (!juegoConstruido) {
            juegoConstruido = true;
            add(new PantallaJuego(controlador, bus, this), JUEGO);
        }
    }

    @Override
    public void inicioMostrar() {
        tarjetas.show(this, INICIO);
    }

    @Override
    public void juegoMostrar() {
        juegoPreparar();
        tarjetas.show(this, JUEGO);
    }

    @Override
    public void mensajeMostrar(String texto) {
        JOptionPane.showMessageDialog(this, texto, InfoJuego.NOMBRE, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public boolean confirmar(String texto) {
        return JOptionPane.showConfirmDialog(this, texto, InfoJuego.NOMBRE, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    @Override
    public int elegir(String titulo, java.util.List<String> opciones) {
        Object escogido = JOptionPane.showInputDialog(this, titulo, InfoJuego.NOMBRE, JOptionPane.QUESTION_MESSAGE, null,
                opciones.toArray(), opciones.isEmpty() ? null : opciones.get(0));
        return escogido == null ? 0 : opciones.indexOf(escogido) + 1;
    }

    @Override
    public void exit() {
        Window ventana = SwingUtilities.getWindowAncestor(this);
        if (ventana != null) {
            ventana.dispose();
        }
        System.exit(0);
    }
}
