package edu.ucc.farmsim.ui.theme;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Eases a component in and out of its hover look: a value goes from 0 to 1 in about a tenth of a second when
 * the cursor enters and back when it leaves. Its timer only runs while the value moves.
 *
 * <p>Stories: HU-48.
 */
public final class HoverAnimator {

    private static final int FRAME_MS = 16;
    private static final float STEP = 0.18f;

    private final JComponent target;
    private final Timer timer;
    private float value;
    private float goal;

    /**
     * Starts following the cursor over a component.
     *
     * @param target the component to repaint while the value moves
     */
    public HoverAnimator(JComponent target) {
        this.target = target;
        this.timer = new Timer(FRAME_MS, event -> tick());
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                aim(1f);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                aim(0f);
            }
        });
    }

    /** How lit the component is now, from 0 to 1. */
    public float value() {
        return value;
    }

    /** How many pixels the component rises now. */
    public int lift(int most) {
        return Math.round(value * most);
    }

    private void aim(float newGoal) {
        goal = newGoal;
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void tick() {
        value = goal > value ? Math.min(goal, value + STEP) : Math.max(goal, value - STEP);
        target.repaint();
        if (value == goal) {
            timer.stop();
        }
    }
}
