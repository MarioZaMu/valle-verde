package edu.ucc.farmsim.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.ConnectionString;
import edu.ucc.farmsim.domain.save.SaveStorageException;
import edu.ucc.farmsim.infrastructure.persistence.MongoSaveRepository;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.junit.jupiter.api.Assumptions;

/**
 * El mismo repositorio contra un MongoDB real. Solo corre cuando uno responde en {@code localhost:27017} (o en la
 * dirección de {@code VALLEVERDE_TEST_MONGO_URI}); si no, se omite con un aviso, y el resto del build no depende de él
 * (RT-11). Cada ejecución usa su propia base de datos desechable.
 */
class MongoSaveRepositoryRealMongoTest {

    private static final String DEFAULT_URI = "mongodb://localhost:27017";

    private static MongoClient client;
    private static String database;
    private static MongoTemplate template;

    @BeforeAll
    static void connectOrSkip() {
        String uri = System.getenv().getOrDefault("VALLEVERDE_TEST_MONGO_URI", DEFAULT_URI);
        MongoClient candidate = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(1, TimeUnit.SECONDS))
                .build());
        try {
            candidate.getDatabase("admin").runCommand(new Document("ping", 1));
        } catch (RuntimeException e) {
            candidate.close();
            Assumptions.abort("No MongoDB answers at " + uri + ": the real-database test is skipped");
        }
        client = candidate;
        database = "valleverde_test_" + UUID.randomUUID().toString().replace("-", "");
        template = new MongoTemplate(new SimpleMongoClientDatabaseFactory(client, database));
    }

    @AfterAll
    static void dropDatabase() {
        if (client != null) {
            client.getDatabase(database).drop();
            client.close();
        }
    }

    @Test
    void emptySlotsAreReportedAsEmptyAndAStoredDocumentMakesItsSlotExist() {
        MongoSaveRepository repository = new MongoSaveRepository(template);
        assertFalse(repository.slotExists(1));
        assertFalse(repository.slotExists(2));

        template.save(new SaveDocument(SaveDocument.idOf(2), 1, Instant.now(), "{}", "abc"));

        assertFalse(repository.slotExists(1));
        assertTrue(repository.slotExists(2));
        assertFalse(repository.slotExists(3));
        assertTrue(template.getCollectionNames().contains(SaveDocument.COLLECTION));
    }

    @Test
    void anUnreachableMongoDbBecomesASaveStorageException() {
        MongoClient lost = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://127.0.0.1:1"))
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(300, TimeUnit.MILLISECONDS))
                .build());
        try {
            MongoSaveRepository repository = new MongoSaveRepository(
                    new MongoTemplate(new SimpleMongoClientDatabaseFactory(lost, "valleverde")));

            assertThrows(SaveStorageException.class, () -> repository.slotExists(1));
        } finally {
            lost.close();
        }
    }
}
