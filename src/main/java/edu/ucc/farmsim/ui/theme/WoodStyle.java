package edu.ucc.farmsim.ui.theme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;

/**
 * The look of the panels and buttons of the game: a frame of warm wood with a dark outline around a sheet of
 * parchment, and wooden buttons that light up and rise under the cursor. Everything is painted with Java2D,
 * so it needs no image and looks the same with or without the sprite pack.
 *
 * <p>Stories: HU-43, HU-48.
 */
public final class WoodStyle {

    /** Dark outline of every frame. */
    public static final Color OUTLINE = new Color(0x2E1A0C);
    /** Wood in shadow. */
    public static final Color WOOD_DARK = new Color(0x7A4A22);
    /** Wood. */
    public static final Color WOOD = new Color(0xA8692F);
    /** Wood in the light. */
    public static final Color WOOD_LIGHT = new Color(0xD08E48);
    /** Parchment inside a frame. */
    public static final Color PARCHMENT = new Color(0xF6E3B4);
    /** Parchment in shadow, along the inner edge of a frame. */
    public static final Color PARCHMENT_SHADOW = new Color(0xE2C487);
    /** Text on parchment. */
    public static final Color INK = new Color(0x3B2412);
    /** Text on wood. */
    public static final Color CREAM = new Color(0xFFF4D6);
    /** Frame of the chosen slot. */
    public static final Color SELECTED = new Color(0xE0502A);
    /** Glow around the chosen slot. */
    public static final Color GLOW = new Color(0xFFD23F);

    /** Thickness of the wooden frame, in pixels. */
    public static final int FRAME = 5;

    private static final int ARC = 10;
    private static final Color HOVER_LIGHT = new Color(255, 255, 255, 60);

    private WoodStyle() {
        // static helpers only
    }

    /** Paints a wooden frame around parchment covering the given rectangle. */
    public static void paintFrame(Graphics2D g, int x, int y, int w, int h) {
        if (w <= 2 * FRAME || h <= 2 * FRAME) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(x + 2, y + 3, w - 2, h - 2, ARC, ARC);
        g.setColor(OUTLINE);
        g.fillRoundRect(x, y, w - 2, h - 3, ARC, ARC);
        g.setPaint(new GradientPaint(x, y, WOOD_LIGHT, x, y + h, WOOD_DARK));
        g.fillRoundRect(x + 2, y + 2, w - 6, h - 7, ARC - 2, ARC - 2);
        g.setColor(WOOD_LIGHT.brighter());
        g.drawLine(x + ARC / 2 + 2, y + 3, x + w - ARC / 2 - 6, y + 3);
        int ix = x + FRAME;
        int iy = y + FRAME;
        int iw = w - 2 * FRAME - 2;
        int ih = h - 2 * FRAME - 3;
        g.setColor(WOOD_DARK.darker());
        g.fillRect(ix - 1, iy - 1, iw + 2, ih + 2);
        g.setColor(PARCHMENT);
        g.fillRect(ix, iy, iw, ih);
        g.setColor(PARCHMENT_SHADOW);
        g.fillRect(ix, iy, iw, 2);
        g.fillRect(ix, iy, 2, ih);
        g.setColor(WOOD_LIGHT.brighter());
        for (int[] nail : new int[][] {{x + 3, y + 3}, {x + w - 7, y + 3}, {x + 3, y + h - 8}, {x + w - 7, y + h - 8}}) {
            g.fillRect(nail[0], nail[1], 2, 2);
        }
    }

    /**
     * Paints the body of a button. A plain button is a plank of wood; a slot is a parchment square in a
     * frame, as in a tool bar, and gets a bright frame when it is the chosen one.
     *
     * @param g        where to paint
     * @param b        the button
     * @param hover    how lit it is, from 0 (cursor away) to 1 (cursor on it)
     * @param slot     true for a tool bar slot
     */
    public static void paintButton(Graphics2D g, AbstractButton b, float hover, boolean slot) {
        int w = b.getWidth();
        int h = b.getHeight();
        boolean pressed = b.getModel().isArmed() && b.getModel().isPressed();
        boolean chosen = b.isSelected();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (slot) {
            paintFrame(g, 0, 0, w, h);
            if (chosen) {
                g.setColor(GLOW);
                g.fillRoundRect(FRAME - 2, FRAME - 2, w - 2 * FRAME, h - 2 * FRAME - 1, 6, 6);
                g.setColor(PARCHMENT);
                g.fillRect(FRAME + 2, FRAME + 2, w - 2 * FRAME - 6, h - 2 * FRAME - 7);
                g.setColor(SELECTED);
                g.drawRoundRect(FRAME - 2, FRAME - 2, w - 2 * FRAME, h - 2 * FRAME - 1, 6, 6);
            }
        } else {
            int drop = pressed ? 1 : 3;
            int top = pressed ? 2 : 0;
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(1, 3, w - 2, h - 3, ARC, ARC);
            g.setColor(OUTLINE);
            g.fillRoundRect(0, top, w - 1, h - 1 - top, ARC, ARC);
            g.setColor(WOOD_DARK);
            g.fillRoundRect(2, top + 2, w - 5, h - 5 - top, ARC - 2, ARC - 2);
            Color light = b.isEnabled() ? WOOD_LIGHT : WOOD;
            g.setPaint(new GradientPaint(0, top, light, 0, h - drop, WOOD));
            g.fillRoundRect(2, top + 2, w - 5, h - 5 - top - drop, ARC - 2, ARC - 2);
            g.setColor(new Color(255, 230, 180, 120));
            g.drawLine(6, top + 3, w - 8, top + 3);
            g.setColor(new Color(90, 50, 20, 70));
            for (int y = top + 9; y < h - drop - 4; y += 7) {
                g.drawLine(6, y, w / 3, y);
                g.drawLine(w / 2, y + 3, w - 9, y + 3);
            }
        }
        if (hover > 0f && b.isEnabled()) {
            g.setColor(new Color(255, 255, 255, Math.round(HOVER_LIGHT.getAlpha() * hover)));
            g.fillRoundRect(2, 2, w - 5, h - 6, ARC - 2, ARC - 2);
        }
    }

    /** Makes a Swing button ready to be painted by {@link #paintButton}. */
    public static void prepare(AbstractButton b, boolean slot) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFocusable(false);
        b.setForeground(slot ? INK : CREAM);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(slot ? BorderFactory.createEmptyBorder(FRAME + 4, FRAME + 8, FRAME + 6, FRAME + 8)
                : BorderFactory.createEmptyBorder(8, 18, 11, 18));
    }
}
