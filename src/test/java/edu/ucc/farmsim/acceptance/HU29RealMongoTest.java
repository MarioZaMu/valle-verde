package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.infrastructure.persistence.Checksum;
import edu.ucc.farmsim.infrastructure.persistence.MongoSaveRepository;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * HU-29 to HU-31 against a real MongoDB on {@code localhost:27017} (RN-31, RN-32), in a database of its own
 * ({@code valleverde_test}) that is dropped after each test, so the saved games of the player are never touched.
 * Without a MongoDB on this machine the tests are skipped, as the game itself does not need one to play.
 */
class HU29RealMongoTest {

    private static final String HOST = "localhost";
    private static final int PORT = 27017;
    private static final String DATABASE = "valleverde_test";
    private static final String URI = "mongodb://" + HOST + ":" + PORT
            + "/?serverSelectionTimeoutMS=2000&connectTimeoutMS=2000&socketTimeoutMS=2000";

    private MongoClient client;
    private MongoTemplate mongo;

    @BeforeEach
    void connect() {
        assumeTrue(reachable(), "no MongoDB on " + HOST + ":" + PORT);
        client = MongoClients.create(URI);
        mongo = new MongoTemplate(client, DATABASE);
        mongo.getDb().drop();
    }

    @AfterEach
    void dropAndClose() {
        if (client != null) {
            mongo.getDb().drop();
            client.close();
        }
    }

    private static boolean reachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 500);
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private TestApp app() {
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), new MongoSaveRepository(mongo));
        app.controller.newGame(TestApp.SEED);
        app.controller.skipTutorial();
        return app;
    }

    @Test
    void HU29_ca1_savingWritesTheSlotDocumentIntoTheRealPartidasCollection() {
        TestApp app = app();

        assertTrue(app.controller.save(2).isOk());

        Document stored = mongo.getCollection("partidas").find(new Document("_id", "slot2")).first();
        assertNotNull(stored, "the document is really in MongoDB");
        assertEquals(3, stored.getInteger("schemaVersion"));
        assertEquals(Checksum.of(stored.getString("state")), stored.getString("checksum"));
        assertNotNull(stored.get("savedAt"));
        assertEquals(1, mongo.getCollection("partidas").countDocuments());
    }

    @Test
    void HU29_ca2_whenTheRealMongoRejectsTheWriteThePreviousDocumentStaysAndTheGameGoesOn() {
        TestApp app = app();
        assertTrue(app.controller.save(1).isOk());
        SaveDocument before = mongo.findById("slot1", SaveDocument.class);
        app.controller.endDay();
        mongo.getDb().runCommand(new Document("collMod", "partidas")
                .append("validator", new Document("schemaVersion", new Document("$gt", 1000)))
                .append("validationLevel", "strict").append("validationAction", "error"));

        Result<?> result = app.controller.save(1);

        assertEquals(ErrorCode.ERROR_ES, result.code());
        assertEquals(before, mongo.findById("slot1", SaveDocument.class), "the old document is intact");
        assertEquals(2, app.controller.hud().value().day(), "the game is still in play");
    }

    @Test
    void HU29_ca2_whenMongoDoesNotAnswerSavingFailsWithinSecondsAndTheGameGoesOn() {
        try (MongoClient nowhere = MongoClients.create(
                "mongodb://127.0.0.1:1/?serverSelectionTimeoutMS=500&connectTimeoutMS=500")) {
            TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(),
                    new MongoSaveRepository(new MongoTemplate(nowhere, DATABASE)));
            app.controller.newGame(TestApp.SEED);
            long start = System.nanoTime();

            Result<?> result = app.controller.save(1);

            assertEquals(ErrorCode.ERROR_ES, result.code());
            assertTrue((System.nanoTime() - start) / 1_000_000 < 3_000, "gives up within the 2 s of RN-31");
            assertTrue(app.controller.hud().isOk());
        }
    }

    @Test
    void HU31_ca1_aGameSavedInTheRealMongoLoadsBackExactlyInAnotherSession() {
        TestApp first = app();
        assertTrue(first.controller.plant(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW, "CARROT_SEED").isOk());
        first.controller.endDay();
        assertTrue(first.controller.save(3).isOk());
        HudView hud = first.controller.hud().value();
        MapView map = first.controller.map().value();

        TestApp second = TestApp.create(new GameSession(), DayPipeline.standard(), new MongoSaveRepository(mongo));
        second.controller.newGame(TestApp.SEED + 7);
        Result<HudView> loaded = second.controller.load(3);

        assertTrue(loaded.isOk(), String.valueOf(loaded.message()));
        assertEquals(hud, second.controller.hud().value());
        assertEquals(map, second.controller.map().value());
        assertEquals("OK", second.controller.saveSlots().value().get(2).status());
    }
}
