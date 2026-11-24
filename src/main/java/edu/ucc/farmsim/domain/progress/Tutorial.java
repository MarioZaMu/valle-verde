package edu.ucc.farmsim.domain.progress;

/**
 * Dónde va el jugador en el tutorial de inicio (RN-39). Empieza en {@link TutorialStep#WALK}; una acción lo hace
 * avanzar solo si es la acción del paso actual, y una vez terminado, hecho o saltado, nunca vuelve a empezar. No cambia
 * nada más del juego.
 *
 * <p>Stories: HU-49.
 */
public final class Tutorial {

    private TutorialStep step = TutorialStep.WALK;

    /** El paso actual, o cómo terminó. */
    public TutorialStep step() {
        return step;
    }

    /** Verdadero cuando el tutorial terminó. */
    public boolean isFinished() {
        return step.isFinished();
    }

    /**
     * El jugador hizo una acción.
     *
     * @param action el paso al que pertenece la acción
     * @return {@code true} si era el paso actual y el tutorial avanzó
     */
    public boolean complete(TutorialStep action) {
        if (isFinished() || action != step) {
            return false;
        }
        step = step.next();
        return true;
    }

    /**
     * Termina el tutorial de inmediato.
     *
     * @return {@code true} si todavía estaba en curso
     */
    public boolean skip() {
        if (isFinished()) {
            return false;
        }
        step = TutorialStep.SKIPPED;
        return true;
    }

    /** El paso, para el memento. */
    public TutorialStep snapshot() {
        return step;
    }

    /** Deja el paso como estaba; una partida escrita antes del tutorial ({@code null}) lo carga terminado. */
    public void restoreFrom(TutorialStep saved) {
        step = saved == null ? TutorialStep.DONE : saved;
    }
}
