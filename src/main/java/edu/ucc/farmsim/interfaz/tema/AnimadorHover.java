package edu.ucc.farmsim.interfaz.tema;

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
public final class AnimadorHover {

    private static final int MS_MARCO = 16;
    private static final float PASO = 0.18f;

    private final JComponent objetivo;
    private final Timer temporizador;
    private float value;
    private float meta;

    /**
     * Empieza a seguir el cursor sobre un componente.
     *
     * @param objetivo el componente que se repinta mientras el valor se mueve
     */
    public AnimadorHover(JComponent objetivo) {
        this.objetivo = objetivo;
        this.temporizador = new Timer(MS_MARCO, evento -> tic());
        objetivo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                apuntar(1f);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                apuntar(0f);
            }
        });
    }

    /** Qué tan iluminado está el componente ahora, de 0 a 1. */
    public float value() {
        return value;
    }

    /** Cuántos píxeles sube ahora el componente. */
    public int levantar(int muchisimo) {
        return Math.round(value * muchisimo);
    }

    private void apuntar(float metaNueva) {
        meta = metaNueva;
        if (!temporizador.isRunning()) {
            temporizador.start();
        }
    }

    private void tic() {
        value = meta > value ? Math.min(meta, value + PASO) : Math.max(meta, value - PASO);
        objetivo.repaint();
        if (value == meta) {
            temporizador.stop();
        }
    }
}
