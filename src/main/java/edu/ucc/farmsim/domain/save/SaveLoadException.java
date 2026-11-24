package edu.ucc.farmsim.domain.save;

/**
 * Una ranura no se pudo convertir en una partida: no contiene nada, está dañada o viene de otra versión (RN-33). La
 * aplicación asigna el motivo a su código de RN-35.
 *
 * <p>Stories: HU-30, HU-31.
 */
public final class SaveLoadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Por qué no se pudo cargar la ranura. */
    public enum Reason {
        /** La ranura no contiene ningún documento. */
        MISSING,
        /** El texto no es válido, la suma de control no coincide o los datos no tienen sentido. */
        CORRUPT,
        /** El documento lo escribió una versión del juego que esta no conoce. */
        UNSUPPORTED_VERSION
    }

    private final Reason reason;

    /**
     * Crea la excepción.
     *
     * @param reason por qué falló la carga
     * @param detail detalle técnico para el registro
     */
    public SaveLoadException(Reason reason, String detail) {
        super(detail);
        this.reason = reason;
    }

    /** Por qué falló la carga. */
    public Reason reason() {
        return reason;
    }
}
