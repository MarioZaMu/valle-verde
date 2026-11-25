package edu.ucc.farmsim.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** RNF-15: cada código de rechazo tiene un estado HTTP deliberado (docs/API.md). */
class MapeadorEstadoErrorTest {

    @Test
    void RNF15_faltante_cosas_son_no_encontrado() {
        assertEquals(HttpStatus.NOT_FOUND, MapeadorEstadoError.situacionDe(CodigoError.SIN_PARTIDA));
        assertEquals(HttpStatus.NOT_FOUND, MapeadorEstadoError.situacionDe(CodigoError.SIN_CULTIVO));
        assertEquals(HttpStatus.NOT_FOUND, MapeadorEstadoError.situacionDe(CodigoError.ARCHIVO_INEXISTENTE));
    }

    @Test
    void RNF15_roto_negocio_reglas_son_conflictos() {
        assertEquals(HttpStatus.CONFLICT, MapeadorEstadoError.situacionDe(CodigoError.FUERA_DE_ESTACION));
        assertEquals(HttpStatus.CONFLICT, MapeadorEstadoError.situacionDe(CodigoError.SIN_ENERGIA));
        assertEquals(HttpStatus.CONFLICT, MapeadorEstadoError.situacionDe(CodigoError.INVENTARIO_LLENO));
        assertEquals(HttpStatus.CONFLICT, MapeadorEstadoError.situacionDe(CodigoError.YA_REGADO));
    }

    @Test
    void RNF15_invalido_entrada_y_danado_guardados_tener_su_propio_situacion() {
        assertEquals(HttpStatus.BAD_REQUEST, MapeadorEstadoError.situacionDe(CodigoError.CANTIDAD_INVALIDA));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, MapeadorEstadoError.situacionDe(CodigoError.ARCHIVO_CORRUPTO));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, MapeadorEstadoError.situacionDe(CodigoError.VERSION_NO_SOPORTADA));
    }

    @Test
    void RNF15_fallos_afuera_los_reglas_son_servidor_errores() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, MapeadorEstadoError.situacionDe(CodigoError.ERROR_ES));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, MapeadorEstadoError.situacionDe(CodigoError.ERROR_INTERNO));
    }

    @Test
    void RNF15_cada_codigo_mapas_a_un_error_situacion() {
        for (CodigoError codigo : CodigoError.values()) {
            assertEquals(true, MapeadorEstadoError.situacionDe(codigo).isError(), codigo.name());
        }
    }
}
