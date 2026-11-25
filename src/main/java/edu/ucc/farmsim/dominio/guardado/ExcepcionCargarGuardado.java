package edu.ucc.farmsim.dominio.guardado;

/**
 * Una ranura no se pudo convertir en una partida: no contiene nada, está dañada o viene de otra versión (RN-33). La
 * aplicación asigna el motivo a su código de RN-35.
 *
 * <p>Stories: HU-30, HU-31.
 */
public final class ExcepcionCargarGuardado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Por qué no se pudo cargar la ranura. */
    public enum Motivo {
        /** La ranura no contiene ningún documento. */
        MISSING,
        /** El texto no es válido, la suma de control no coincide o los datos no tienen sentido. */
        CORRUPT,
        /** El documento lo escribió una versión del juego que esta no conoce. */
        UNSUPPORTED_VERSION
    }

    private final Motivo motivo;

    /**
     * Crea la excepción.
     *
     * @param motivo por qué falló la carga
     * @param detalle detalle técnico para el registro
     */
    public ExcepcionCargarGuardado(Motivo motivo, String detalle) {
        super(detalle);
        this.motivo = motivo;
    }

    /** Por qué falló la carga. */
    public Motivo motivo() {
        return motivo;
    }
}
