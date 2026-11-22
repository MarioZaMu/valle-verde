package edu.ucc.farmsim.api;

import edu.ucc.farmsim.application.result.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Translates each rejection code of RN-35 into the HTTP status of the response (DT-18, docs/API.md). The
 * {@code switch} has no default branch, so adding an {@link ErrorCode} without deciding its status does not
 * compile.
 *
 * <p>Stories: none.
 */
public final class ErrorStatusMapper {

    private ErrorStatusMapper() {
        // static helper
    }

    /** The HTTP status that answers a rejection with the given code. */
    public static HttpStatus statusOf(ErrorCode code) {
        return switch (code) {
            case SIN_PARTIDA, SIN_CULTIVO, ARCHIVO_INEXISTENTE -> HttpStatus.NOT_FOUND;
            case CANTIDAD_INVALIDA -> HttpStatus.BAD_REQUEST;
            case FUERA_DE_ESTACION, SIN_ENERGIA, SIN_FONDOS, INVENTARIO_LLENO, SIN_SEMILLAS,
                 PARCELA_NO_CULTIVABLE, PARCELA_OCUPADA, YA_REGADO, NO_MADURO, CULTIVO_MARCHITO,
                 EDIFICIO_BLOQUEADO, UBICACION_INVALIDA, LIMITE_EDIFICIOS, SIN_ESPACIO_ANIMALES,
                 SIN_HENO, SIN_MEDICINA, ANIMAL_ENFERMO -> HttpStatus.CONFLICT;
            case ARCHIVO_CORRUPTO, VERSION_NO_SOPORTADA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case ERROR_ES -> HttpStatus.SERVICE_UNAVAILABLE;
            case ERROR_INTERNO -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
