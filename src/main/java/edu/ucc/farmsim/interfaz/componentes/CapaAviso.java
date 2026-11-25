package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.interfaz.tema.Paleta;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Dibuja los avisos de una {@link ColaAviso} en la esquina inferior derecha, sobre lo que haya debajo, y deja pasar
 * todos los eventos del ratón. Un {@link Timer} de Swing la repinta solo mientras hay algo que mostrar.
 *
 * <p>Stories: HU-39.
 */
public final class CapaAviso extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final int MS_MARCO = 33;
    private static final int MARGEN = 16;
    private static final int RELLENO = 10;
    private static final int HUECO = 8;
    private static final int ARCO = 14;

    private final transient ColaAviso cola;
    private final transient Timer temporizador;
    private boolean mostrandoFue;

    /**
     * Crea la capa.
     *
     * @param cola los avisos que se dibujan
     */
    public CapaAviso(ColaAviso cola) {
        this.cola = cola;
        setOpaque(false);
        temporizador = new Timer(MS_MARCO, evento -> {
            boolean mostrando = !cola.esOcioso();
            if (mostrando || mostrandoFue) {
                repaint();
            }
            mostrandoFue = mostrando;
        });
        temporizador.setRepeats(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        temporizador.start();
    }

    @Override
    public void removeNotify() {
        temporizador.stop();
        super.removeNotify();
    }

    /** Nunca atrapa el ratón: el mapa de abajo sigue recibiendo los clics. */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    @Override
    protected void paintComponent(Graphics graficos) {
        List<VistaAviso> avisos = cola.visible();
        if (avisos.isEmpty()) {
            return;
        }
        Graphics2D g = (Graphics2D) graficos.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(getFont() == null ? new Font(Font.SANS_SERIF, Font.BOLD, 14) : getFont().deriveFont(Font.BOLD, 14f));
            FontMetrics metricas = g.getFontMetrics();
            int height = metricas.getHeight() + 2 * RELLENO;
            int y = getHeight() - MARGEN - height;
            for (int i = avisos.size() - 1; i >= 0; i--) {
                VistaAviso aviso = avisos.get(i);
                String texto = aviso.categoria().simbolo() + "  " + aviso.texto();
                int width = metricas.stringWidth(texto) + 2 * RELLENO;
                int x = getWidth() - MARGEN - width;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, aviso.opacidad())));
                g.setColor(fondo(aviso.categoria()));
                g.fillRoundRect(x, y, width, height, ARCO, ARCO);
                g.setColor(Paleta.TEXTO_AVISO);
                g.drawString(texto, x + RELLENO, y + RELLENO + metricas.getAscent());
                y -= height + HUECO;
            }
        } finally {
            g.dispose();
        }
    }

    private static Color fondo(CategoriaAviso categoria) {
        return switch (categoria) {
            case ERROR -> Paleta.ERROR_AVISO;
            case SUCCESS -> Paleta.EXITO_AVISO;
            case ACHIEVEMENT -> Paleta.LOGRO_AVISO;
            case INFO -> Paleta.INFO_AVISO;
        };
    }
}
