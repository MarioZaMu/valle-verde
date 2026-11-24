package edu.ucc.farmsim.domain.progress;

/**
 * Los pasos del tutorial de inicio, en el orden en que los recorre el jugador (RN-39), y las dos formas en que termina:
 * todos los pasos hechos, o saltado.
 *
 * <p>Stories: HU-49.
 */
public enum TutorialStep {
    /** Dar un paso con el avatar. */
    WALK,
    /** Sembrar una semilla. */
    SOW,
    /** Regar un cultivo. */
    WATER,
    /** Hablar con una persona del valle. */
    TALK,
    /** Terminar el día. */
    END_DAY,
    /** Se hicieron todos los pasos. */
    DONE,
    /** El jugador lo saltó. */
    SKIPPED;

    /** Cuántos pasos recorre el jugador. */
    public static final int COUNT = 5;

    /** Verdadero cuando el tutorial terminó, hecho o saltado. */
    public boolean isFinished() {
        return this == DONE || this == SKIPPED;
    }

    /** Posición del paso, de 1 a {@link #COUNT}; un tutorial terminado cuenta como el último. */
    public int number() {
        return isFinished() ? COUNT : ordinal() + 1;
    }

    /** El paso que viene después de este; {@link #DONE} después del último. */
    TutorialStep next() {
        return isFinished() ? this : values()[ordinal() + 1];
    }
}
