package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.ui.theme.Texts;
import edu.ucc.farmsim.ui.theme.WoodStyle;
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
 * solo cuando termina. Sigue la partida mediante sus eventos; la lógica está en {@link TutorialPresenter}.
 *
 * <p>Stories: HU-49.
 */
public final class TutorialBox extends WoodBox implements GameListener {

    /**
     * Lo más ancho que llega a ser el cuadro, para que quepa junto a la barra superior en una ventana de 1024 x 768.
     */
    public static final int MAX_WIDTH = 480;
    /** Lo más alto que llega a ser el cuadro. */
    public static final int MAX_HEIGHT = 140;

    private static final long serialVersionUID = 1L;
    private static final int PORTRAIT = 64;
    private static final int TEXT_WIDTH = MAX_WIDTH - PORTRAIT - 40;

    private final transient TutorialPresenter presenter;
    private final JLabel portrait = DialogueDialog.portrait("characters/AVATAR_S_0.png", PORTRAIT);
    private final JLabel step = new JLabel();
    private final JTextArea message = new JTextArea();
    private final WoodButton skip = new WoodButton(Texts.SKIP_TUTORIAL);

    /**
     * Construye el cuadro, oculto hasta que {@link #refresh()} encuentre un tutorial en marcha.
     *
     * @param presenter la lógica del cuadro
     */
    public TutorialBox(TutorialPresenter presenter) {
        super(new BorderLayout(10, 2));
        this.presenter = presenter;
        setName("tutorial-box");
        add(portrait, BorderLayout.WEST);

        step.setForeground(WoodStyle.INK);
        step.setFont(step.getFont().deriveFont(Font.BOLD));
        message.setName("tutorial-text");
        message.setEditable(false);
        message.setFocusable(false);
        message.setOpaque(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setBorder(BorderFactory.createEmptyBorder());
        message.setMargin(new Insets(0, 0, 0, 0));
        message.setForeground(WoodStyle.INK);
        message.setFont(step.getFont().deriveFont(Font.PLAIN));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(step, BorderLayout.WEST);
        JPanel words = new JPanel(new BorderLayout(0, 4));
        words.setOpaque(false);
        words.add(top, BorderLayout.NORTH);
        words.add(message, BorderLayout.CENTER);
        skip.setName("skip-tutorial");
        skip.setFocusable(false);
        skip.addActionListener(event -> {
            presenter.skip();
            update();
        });
        top.add(skip, BorderLayout.EAST);
        add(words, BorderLayout.CENTER);
        setVisible(false);
    }

    /** Le pide el tutorial a la partida y muestra u oculta el cuadro. */
    public void refresh() {
        presenter.reload();
        update();
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof GameEvent.TutorialAdvanced advanced) {
            onSwing(() -> {
                presenter.show(advanced.tutorial());
                update();
            });
        } else if (event instanceof GameEvent.GameStarted) {
            onSwing(this::refresh);
        }
    }

    /** En el hilo de Swing; un cuadro que todavía no está en pantalla (o en una prueba) se cambia de inmediato. */
    private void onSwing(Runnable change) {
        if (SwingUtilities.isEventDispatchThread() || !isDisplayable()) {
            change.run();
        } else {
            SwingUtilities.invokeLater(change);
        }
    }

    private void update() {
        step.setText(presenter.stepText());
        message.setText(presenter.messageText());
        message.setPreferredSize(null);
        message.setSize(TEXT_WIDTH, Short.MAX_VALUE);
        message.setPreferredSize(new Dimension(TEXT_WIDTH, message.getPreferredSize().height));
        boolean visible = presenter.isActive();
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
        return new Dimension(Math.min(MAX_WIDTH, size.width), size.height);
    }

    /** «Paso n de 5» tal como se muestra. */
    public String stepText() {
        return step.getText();
    }

    /** Lo que dice el avatar, tal como se muestra. */
    public String messageText() {
        return message.getText();
    }

    /** El botón que salta el tutorial. */
    public WoodButton skipButton() {
        return skip;
    }

    /** Verdadero si se dibuja el retrato del granjero (solo falta cuando no hay sprites). */
    public boolean hasPortrait() {
        return portrait.getIcon() != null;
    }
}
