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
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.RepositorioGuardadoMongo;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
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
    void idElObjetivosConsultaElDeRanuraEl() {
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
