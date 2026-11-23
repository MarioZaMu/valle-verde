package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.theme.Texts;
import edu.ucc.farmsim.ui.theme.WoodStyle;
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
 * The dialogue box of a person of the valley, at the bottom of the window as in a role-playing game (HU-46):
 * portrait, name and role, what it says, its errand and the buttons Entregar and Cerrar. It is modal; {@code Esc}
 * or {@code E} closes it. All the logic is in {@link DialoguePresenter}.
 *
 * <p>Stories: HU-46, HU-47, HU-49.
 */
public final class DialogueDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int PORTRAIT = 96;
    private static final int MAX_WIDTH = 780;
    private static final int HEIGHT = 230;
    private static final int MARGIN = 40;
    private static final Color REJECTED = new Color(0x8E1F12);

    private final transient DialoguePresenter presenter;
    private final JLabel name = new JLabel();
    private final JTextArea text = new JTextArea();
    private final JLabel errand = new JLabel();
    private final JLabel message = new JLabel(" ");
    private final WoodButton deliver = new WoodButton(Texts.DELIVER);

    /**
     * Builds the dialog.
     *
     * @param owner     the window it belongs to
     * @param presenter the logic of the conversation
     */
    public DialogueDialog(Window owner, DialoguePresenter presenter) {
        super(owner, presenter.view().name(), ModalityType.APPLICATION_MODAL);
        this.presenter = presenter;
        setUndecorated(true);
        WoodBox box = new WoodBox(new BorderLayout(12, 6));
        box.add(portrait("characters/" + presenter.view().npcId() + ".png", PORTRAIT), BorderLayout.WEST);

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        name.setFont(name.getFont().deriveFont(Font.BOLD, name.getFont().getSize2D() + 4));
        name.setForeground(WoodStyle.INK);
        text.setName("dialogue-text");
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBorder(BorderFactory.createEmptyBorder());
        text.setMargin(new java.awt.Insets(0, 0, 0, 0));
        text.setForeground(WoodStyle.INK);
        text.setFont(name.getFont().deriveFont(Font.PLAIN, name.getFont().getSize2D() - 3));
        errand.setFont(text.getFont().deriveFont(Font.ITALIC));
        errand.setForeground(WoodStyle.INK);
        message.setForeground(REJECTED);
        message.setFont(message.getFont().deriveFont(Font.BOLD));
        for (JComponent part : new JComponent[] {name, text, errand, message}) {
            part.setAlignmentX(LEFT_ALIGNMENT);
            words.add(part);
            words.add(Box.createVerticalStrut(4));
        }
        box.add(words, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        deliver.setName("deliver-button");
        deliver.addActionListener(event -> {
            presenter.deliver();
            refresh();
        });
        WoodButton close = new WoodButton(Texts.CLOSE);
        close.setName("close-button");
        close.addActionListener(event -> dispose());
        buttons.add(deliver);
        buttons.add(close);
        box.add(buttons, BorderLayout.SOUTH);
        setContentPane(box);
        getRootPane().setBorder(BorderFactory.createEmptyBorder());
        setBackground(new Color(0, 0, 0, 0));
        for (String key : new String[] {"ESCAPE", "E"}) {
            getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke(key),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        refresh();
        place(owner);
    }

    /** The current texts of the presenter on screen. */
    private void refresh() {
        name.setText(presenter.view().name() + " · " + presenter.view().role());
        text.setText(presenter.view().text());
        errand.setText(presenter.view().errand());
        message.setText(presenter.message().isEmpty() ? " " : presenter.message());
        deliver.setVisible(presenter.canDeliver());
    }

    private void place(Window owner) {
        if (owner == null || owner.getWidth() <= 0) {
            setSize(MAX_WIDTH, HEIGHT);
            setLocationRelativeTo(null);
            return;
        }
        Rectangle area = owner.getBounds();
        int width = Math.min(MAX_WIDTH, area.width - 2 * MARGIN);
        setSize(new Dimension(width, HEIGHT));
        setLocation(area.x + (area.width - width) / 2, area.y + area.height - HEIGHT - MARGIN * 3);
    }

    /** A sprite enlarged without blur to a square of the given side; empty (but of that size) without the sprite. */
    static JLabel portrait(String sprite, int side) {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(side, side));
        AssetManager.shared().sprite(sprite).ifPresent(img -> {
            BufferedImage big = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = big.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(img, 0, 0, side, side, null);
            g.dispose();
            label.setIcon(new ImageIcon(big));
        });
        return label;
    }
}
