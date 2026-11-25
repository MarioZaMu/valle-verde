package edu.ucc.farmsim.dominio.progreso;

/**
 * Dónde va el jugador en el tutorial de inicio (RN-39). Empieza en {@link PasoTutorial#WALK}; una acción lo hace
 * avanzar solo si es la acción del paso actual, y una vez terminado, hecho o saltado, nunca vuelve a empezar. No cambia
 * nada más del juego.
 *
 * <p>Stories: HU-49.
 */
public final class Tutorial {

    private PasoTutorial paso = PasoTutorial.WALK;

    /** El paso actual, o cómo terminó. */
    public PasoTutorial paso() {
        return paso;
    }

    /** Verdadero cuando el tutorial terminó. */
    public boolean esConcluido() {
        return paso.esConcluido();
    }

    /**
     * El jugador hizo una acción.
     *
     * @param accion el paso al que pertenece la acción
     * @return {@code true} si era el paso actual y el tutorial avanzó
     */
    public boolean complete(PasoTutorial accion) {
        if (esConcluido() || accion != paso) {
            return false;
        }
        paso = paso.next();
        return true;
    }

    /**
     * Termina el tutorial de inmediato.
     *
     * @return {@code true} si todavía estaba en curso
     */
    public boolean saltar() {
        if (esConcluido()) {
            return false;
        }
        paso = PasoTutorial.SKIPPED;
        return true;
    }

    /** El paso, para el memento. */
    public PasoTutorial instantanea() {
        return paso;
    }

    /** Deja el paso como estaba; una partida escrita antes del tutorial ({@code null}) lo carga terminado. */
    public void restaurarDesde(PasoTutorial archivado) {
        paso = archivado == null ? PasoTutorial.DONE : archivado;
    }
}
