package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.JuegoRico;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.infraestructura.persistencia.Checksum;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import org.junit.jupiter.api.Test;

/**
 * HU-29: guardar una partida en una de tres ranuras como un documento con una versión y una suma de control (RN-31,
 * RN-32).
 */
class HU29JuegoGuardadoTest {

    private final MongoEnMemoria mongo = new MongoEnMemoria();

    private AplicacionTest aplicacionCon(MongoEnMemoria basedatos) {
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), basedatos.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        return aplicacion;
    }

    @Test
    void HU29_ca1_guardando_crea_la_ranura_documento_con_version_fecha_estado_y_checksum() {
                // Dada una partida en curso
        AplicacionTest aplicacion = aplicacionCon(mongo);

                // Cuando el jugador guarda en la ranura 2
        Resultado<VistaRanuraGuardado> resultado = aplicacion.controlador.save(2);

                // Entonces existe el documento slot2 con schemaVersion 3 (1 antes del avatar, 2 antes del valle), savedAt,
        // state y el SHA-256 del estado
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        DocumentoGuardado documento = mongo.documento(2);
        assertNotNull(documento);
        assertEquals("slot2", documento.id());
        assertEquals(3, documento.versionEsquema());
        assertEquals(AplicacionTest.RELOJ_FIJO.instant(), documento.archivadoEn());
        assertTrue(documento.estado().startsWith("{"));
        assertEquals(Checksum.of(documento.estado()), documento.checksum());
        assertEquals(64, documento.checksum().length());
        assertEquals("OK", resultado.value().status());
        assertEquals("2026-11-22 10:30", resultado.value().archivadoEn());
        assertEquals(null, mongo.documento(1));
    }

    @Test
    void HU29_ca2_aFailedWriteLeavesThePreviousDocumentAndTheGameInPlay() {
        AplicacionTest aplicacion = aplicacionCon(mongo);
        aplicacion.controlador.save(1);
        DocumentoGuardado antes = mongo.documento(1);
        aplicacion.controlador.diaFin();
        mongo.abajoIr();

        Resultado<VistaRanuraGuardado> resultado = aplicacion.controlador.save(1);

        assertEquals(CodigoError.ERROR_ES, resultado.codigo());
        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", resultado.mensaje());
        assertEquals(antes, mongo.documento(1));
        assertEquals(2, aplicacion.controlador.hud().value().dia());
        mongo.atrasVenir();
        assertTrue(aplicacion.controlador.save(1).isOk());
    }

    @Test
    void HU29_ca3_guardando_en_un_ocupado_ranura_reemplaza_su_documento() {
        AplicacionTest aplicacion = aplicacionCon(mongo);
        aplicacion.controlador.save(3);
        String first = mongo.documento(3).estado();
        aplicacion.controlador.diaFin();

        aplicacion.controlador.save(3);

        assertTrue(!first.equals(mongo.documento(3).estado()));
        assertEquals(Checksum.of(mongo.documento(3).estado()), mongo.documento(3).checksum());
    }

    @Test
    void HU29_withoutAGameThereIsNothingToSave() {
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());

        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.save(1).codigo());
    }

    @Test
    void HU29_aSlotOutsideTheThreeIsAnInternalErrorAndWritesNothing() {
        AplicacionTest aplicacion = aplicacionCon(mongo);

        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.save(4).codigo());
        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.save(0).codigo());
    }

    @Test
    void HU29_aRichGameIsWrittenAsOneValidDocument() {
        AplicacionTest aplicacion = JuegoRico.jugar();
        MongoEnMemoria basedatos = new MongoEnMemoria();
        AplicacionTest guardando = AplicacionTest.create(aplicacion.sesion, SecuenciaDia.estandar(), basedatos.repositorio());

        assertTrue(guardando.controlador.save(1).isOk());

        assertTrue(basedatos.documento(1).estado().contains("\"edificios\""));
        assertTrue(basedatos.documento(1).estado().length() < 200 * 1024);
    }
}
