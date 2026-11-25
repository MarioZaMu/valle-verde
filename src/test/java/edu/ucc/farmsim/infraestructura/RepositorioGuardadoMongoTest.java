package edu.ucc.farmsim.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.ucc.farmsim.dominio.guardado.ExcepcionAlmacenamientoGuardado;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado.Motivo;
import edu.ucc.farmsim.dominio.guardado.InfoRanura;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.RepositorioGuardadoMongo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.MappingInstantiationException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/** RN-31: el repositorio le pregunta a MongoDB qué ranuras existen y oculta todo fallo técnico (DT-21). */
class RepositorioGuardadoMongoTest {

    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final RepositorioGuardadoMongo repositorio = new RepositorioGuardadoMongo(mongo);

    @Test
    void aSlotWhoseDocumentExistsIsReportedAsExisting() {
        when(mongo.exists(any(Query.class), eq(DocumentoGuardado.class))).thenReturn(true);

        assertTrue(repositorio.existeRanura(2));
    }

    @Test
    void aSlotWithoutADocumentIsReportedAsEmpty() {
        when(mongo.exists(any(Query.class), eq(DocumentoGuardado.class))).thenReturn(false);

        assertFalse(repositorio.existeRanura(1));
    }

    @Test
    void theQueryTargetsTheIdOfTheSlot() {
        when(mongo.exists(any(Query.class), eq(DocumentoGuardado.class))).thenAnswer(llamar -> {
            Query query = llamar.getArgument(0);
            return query.getQueryObject().toJson().contains("slot3");
        });

        assertTrue(repositorio.existeRanura(3));
        assertFalse(repositorio.existeRanura(1));
    }

    @Test
    void whenMongoDbDoesNotAnswerTheFailureBecomesASaveStorageException() {
        DataAccessResourceFailureException caida = new DataAccessResourceFailureException("timed out");
        when(mongo.exists(any(Query.class), eq(DocumentoGuardado.class))).thenThrow(caida);

        ExcepcionAlmacenamientoGuardado lanzado = assertThrows(ExcepcionAlmacenamientoGuardado.class, () -> repositorio.existeRanura(1));

        assertEquals("MongoDB no respondió", lanzado.getMessage());
        assertSame(caida, lanzado.getCause());
    }

    @Test
    void HU32_ca2_a_document_that_cannot_be_mapped_is_listed_as_damaged_and_the_rest_still_works() {
        // lo que lanza Spring Data de verdad cuando el documento trae campos con otros nombres (el int queda en null)
        when(mongo.findById(eq("slot1"), eq(DocumentoGuardado.class))).thenThrow(
                new MappingInstantiationException(java.util.Arrays.asList("slot1", null, null), new IllegalArgumentException("int no admite null")));
        when(mongo.findById(eq("slot2"), eq(DocumentoGuardado.class))).thenReturn(null);
        when(mongo.findById(eq("slot3"), eq(DocumentoGuardado.class))).thenReturn(null);

        List<InfoRanura> ranuras = repositorio.ranuras();

        assertEquals(3, ranuras.size());
        assertEquals(InfoRanura.Situacion.DAMAGED, ranuras.get(0).status());
        assertEquals(InfoRanura.Situacion.EMPTY, ranuras.get(1).status());
        assertEquals(InfoRanura.Situacion.EMPTY, ranuras.get(2).status());
    }

    @Test
    void HU31_ca4_loading_a_document_that_cannot_be_mapped_is_rejected_as_corrupt_not_as_an_outage() {
        when(mongo.findById(eq("slot1"), eq(DocumentoGuardado.class))).thenThrow(new MappingException("campos con nombres viejos"));

        ExcepcionCargarGuardado lanzado = assertThrows(ExcepcionCargarGuardado.class, () -> repositorio.cargar(1));

        assertEquals(Motivo.CORRUPT, lanzado.motivo());
    }

    @Test
    void aSlotOutsideTheThreeSlotsIsRejectedBeforeAskingMongoDb() {
        assertThrows(IllegalArgumentException.class, () -> repositorio.existeRanura(0));
        assertThrows(IllegalArgumentException.class, () -> repositorio.existeRanura(4));
    }

    @Test
    void theIdOfASlotIsItsNameAndTheCollectionIsPartidas() {
        assertEquals("slot2", DocumentoGuardado.idDe(2));
        assertEquals("partidas", DocumentoGuardado.COLECCION);
    }
}
