package edu.ucc.farmsim.interfaz.pantallas;

import edu.ucc.farmsim.InfoJuego;
import edu.ucc.farmsim.interfaz.componentes.BotonMadera;
import edu.ucc.farmsim.interfaz.dibujo.EscenaTitulo;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * La pantalla de inicio: una escena viva de granja ({@link EscenaTitulo}) con el nombre del juego arriba, flotando
 * suavemente, y los botones de madera «Nueva partida», «Cargar» y «Salir». Cada botón solo llama al {@link
 * PresentadorPantallaInicio}. La escena solo se anima mientras se muestra la pantalla.
 *
 * <p>Stories: HU-33, HU-48.
 */
public final class PantallaInicio extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final float TAMANIO_TITULO = 76f;
    private static final float TAMANIO_SUBTITULO = 18f;
    private static final float TAMANIO_BOTON = 22f;
    private static final int HUECO = 10;
    private static final int MS_MARCO = 33;
    private static final Dimension PX_TAMANIO_BOTON = new Dimension(320, 58);
    private static final Color ARRIBA_TITULO = new Color(0xFFE877);
    private static final Color INFERIOR_TITULO = new Color(0xF29A2E);
    private static final Color BORDE_TITULO = new Color(0x4A2A12);
    private static final Color SOMBRA_TITULO = new Color(0, 0, 0, 90);

    private final transient EscenaTitulo escena = new EscenaTitulo(GestorRecurso.compartido());
    private final Timer marcos = new Timer(MS_MARCO, evento -> repaint());
    private long mostradoEn = System.currentTimeMillis();

    /**
     * Construye la pantalla.
     *
     * @param presentador maneja las pulsaciones de los botones
     */
    public PantallaInicio(PresentadorPantallaInicio presentador) {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(HUECO, HUECO, HUECO, HUECO);
        c.fill = GridBagConstraints.HORIZONTAL;
        add(new Banner(), c);
        c.fill = GridBagConstraints.NONE;
        add(boton(Textos.JUEGO_INICIO_NUEVO, presentador::onJuegoNuevo), c);
        add(boton(Textos.CARGAR_INICIO, presentador::onCargar), c);
        add(boton(Textos.SALIR_INICIO, presentador::onSalir), c);
        c.weighty = 1;
        add(new JPanel() {
            private static final long serialVersionUID = 1L;

            {
                setOpaque(false);
            }
        }, c);
        addHierarchyListener(evento -> {
            if ((evento.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    mostradoEn = System.currentTimeMillis();
                    marcos.start();
                } else {
                    marcos.stop();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        escena.paint(g2, getWidth(), getHeight(), System.currentTimeMillis() - mostradoEn);
        g2.dispose();
    }

    private long transcurrido() {
        return System.currentTimeMillis() - mostradoEn;
    }

    private static JButton boton(String texto, Runnable accion) {
        JButton boton = new BotonMadera(texto);
        boton.setFont(boton.getFont().deriveFont(Font.BOLD, TAMANIO_BOTON));
        boton.setPreferredSize(PX_TAMANIO_BOTON);
        boton.addActionListener(evento -> accion.run());
        return boton;
    }

    /**
     * El nombre del juego en letras grandes doradas con un borde oscuro, flotando arriba y abajo, y una línea debajo.
     */
    private final class Banner extends JComponent {

        private static final long serialVersionUID = 1L;

        Banner() {
            setPreferredSize(new Dimension(720, 170));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            long t = transcurrido();
            double vaiven = Math.sin(t / 650.0) * 5;
            double aparecer = Math.min(1.0, t / 900.0);
            Font tipografia = getFont().deriveFont(Font.BOLD, TAMANIO_TITULO);
            GlyphVector glifos = tipografia.createGlyphVector(g2.getFontRenderContext(), InfoJuego.NOMBRE);
            double anchoTexto = glifos.getVisualBounds().getWidth();
            double x = (getWidth() - anchoTexto) / 2;
            double y = 92 + vaiven - (1 - aparecer) * 40;
            Shape contorno = glifos.getOutline();
            Shape colocado = AffineTransform.getTranslateInstance(x, y).createTransformedShape(contorno);
            Shape sombra = AffineTransform.getTranslateInstance(x + 5, y + 6).createTransformedShape(contorno);
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) aparecer));
            g2.setColor(SOMBRA_TITULO);
            g2.fill(sombra);
            g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(BORDE_TITULO);
            g2.draw(colocado);
            g2.setPaint(new GradientPaint(0, (float) (y - 60), ARRIBA_TITULO, 0, (float) y, INFERIOR_TITULO));
            g2.fill(colocado);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 255, 255, 120));
            g2.draw(AffineTransform.getTranslateInstance(x - 1, y - 2).createTransformedShape(contorno));

            Font pequenio = getFont().deriveFont(Font.BOLD, TAMANIO_SUBTITULO);
            FontMetrics metricas = g2.getFontMetrics(pequenio);
            int sw = metricas.stringWidth(Textos.SUBTITULO_INICIO);
            int bx = (getWidth() - sw) / 2 - 18;
            int by = (int) (y + 22);
            EstiloMadera.marcoPintar(g2, bx, by, sw + 36, metricas.getHeight() + 18);
            g2.setFont(pequenio);
            g2.setColor(EstiloMadera.TINTA);
            g2.drawString(Textos.SUBTITULO_INICIO, bx + 18, by + 9 + metricas.getAscent());
            g2.dispose();
        }
    }
}
