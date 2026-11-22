package edu.ucc.farmsim.api;

/**
 * Body of every rejected response: the RN-35 code and its Spanish message. A rejection carries no events,
 * because a rejected operation never changes the game.
 *
 * <p>Stories: none.
 *
 * @param code    name of the {@code ErrorCode}, or {@code SOLICITUD_INVALIDA} for a malformed request
 * @param message text for the player
 */
public record ApiError(String code, String message) {

    /** Code used for requests that are not well formed (they never reach the game). */
    public static final String INVALID_REQUEST = "SOLICITUD_INVALIDA";
}
