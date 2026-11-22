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
 * Answers the failures that happen before an order reaches the game (a malformed request, a wrong route or
 * verb) or outside it (a bug), always with the same {@link ApiError} body. Nothing leaks a stack trace to
 * the client (RNF-07).
 *
 * <p>Stories: none.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = Logger.getLogger(ApiExceptionHandler.class.getName());

    /** A body that is not JSON, or a path value of the wrong type. */
    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ApiError> invalidRequest(Exception e) {
        LOG.log(Level.FINE, "Invalid request", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ApiError.INVALID_REQUEST, "La solicitud no es válida."));
    }

    /** Spring's own request errors (constraints, wrong verb, unknown route) and anything unexpected. */
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
