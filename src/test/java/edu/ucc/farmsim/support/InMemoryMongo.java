package edu.ucc.farmsim.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.ucc.farmsim.infrastructure.persistence.MongoSaveRepository;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * A MongoDB that lives in a map: the real {@link MongoSaveRepository} (codec, checksum, format) runs on top of
 * it, so saving, loading and damaged documents are tested end to end without a database. Tests can read and
 * change the stored documents, or make the "server" stop answering.
 */
public final class InMemoryMongo {

    private final Map<String, SaveDocument> documents = new HashMap<>();
    private final MongoTemplate template = mock(MongoTemplate.class);
    private boolean down;
    private final MongoSaveRepository repository;

    /** Creates an empty database and the repository over it. */
    public InMemoryMongo() {
        when(template.save(any(SaveDocument.class))).thenAnswer(call -> {
            failIfDown();
            SaveDocument document = call.getArgument(0);
            documents.put(document.id(), document);
            return document;
        });
        when(template.findById(anyString(), eq(SaveDocument.class))).thenAnswer(call -> {
            failIfDown();
            return documents.get(call.<String>getArgument(0));
        });
        when(template.exists(any(Query.class), eq(SaveDocument.class))).thenAnswer(call -> {
            failIfDown();
            Query query = call.getArgument(0);
            String json = query.getQueryObject().toJson();
            return documents.keySet().stream().anyMatch(json::contains);
        });
        repository = new MongoSaveRepository(template);
    }

    private void failIfDown() {
        if (down) {
            throw new DataAccessResourceFailureException("timed out after 2000 ms");
        }
    }

    /** The repository the application uses. */
    public MongoSaveRepository repository() {
        return repository;
    }

    /** The document stored in a slot, or {@code null}. */
    public SaveDocument document(int slot) {
        return documents.get(SaveDocument.idOf(slot));
    }

    /** Replaces the document of a slot with a changed copy (to damage it). */
    public void tamper(int slot, UnaryOperator<SaveDocument> change) {
        documents.compute(SaveDocument.idOf(slot), (id, old) -> change.apply(old));
    }

    /** Stores a document as it is. */
    public void put(SaveDocument document) {
        documents.put(document.id(), document);
    }

    /** Makes the "server" stop answering, as if it timed out. */
    public void goDown() {
        down = true;
    }

    /** Makes the "server" answer again. */
    public void comeBack() {
        down = false;
    }
}
