package edu.ucc.farmsim.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucc.farmsim.application.result.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** RNF-15: cada código de rechazo tiene un estado HTTP deliberado (docs/API.md). */
class ErrorStatusMapperTest {

    @Test
    void RNF15_missingThingsAreNotFound() {
        assertEquals(HttpStatus.NOT_FOUND, ErrorStatusMapper.statusOf(ErrorCode.SIN_PARTIDA));
        assertEquals(HttpStatus.NOT_FOUND, ErrorStatusMapper.statusOf(ErrorCode.SIN_CULTIVO));
        assertEquals(HttpStatus.NOT_FOUND, ErrorStatusMapper.statusOf(ErrorCode.ARCHIVO_INEXISTENTE));
    }

    @Test
    void RNF15_brokenBusinessRulesAreConflicts() {
        assertEquals(HttpStatus.CONFLICT, ErrorStatusMapper.statusOf(ErrorCode.FUERA_DE_ESTACION));
        assertEquals(HttpStatus.CONFLICT, ErrorStatusMapper.statusOf(ErrorCode.SIN_ENERGIA));
        assertEquals(HttpStatus.CONFLICT, ErrorStatusMapper.statusOf(ErrorCode.INVENTARIO_LLENO));
        assertEquals(HttpStatus.CONFLICT, ErrorStatusMapper.statusOf(ErrorCode.YA_REGADO));
    }

    @Test
    void RNF15_invalidInputAndDamagedSavesHaveTheirOwnStatus() {
        assertEquals(HttpStatus.BAD_REQUEST, ErrorStatusMapper.statusOf(ErrorCode.CANTIDAD_INVALIDA));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ErrorStatusMapper.statusOf(ErrorCode.ARCHIVO_CORRUPTO));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ErrorStatusMapper.statusOf(ErrorCode.VERSION_NO_SOPORTADA));
    }

    @Test
    void RNF15_failuresOutsideTheRulesAreServerErrors() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ErrorStatusMapper.statusOf(ErrorCode.ERROR_ES));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ErrorStatusMapper.statusOf(ErrorCode.ERROR_INTERNO));
    }

    @Test
    void RNF15_everyCodeMapsToAnErrorStatus() {
        for (ErrorCode code : ErrorCode.values()) {
            assertEquals(true, ErrorStatusMapper.statusOf(code).isError(), code.name());
        }
    }
}
