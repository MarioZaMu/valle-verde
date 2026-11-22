package edu.ucc.farmsim.domain.save;

/**
 * A slot could not be turned into a game: it holds nothing, it is damaged or it comes from another version
 * (RN-33). The application maps the reason to its RN-35 code.
 *
 * <p>Stories: HU-30, HU-31.
 */
public final class SaveLoadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Why the slot could not be loaded. */
    public enum Reason {
        /** The slot holds no document. */
        MISSING,
        /** The text is not valid, the checksum does not match or the data does not make sense. */
        CORRUPT,
        /** The document was written by a version of the game this one does not know. */
        UNSUPPORTED_VERSION
    }

    private final Reason reason;

    /**
     * Creates the exception.
     *
     * @param reason why the load failed
     * @param detail technical detail for the log
     */
    public SaveLoadException(Reason reason, String detail) {
        super(detail);
        this.reason = reason;
    }

    /** Why the load failed. */
    public Reason reason() {
        return reason;
    }
}
