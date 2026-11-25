package edu.ucc.farmsim.api;

import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Mensajes;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Responde a los fallos que ocurren antes de que una orden llegue al juego (petición mal formada, ruta o verbo
 * equivocado) o fuera de él (un error), siempre con el mismo cuerpo {@link ApiError}. Nada filtra una traza de error al
 * cliente (RNF-07).
 *
 * <p>Stories: none.
 */
@RestControllerAdvice
public class ManejadorExcepcionApi {

    private static final Logger BITACORA = Logger.getLogger(ManejadorExcepcionApi.class.getName());

    /** Un cuerpo que no es JSON, o un valor de ruta del tipo equivocado. */
    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ApiError> peticionInvalida(Exception e) {
        BITACORA.log(Level.FINE, "Invalid request", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ApiError.PETICION_INVALIDA, "La solicitud no es válida."));
    }

    /** Los errores propios de Spring (restricciones, verbo equivocado, ruta desconocida) y cualquier imprevisto. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> otro(Exception e) {
        if (e instanceof ErrorResponse respuesta) {
            HttpStatus status = HttpStatus.valueOf(respuesta.getStatusCode().value());
            String mensaje = status == HttpStatus.NOT_FOUND ? "La ruta no existe." : "La solicitud no es válida.";
            return ResponseEntity.status(status).body(new ApiError(ApiError.PETICION_INVALIDA, mensaje));
        }
        BITACORA.log(Level.SEVERE, "Unexpected error in the API", e);
        return ResponseEntity.status(MapeadorEstadoError.situacionDe(CodigoError.ERROR_INTERNO))
                .body(new ApiError(CodigoError.ERROR_INTERNO.name(), Mensajes.of(CodigoError.ERROR_INTERNO)));
    }
}
