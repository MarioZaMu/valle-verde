package edu.ucc.farmsim.domain.save;

/**
 * The place where games are stored could not be reached or refused the operation. The infrastructure turns
 * every technical failure into this unchecked exception, so the domain and the application never see MongoDB
 * types; the application translates it into {@code ERROR_ES} (RN-31, RN-35).
 *
 * <p>Stories: HU-33.
 */
public final class SaveStorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param detail short Spanish explanation that is shown to the player
     * @param cause  the technical failure
     */
    public SaveStorageException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
