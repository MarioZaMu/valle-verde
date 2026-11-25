package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

/**
 * El cuadro de diálogo de una persona del valle, en la parte de abajo de la ventana como en un juego de rol (HU-46):
 * retrato, nombre y oficio, lo que dice, su encargo y los botones Entregar y Cerrar. Es modal; {@code Esc} o {@code E}
 * lo cierran. Toda la lógica está en {@link PresentadorConversacion}.
 *
 * <p>Stories: HU-46, HU-47, HU-49.
 */
public final class DialogoConversacion extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int RETRATO = 96;
    private static final int ANCHO_MAXIMO = 780;
    private static final int ALTO = 230;
    private static final int MARGEN = 40;
    private static final Color RECHAZADO = new Color(0x8E1F12);

    private final transient PresentadorConversacion presentador;
    private final JLabel name = new JLabel();
    private final JTextArea texto = new JTextArea();
    private final JLabel encargo = new JLabel();
    private final JLabel mensaje = new JLabel(" ");
    private final BotonMadera entregar = new BotonMadera(Textos.ENTREGAR);

    /**
     * Construye el diálogo.
     *
     * @param dueno     la ventana a la que pertenece
     * @param presentador la lógica de la conversación
     */
    public DialogoConversacion(Window dueno, PresentadorConversacion presentador) {
        super(dueno, presentador.vista().name(), ModalityType.APPLICATION_MODAL);
        this.presentador = presentador;
        setUndecorated(true);
        CajaMadera caja = new CajaMadera(new BorderLayout(12, 6));
        caja.add(retrato("characters/" + presentador.vista().npcId() + ".png", RETRATO), BorderLayout.WEST);

        JPanel palabras = new JPanel();
        palabras.setOpaque(false);
        palabras.setLayout(new BoxLayout(palabras, BoxLayout.Y_AXIS));
        name.setFont(name.getFont().deriveFont(Font.BOLD, name.getFont().getSize2D() + 4));
        name.setForeground(EstiloMadera.TINTA);
        texto.setName("dialogue-text");
        texto.setEditable(false);
        texto.setFocusable(false);
        texto.setOpaque(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setBorder(BorderFactory.createEmptyBorder());
        texto.setMargin(new java.awt.Insets(0, 0, 0, 0));
        texto.setForeground(EstiloMadera.TINTA);
        texto.setFont(name.getFont().deriveFont(Font.PLAIN, name.getFont().getSize2D() - 3));
        encargo.setFont(texto.getFont().deriveFont(Font.ITALIC));
        encargo.setForeground(EstiloMadera.TINTA);
        mensaje.setForeground(RECHAZADO);
        mensaje.setFont(mensaje.getFont().deriveFont(Font.BOLD));
        for (JComponent parte : new JComponent[] {name, texto, encargo, mensaje}) {
            parte.setAlignmentX(LEFT_ALIGNMENT);
            palabras.add(parte);
            palabras.add(Box.createVerticalStrut(4));
        }
        caja.add(palabras, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        entregar.setName("deliver-button");
        entregar.addActionListener(evento -> {
            presentador.entregar();
            refrescar();
        });
        BotonMadera close = new BotonMadera(Textos.CERRAR);
        close.setName("close-button");
        close.addActionListener(evento -> dispose());
        botones.add(entregar);
        botones.add(close);
        caja.add(botones, BorderLayout.SOUTH);
        setContentPane(caja);
        getRootPane().setBorder(BorderFactory.createEmptyBorder());
        setBackground(new Color(0, 0, 0, 0));
        for (String clave : new String[] {"ESCAPE", "E"}) {
            getRootPane().registerKeyboardAction(evento -> dispose(), KeyStroke.getKeyStroke(clave),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        refrescar();
        colocar(dueno);
    }

    /** Los textos actuales del presentador en pantalla. */
    private void refrescar() {
        name.setText(presentador.vista().name() + " · " + presentador.vista().rol());
        texto.setText(presentador.vista().texto());
        encargo.setText(presentador.vista().encargo());
        mensaje.setText(presentador.mensaje().isEmpty() ? " " : presentador.mensaje());
        entregar.setVisible(presentador.puedeEntregar());
    }

    private void colocar(Window dueno) {
        if (dueno == null || dueno.getWidth() <= 0) {
            setSize(ANCHO_MAXIMO, ALTO);
            setLocationRelativeTo(null);
            return;
        }
        Rectangle area = dueno.getBounds();
        int width = Math.min(ANCHO_MAXIMO, area.width - 2 * MARGEN);
        setSize(new Dimension(width, ALTO));
        setLocation(area.x + (area.width - width) / 2, area.y + area.height - ALTO - MARGEN * 3);
    }

    /** Un sprite ampliado sin desenfoque a un cuadrado del lado dado; vacío (pero de ese tamaño) sin el sprite. */
    static JLabel retrato(String sprite, int lado) {
        JLabel etiqueta = new JLabel();
        etiqueta.setPreferredSize(new Dimension(lado, lado));
        GestorRecurso.compartido().sprite(sprite).ifPresent(img -> {
            BufferedImage grande = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = grande.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(img, 0, 0, lado, lado, null);
            g.dispose();
            etiqueta.setIcon(new ImageIcon(grande));
        });
        return etiqueta;
    }
}
