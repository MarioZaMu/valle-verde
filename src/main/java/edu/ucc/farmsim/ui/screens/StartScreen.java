package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.GameInfo;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.components.WoodButton;
import edu.ucc.farmsim.ui.render.TitleScene;
import edu.ucc.farmsim.ui.theme.Texts;
import edu.ucc.farmsim.ui.theme.WoodStyle;
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
 * The start screen: a living farm scene ({@link TitleScene}) with the game name on top, gently floating, and
 * the wooden buttons "New game", "Load" and "Exit". Every button just calls the {@link StartScreenPresenter}.
 * The scene is only animated while the screen is shown.
 *
 * <p>Stories: HU-33, HU-48.
 */
public final class StartScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final float TITLE_SIZE = 76f;
    private static final float SUBTITLE_SIZE = 18f;
    private static final float BUTTON_SIZE = 22f;
    private static final int GAP = 10;
    private static final int FRAME_MS = 33;
    private static final Dimension BUTTON_SIZE_PX = new Dimension(320, 58);
    private static final Color TITLE_TOP = new Color(0xFFE877);
    private static final Color TITLE_BOTTOM = new Color(0xF29A2E);
    private static final Color TITLE_EDGE = new Color(0x4A2A12);
    private static final Color TITLE_SHADOW = new Color(0, 0, 0, 90);

    private final transient TitleScene scene = new TitleScene(AssetManager.shared());
    private final Timer frames = new Timer(FRAME_MS, event -> repaint());
    private long shownAt = System.currentTimeMillis();

    /**
     * Builds the screen.
     *
     * @param presenter handles the button presses
     */
    public StartScreen(StartScreenPresenter presenter) {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(GAP, GAP, GAP, GAP);
        c.fill = GridBagConstraints.HORIZONTAL;
        add(new Banner(), c);
        c.fill = GridBagConstraints.NONE;
        add(button(Texts.START_NEW_GAME, presenter::onNewGame), c);
        add(button(Texts.START_LOAD, presenter::onLoad), c);
        add(button(Texts.START_EXIT, presenter::onExit), c);
        c.weighty = 1;
        add(new JPanel() {
            private static final long serialVersionUID = 1L;

            {
                setOpaque(false);
            }
        }, c);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    shownAt = System.currentTimeMillis();
                    frames.start();
                } else {
                    frames.stop();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        scene.paint(g2, getWidth(), getHeight(), System.currentTimeMillis() - shownAt);
        g2.dispose();
    }

    private long elapsed() {
        return System.currentTimeMillis() - shownAt;
    }

    private static JButton button(String text, Runnable action) {
        JButton button = new WoodButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD, BUTTON_SIZE));
        button.setPreferredSize(BUTTON_SIZE_PX);
        button.addActionListener(event -> action.run());
        return button;
    }

    /** The game name in large golden letters with a dark edge, floating up and down, and a line under it. */
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
            long t = elapsed();
            double bob = Math.sin(t / 650.0) * 5;
            double appear = Math.min(1.0, t / 900.0);
            Font font = getFont().deriveFont(Font.BOLD, TITLE_SIZE);
            GlyphVector glyphs = font.createGlyphVector(g2.getFontRenderContext(), GameInfo.NAME);
            double textWidth = glyphs.getVisualBounds().getWidth();
            double x = (getWidth() - textWidth) / 2;
            double y = 92 + bob - (1 - appear) * 40;
            Shape outline = glyphs.getOutline();
            Shape placed = AffineTransform.getTranslateInstance(x, y).createTransformedShape(outline);
            Shape shadow = AffineTransform.getTranslateInstance(x + 5, y + 6).createTransformedShape(outline);
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) appear));
            g2.setColor(TITLE_SHADOW);
            g2.fill(shadow);
            g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(TITLE_EDGE);
            g2.draw(placed);
            g2.setPaint(new GradientPaint(0, (float) (y - 60), TITLE_TOP, 0, (float) y, TITLE_BOTTOM));
            g2.fill(placed);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 255, 255, 120));
            g2.draw(AffineTransform.getTranslateInstance(x - 1, y - 2).createTransformedShape(outline));

            Font small = getFont().deriveFont(Font.BOLD, SUBTITLE_SIZE);
            FontMetrics metrics = g2.getFontMetrics(small);
            int sw = metrics.stringWidth(Texts.START_SUBTITLE);
            int bx = (getWidth() - sw) / 2 - 18;
            int by = (int) (y + 22);
            WoodStyle.paintFrame(g2, bx, by, sw + 36, metrics.getHeight() + 18);
            g2.setFont(small);
            g2.setColor(WoodStyle.INK);
            g2.drawString(Texts.START_SUBTITLE, bx + 18, by + 9 + metrics.getAscent());
            g2.dispose();
        }
    }
}
