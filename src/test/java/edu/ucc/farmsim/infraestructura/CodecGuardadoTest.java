package edu.ucc.farmsim.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.apoyo.JuegoRico;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.guardado.ExcepcionAlmacenamientoGuardado;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado.Motivo;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.infraestructura.persistencia.Checksum;
import edu.ucc.farmsim.infraestructura.persistencia.CodecGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** El formato de una partida guardada (RN-32) y el orden de las comprobaciones de RN-33, sin ninguna base de datos. */
class CodecGuardadoTest {

    private final CodecGuardado codec = new CodecGuardado();
    private final MementoJuego memento = JuegoRico.jugar().estado().mementoCrear();

    @Test
    void aGameSurvivesTheRoundTripAndTheDocumentHasTheFieldsOfRn32() {
        DocumentoGuardado documento = codec.codificar(3, memento, Instant.parse("2026-11-22T10:30:00Z"));

        assertEquals("slot3", documento.id());
        assertEquals(3, documento.versionEsquema());
        assertEquals(Checksum.of(documento.estado()), documento.checksum());
        assertEquals(memento, codec.decodificar(documento));
    }

    @Test
    void esTextoEstadoElEstableParaJuegoElMismo() {
        assertEquals(codec.codificar(1, memento, Instant.EPOCH).estado(), codec.codificar(1, memento, Instant.EPOCH).estado());
    }

    private Motivo motivoDe(DocumentoGuardado documento) {
        return assertThrows(ExcepcionCargarGuardado.class, () -> codec.decodificar(documento)).motivo();
    }

    @Test
    void theChecksOfRn33RunInOrderAndStopAtTheFirstFailure() {
        DocumentoGuardado bueno = codec.codificar(1, memento, Instant.EPOCH);

                // 2. lo mal formado gana sobre una versión equivocada y una suma de control equivocada
        assertEquals(Motivo.CORRUPT, motivoDe(new DocumentoGuardado("slot1", 99, Instant.EPOCH, "{oops", "x")));
                // 3. la versión gana sobre una suma de control equivocada
        assertEquals(Motivo.UNSUPPORTED_VERSION, motivoDe(new DocumentoGuardado("slot1", 99, Instant.EPOCH, "{}", "x")));
                // 4. la suma de control
        assertEquals(Motivo.CORRUPT, motivoDe(new DocumentoGuardado("slot1", 1, Instant.EPOCH, bueno.estado() + " ", "x")));
                // un estado que es JSON válido con una suma de control válida pero no es una partida
        String notAGame = "{\"unknown\":1}";
        assertEquals(Motivo.CORRUPT,
                motivoDe(new DocumentoGuardado("slot1", 1, Instant.EPOCH, notAGame, Checksum.of(notAGame))));
        // a document without state
        assertEquals(Motivo.CORRUPT, motivoDe(new DocumentoGuardado("slot1", 1, Instant.EPOCH, null, null)));
        // JSON null
        assertEquals(Motivo.CORRUPT, motivoDe(new DocumentoGuardado("slot1", 1, Instant.EPOCH, "null", Checksum.of("null"))));
                // una fecha que no existe (día 40)
        String diaMalo = bueno.estado().replaceFirst("\"dia\":\\d+", "\"dia\":40");
        assertEquals(Motivo.CORRUPT, motivoDe(new DocumentoGuardado("slot1", 1, Instant.EPOCH, diaMalo, Checksum.of(diaMalo))));
    }

    @Test
    void escribioEsoQueLeeRepositorioElYRanurasElReportes() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        var repositorio = mongo.repositorio();
        assertEquals(false, repositorio.existeRanura(1));

        repositorio.save(1, memento, Instant.parse("2026-11-22T10:30:00Z"));

        assertTrue(repositorio.existeRanura(1));
        assertEquals(memento, repositorio.cargar(1));
        var ranuras = repositorio.ranuras();
        assertEquals(3, ranuras.size());
        assertEquals("OK", ranuras.get(0).status().name());
        assertEquals(memento.dinero(), ranuras.get(0).dinero());
        assertEquals("EMPTY", ranuras.get(1).status().name());
        assertEquals(Motivo.MISSING, assertThrows(ExcepcionCargarGuardado.class, () -> repositorio.cargar(2)).motivo());
    }

    @Test
    void anOutageOnSavingLoadingOrListingBecomesASaveStorageException() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        var repositorio = mongo.repositorio();
        mongo.abajoIr();

        assertThrows(ExcepcionAlmacenamientoGuardado.class, () -> repositorio.save(1, memento, Instant.EPOCH));
        assertThrows(ExcepcionAlmacenamientoGuardado.class, () -> repositorio.cargar(1));
        assertThrows(ExcepcionAlmacenamientoGuardado.class, repositorio::ranuras);
        assertThrows(IllegalArgumentException.class, () -> repositorio.save(4, memento, Instant.EPOCH));
        assertThrows(IllegalArgumentException.class, () -> repositorio.cargar(0));
    }
}
