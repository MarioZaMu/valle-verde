package edu.ucc.farmsim.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.ucc.farmsim.domain.save.SaveStorageException;
import edu.ucc.farmsim.infrastructure.persistence.MongoSaveRepository;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/** RN-31: the repository asks MongoDB which slots exist and hides every technical failure (DT-21). */
class MongoSaveRepositoryTest {

    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final MongoSaveRepository repository = new MongoSaveRepository(mongo);

    @Test
    void aSlotWhoseDocumentExistsIsReportedAsExisting() {
        when(mongo.exists(any(Query.class), eq(SaveDocument.class))).thenReturn(true);

        assertTrue(repository.slotExists(2));
    }

    @Test
    void aSlotWithoutADocumentIsReportedAsEmpty() {
        when(mongo.exists(any(Query.class), eq(SaveDocument.class))).thenReturn(false);

        assertFalse(repository.slotExists(1));
    }

    @Test
    void theQueryTargetsTheIdOfTheSlot() {
        when(mongo.exists(any(Query.class), eq(SaveDocument.class))).thenAnswer(call -> {
            Query query = call.getArgument(0);
            return query.getQueryObject().toJson().contains("slot3");
        });

        assertTrue(repository.slotExists(3));
        assertFalse(repository.slotExists(1));
    }

    @Test
    void whenMongoDbDoesNotAnswerTheFailureBecomesASaveStorageException() {
        DataAccessResourceFailureException outage = new DataAccessResourceFailureException("timed out");
        when(mongo.exists(any(Query.class), eq(SaveDocument.class))).thenThrow(outage);

        SaveStorageException thrown = assertThrows(SaveStorageException.class, () -> repository.slotExists(1));

        assertEquals("MongoDB no respondió", thrown.getMessage());
        assertSame(outage, thrown.getCause());
    }

    @Test
    void aSlotOutsideTheThreeSlotsIsRejectedBeforeAskingMongoDb() {
        assertThrows(IllegalArgumentException.class, () -> repository.slotExists(0));
        assertThrows(IllegalArgumentException.class, () -> repository.slotExists(4));
    }

    @Test
    void theIdOfASlotIsItsNameAndTheCollectionIsPartidas() {
        assertEquals("slot2", SaveDocument.idOf(2));
        assertEquals("partidas", SaveDocument.COLLECTION);
    }
}
