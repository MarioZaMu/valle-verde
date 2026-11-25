package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.evento.OyenteJuego;
import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * El cuadro del avatar del tutorial de inicio (HU-49): el retrato del granjero, «Paso n de 5», qué hacer ahora y el
 * botón «Saltar tutorial». Flota en la esquina superior izquierda de la granja mientras corre el tutorial y se oculta
 * solo cuando termina. Sigue la partida mediante sus eventos; la lógica está en {@link PresentadorTutorial}.
 *
 * <p>Stories: HU-49.
 */
public final class CajaTutorial extends CajaMadera implements OyenteJuego {

    /**
     * Lo más ancho que llega a ser el cuadro, para que quepa junto a la barra superior en una ventana de 1024 x 768.
     */
    public static final int ANCHO_MAXIMO = 480;
    /** Lo más alto que llega a ser el cuadro. */
    public static final int ALTO_MAXIMO = 140;

    private static final long serialVersionUID = 1L;
    private static final int RETRATO = 64;
    private static final int ANCHO_TEXTO = ANCHO_MAXIMO - RETRATO - 40;

    private final transient PresentadorTutorial presentador;
    private final JLabel retrato = DialogoConversacion.retrato("characters/AVATAR_S_0.png", RETRATO);
    private final JLabel paso = new JLabel();
    private final JTextArea mensaje = new JTextArea();
    private final BotonMadera saltar = new BotonMadera(Textos.TUTORIAL_SALTAR);

    /**
     * Construye el cuadro, oculto hasta que {@link #refrescar()} encuentre un tutorial en marcha.
     *
     * @param presentador la lógica del cuadro
     */
    public CajaTutorial(PresentadorTutorial presentador) {
        super(new BorderLayout(10, 2));
        this.presentador = presentador;
        setName("tutorial-box");
        add(retrato, BorderLayout.WEST);

        paso.setForeground(EstiloMadera.TINTA);
        paso.setFont(paso.getFont().deriveFont(Font.BOLD));
        mensaje.setName("tutorial-text");
        mensaje.setEditable(false);
        mensaje.setFocusable(false);
        mensaje.setOpaque(false);
        mensaje.setLineWrap(true);
        mensaje.setWrapStyleWord(true);
        mensaje.setBorder(BorderFactory.createEmptyBorder());
        mensaje.setMargin(new Insets(0, 0, 0, 0));
        mensaje.setForeground(EstiloMadera.TINTA);
        mensaje.setFont(paso.getFont().deriveFont(Font.PLAIN));

        JPanel arriba = new JPanel(new BorderLayout(8, 0));
        arriba.setOpaque(false);
        arriba.add(paso, BorderLayout.WEST);
        JPanel palabras = new JPanel(new BorderLayout(0, 4));
        palabras.setOpaque(false);
        palabras.add(arriba, BorderLayout.NORTH);
        palabras.add(mensaje, BorderLayout.CENTER);
        saltar.setName("skip-tutorial");
        saltar.setFocusable(false);
        saltar.addActionListener(evento -> {
            presentador.saltar();
            actualizar();
        });
        arriba.add(saltar, BorderLayout.EAST);
        add(palabras, BorderLayout.CENTER);
        setVisible(false);
    }

    /** Le pide el tutorial a la partida y muestra u oculta el cuadro. */
    public void refrescar() {
        presentador.recargar();
        actualizar();
    }

    @Override
    public void onEvento(EventoJuego evento) {
        if (evento instanceof EventoJuego.TutorialAvanzado avanzado) {
            onSwing(() -> {
                presentador.show(avanzado.tutorial());
                actualizar();
            });
        } else if (evento instanceof EventoJuego.JuegoIniciado) {
            onSwing(this::refrescar);
        }
    }

    /** En el hilo de Swing; un cuadro que todavía no está en pantalla (o en una prueba) se cambia de inmediato. */
    private void onSwing(Runnable cambiar) {
        if (SwingUtilities.isEventDispatchThread() || !isDisplayable()) {
            cambiar.run();
        } else {
            SwingUtilities.invokeLater(cambiar);
        }
    }

    private void actualizar() {
        paso.setText(presentador.textoPaso());
        mensaje.setText(presentador.textoMensaje());
        mensaje.setPreferredSize(null);
        mensaje.setSize(ANCHO_TEXTO, Short.MAX_VALUE);
        mensaje.setPreferredSize(new Dimension(ANCHO_TEXTO, mensaje.getPreferredSize().height));
        boolean visible = presentador.esActivo();
        if (visible != isVisible()) {
            setVisible(visible);
        }
        revalidate();
        repaint();
        if (getParent() != null) {
            getParent().revalidate();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.min(ANCHO_MAXIMO, size.width), size.height);
    }

    /** «Paso n de 5» tal como se muestra. */
    public String textoPaso() {
        return paso.getText();
    }

    /** Lo que dice el avatar, tal como se muestra. */
    public String textoMensaje() {
        return mensaje.getText();
    }

    /** El botón que salta el tutorial. */
    public BotonMadera botonSaltar() {
        return saltar;
    }

    /** Verdadero si se dibuja el retrato del granjero (solo falta cuando no hay sprites). */
    public boolean tieneRetrato() {
        return retrato.getIcon() != null;
    }
}
