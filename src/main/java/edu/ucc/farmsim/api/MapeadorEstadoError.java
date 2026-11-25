package edu.ucc.farmsim.api;

import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import org.springframework.http.HttpStatus;

/**
 * Traduce cada código de rechazo de RN-35 al estado HTTP de la respuesta (DT-18, docs/API.md). El {@code switch} no
 * tiene rama por defecto, así que agregar un {@link CodigoError} sin decidir su estado no compila.
 *
 * <p>Stories: none.
 */
public final class MapeadorEstadoError {

    private MapeadorEstadoError() {
                // clase de utilidad estática
    }

    /** El estado HTTP con el que se responde a un rechazo con el código dado. */
    public static HttpStatus situacionDe(CodigoError codigo) {
        return switch (codigo) {
            case SIN_PARTIDA, SIN_CULTIVO, ARCHIVO_INEXISTENTE -> HttpStatus.NOT_FOUND;
            case CANTIDAD_INVALIDA -> HttpStatus.BAD_REQUEST;
            case FUERA_DE_ESTACION, SIN_ENERGIA, SIN_FONDOS, INVENTARIO_LLENO, SIN_SEMILLAS,
                 PARCELA_NO_CULTIVABLE, PARCELA_OCUPADA, YA_REGADO, NO_MADURO, CULTIVO_MARCHITO,
                 EDIFICIO_BLOQUEADO, UBICACION_INVALIDA, LIMITE_EDIFICIOS, SIN_ESPACIO_ANIMALES,
                 SIN_HENO, SIN_MEDICINA, ANIMAL_ENFERMO, MOVIMIENTO_BLOQUEADO,
                 NPC_LEJOS, ENCARGO_INCOMPLETO -> HttpStatus.CONFLICT;
            case ARCHIVO_CORRUPTO, VERSION_NO_SOPORTADA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case ERROR_ES -> HttpStatus.SERVICE_UNAVAILABLE;
            case ERROR_INTERNO -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
