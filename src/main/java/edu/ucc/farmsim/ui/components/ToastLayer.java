package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.Palette;
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
 * Draws the notices of a {@link ToastQueue} in the bottom-right corner, over whatever is below, and lets every
 * mouse event through. A Swing {@link Timer} repaints it only while there is something to show.
 *
 * <p>Stories: HU-39.
 */
public final class ToastLayer extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final int FRAME_MS = 33;
    private static final int MARGIN = 16;
    private static final int PAD = 10;
    private static final int GAP = 8;
    private static final int ARC = 14;

    private final transient ToastQueue queue;
    private final transient Timer timer;
    private boolean wasShowing;

    /**
     * Creates the layer.
     *
     * @param queue the notices to draw
     */
    public ToastLayer(ToastQueue queue) {
        this.queue = queue;
        setOpaque(false);
        timer = new Timer(FRAME_MS, event -> {
            boolean showing = !queue.isIdle();
            if (showing || wasShowing) {
                repaint();
            }
            wasShowing = showing;
        });
        timer.setRepeats(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    /** Never catches the mouse: the map below keeps receiving the clicks. */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        List<ToastView> toasts = queue.visible();
        if (toasts.isEmpty()) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(getFont() == null ? new Font(Font.SANS_SERIF, Font.BOLD, 14) : getFont().deriveFont(Font.BOLD, 14f));
            FontMetrics metrics = g.getFontMetrics();
            int height = metrics.getHeight() + 2 * PAD;
            int y = getHeight() - MARGIN - height;
            for (int i = toasts.size() - 1; i >= 0; i--) {
                ToastView toast = toasts.get(i);
                String text = toast.kind().symbol() + "  " + toast.text();
                int width = metrics.stringWidth(text) + 2 * PAD;
                int x = getWidth() - MARGIN - width;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, toast.opacity())));
                g.setColor(background(toast.kind()));
                g.fillRoundRect(x, y, width, height, ARC, ARC);
                g.setColor(Palette.TOAST_TEXT);
                g.drawString(text, x + PAD, y + PAD + metrics.getAscent());
                y -= height + GAP;
            }
        } finally {
            g.dispose();
        }
    }

    private static Color background(ToastKind kind) {
        return switch (kind) {
            case ERROR -> Palette.TOAST_ERROR;
            case SUCCESS -> Palette.TOAST_SUCCESS;
            case ACHIEVEMENT -> Palette.TOAST_ACHIEVEMENT;
            case INFO -> Palette.TOAST_INFO;
        };
    }
}
