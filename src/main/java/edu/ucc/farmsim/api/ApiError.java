package edu.ucc.farmsim.api;

/**
 * Cuerpo de toda respuesta rechazada: el código de RN-35 y su mensaje en español. Un rechazo no lleva eventos, porque
 * una operación rechazada nunca cambia el juego.
 *
 * <p>Stories: none.
 *
 * @param code    nombre del {@code ErrorCode}, o {@code SOLICITUD_INVALIDA} si la petición está mal formada
 * @param message texto para el jugador
 */
public record ApiError(String code, String message) {

    /** Código para las peticiones mal formadas (nunca llegan al juego). */
    public static final String INVALID_REQUEST = "SOLICITUD_INVALIDA";
}
