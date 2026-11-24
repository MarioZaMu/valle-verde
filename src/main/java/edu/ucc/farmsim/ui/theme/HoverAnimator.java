package edu.ucc.farmsim.ui.theme;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Suaviza la entrada y salida de un componente de su aspecto al pasar el cursor: un valor va de 0 a 1 en una décima de
 * segundo cuando el cursor entra y vuelve cuando sale. Su temporizador solo corre mientras el valor se mueve.
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
     * Empieza a seguir el cursor sobre un componente.
     *
     * @param target el componente que se repinta mientras el valor se mueve
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

    /** Qué tan iluminado está el componente ahora, de 0 a 1. */
    public float value() {
        return value;
    }

    /** Cuántos píxeles sube ahora el componente. */
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
