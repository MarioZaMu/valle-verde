package edu.ucc.farmsim.api;

/**
 * Cuerpo de toda respuesta rechazada: el código de RN-35 y su mensaje en español. Un rechazo no lleva eventos, porque
 * una operación rechazada nunca cambia el juego.
 *
 * <p>Stories: none.
 *
 * @param codigo    nombre del {@code CodigoError}, o {@code SOLICITUD_INVALIDA} si la petición está mal formada
 * @param mensaje texto para el jugador
 */
public record ApiError(String codigo, String mensaje) {

    /** Código para las peticiones mal formadas (nunca llegan al juego). */
    public static final String PETICION_INVALIDA = "SOLICITUD_INVALIDA";
}
