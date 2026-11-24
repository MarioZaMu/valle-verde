package edu.ucc.farmsim.api;

import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Messages;
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
public class ApiExceptionHandler {

    private static final Logger LOG = Logger.getLogger(ApiExceptionHandler.class.getName());

    /** Un cuerpo que no es JSON, o un valor de ruta del tipo equivocado. */
    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ApiError> invalidRequest(Exception e) {
        LOG.log(Level.FINE, "Invalid request", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ApiError.INVALID_REQUEST, "La solicitud no es válida."));
    }

    /** Los errores propios de Spring (restricciones, verbo equivocado, ruta desconocida) y cualquier imprevisto. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception e) {
        if (e instanceof ErrorResponse response) {
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            String message = status == HttpStatus.NOT_FOUND ? "La ruta no existe." : "La solicitud no es válida.";
            return ResponseEntity.status(status).body(new ApiError(ApiError.INVALID_REQUEST, message));
        }
        LOG.log(Level.SEVERE, "Unexpected error in the API", e);
        return ResponseEntity.status(ErrorStatusMapper.statusOf(ErrorCode.ERROR_INTERNO))
                .body(new ApiError(ErrorCode.ERROR_INTERNO.name(), Messages.of(ErrorCode.ERROR_INTERNO)));
    }
}
