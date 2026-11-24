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
 * Un MongoDB que vive en un mapa: encima de él corre el {@link MongoSaveRepository} real (codec, suma de control,
 * formato), de modo que guardar, cargar y los documentos dañados se prueban de punta a punta sin base de datos. Las
 * pruebas pueden leer y cambiar los documentos guardados, o hacer que el «servidor» deje de responder.
 */
public final class InMemoryMongo {

    private final Map<String, SaveDocument> documents = new HashMap<>();
    private final MongoTemplate template = mock(MongoTemplate.class);
    private boolean down;
    private final MongoSaveRepository repository;

    /** Crea una base de datos vacía y el repositorio sobre ella. */
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

    /** El repositorio que usa la aplicación. */
    public MongoSaveRepository repository() {
        return repository;
    }

    /** El documento guardado en una ranura, o {@code null}. */
    public SaveDocument document(int slot) {
        return documents.get(SaveDocument.idOf(slot));
    }

    /** Reemplaza el documento de una ranura por una copia cambiada (para dañarlo). */
    public void tamper(int slot, UnaryOperator<SaveDocument> change) {
        documents.compute(SaveDocument.idOf(slot), (id, old) -> change.apply(old));
    }

    /** Guarda un documento tal como está. */
    public void put(SaveDocument document) {
        documents.put(document.id(), document);
    }

    /** Hace que el «servidor» deje de responder, como si se agotara el tiempo. */
    public void goDown() {
        down = true;
    }

    /** Hace que el «servidor» vuelva a responder. */
    public void comeBack() {
        down = false;
    }
}
