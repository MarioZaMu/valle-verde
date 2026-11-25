package edu.ucc.farmsim.infraestructura;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import edu.ucc.farmsim.dominio.guardado.ExcepcionAlmacenamientoGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.RepositorioGuardadoMongo;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * El mismo repositorio contra un MongoDB real. Solo corre cuando uno responde en {@code localhost:27017} (o en la
 * dirección de {@code VALLEVERDE_TEST_MONGO_URI}); si no, se omite con un aviso, y el resto del build no depende de él
 * (RT-11). Cada ejecución usa su propia base de datos desechable.
 */
class RepositorioGuardadoMongoRealTest {

    private static final String URI_PREDETERMINADO = "mongodb://localhost:27017";

    private static MongoClient cliente;
    private static String basedatos;
    private static MongoTemplate plantilla;

    @BeforeAll
    static void conectarOSaltar() {
        String uri = System.getenv().getOrDefault("VALLEVERDE_TEST_MONGO_URI", URI_PREDETERMINADO);
        MongoClient candidate = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToClusterSettings(racimo -> racimo.serverSelectionTimeout(1, TimeUnit.SECONDS))
                .build());
        try {
            candidate.getDatabase("admin").runCommand(new Document("ping", 1));
        } catch (RuntimeException e) {
            candidate.close();
            Assumptions.abort("No MongoDB answers at " + uri + ": the real-database test is skipped");
        }
        cliente = candidate;
        basedatos = "valleverde_test_" + UUID.randomUUID().toString().replace("-", "");
        plantilla = new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, basedatos));
    }

    @AfterAll
    static void basedatosSoltar() {
        if (cliente != null) {
            cliente.getDatabase(basedatos).drop();
            cliente.close();
        }
    }

    @Test
    void emptySlotsAreReportedAsEmptyAndAStoredDocumentMakesItsSlotExist() {
        RepositorioGuardadoMongo repositorio = new RepositorioGuardadoMongo(plantilla);
        assertFalse(repositorio.existeRanura(1));
        assertFalse(repositorio.existeRanura(2));

        plantilla.save(new DocumentoGuardado(DocumentoGuardado.idDe(2), 1, Instant.now(), "{}", "abc"));

        assertFalse(repositorio.existeRanura(1));
        assertTrue(repositorio.existeRanura(2));
        assertFalse(repositorio.existeRanura(3));
        assertTrue(plantilla.getCollectionNames().contains(DocumentoGuardado.COLECCION));
    }

    @Test
    void anUnreachableMongoDbBecomesASaveStorageException() {
        MongoClient perdido = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://127.0.0.1:1"))
                .applyToClusterSettings(racimo -> racimo.serverSelectionTimeout(300, TimeUnit.MILLISECONDS))
                .build());
        try {
            RepositorioGuardadoMongo repositorio = new RepositorioGuardadoMongo(
                    new MongoTemplate(new SimpleMongoClientDatabaseFactory(perdido, "valleverde")));

            assertThrows(ExcepcionAlmacenamientoGuardado.class, () -> repositorio.existeRanura(1));
        } finally {
            perdido.close();
        }
    }
}
