package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.infrastructure.persistence.Checksum;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.RichGame;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-29: saving a game in one of three slots as a document with a version and a checksum (RN-31, RN-32). */
class HU29SaveGameTest {

    private final InMemoryMongo mongo = new InMemoryMongo();

    private TestApp appWith(InMemoryMongo database) {
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), database.repository());
        app.controller.newGame(TestApp.SEED);
        return app;
    }

    @Test
    void HU29_ca1_savingCreatesTheSlotDocumentWithVersionDateStateAndChecksum() {
        // Given a game in progress
        TestApp app = appWith(mongo);

        // When the player saves in slot 2
        Result<SaveSlotView> result = app.controller.save(2);

        // Then the document slot2 exists with schemaVersion 3 (1 before the avatar, 2 before the valley), savedAt, state and the SHA-256 of the state
        assertTrue(result.isOk(), String.valueOf(result.message()));
        SaveDocument document = mongo.document(2);
        assertNotNull(document);
        assertEquals("slot2", document.id());
        assertEquals(3, document.schemaVersion());
        assertEquals(TestApp.FIXED_CLOCK.instant(), document.savedAt());
        assertTrue(document.state().startsWith("{"));
        assertEquals(Checksum.of(document.state()), document.checksum());
        assertEquals(64, document.checksum().length());
        assertEquals("OK", result.value().status());
        assertEquals("2026-11-22 10:30", result.value().savedAt());
        assertEquals(null, mongo.document(1));
    }

    @Test
    void HU29_ca2_aFailedWriteLeavesThePreviousDocumentAndTheGameInPlay() {
        TestApp app = appWith(mongo);
        app.controller.save(1);
        SaveDocument before = mongo.document(1);
        app.controller.endDay();
        mongo.goDown();

        Result<SaveSlotView> result = app.controller.save(1);

        assertEquals(ErrorCode.ERROR_ES, result.code());
        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", result.message());
        assertEquals(before, mongo.document(1));
        assertEquals(2, app.controller.hud().value().day());
        mongo.comeBack();
        assertTrue(app.controller.save(1).isOk());
    }

    @Test
    void HU29_ca3_savingInAnOccupiedSlotReplacesItsDocument() {
        TestApp app = appWith(mongo);
        app.controller.save(3);
        String first = mongo.document(3).state();
        app.controller.endDay();

        app.controller.save(3);

        assertTrue(!first.equals(mongo.document(3).state()));
        assertEquals(Checksum.of(mongo.document(3).state()), mongo.document(3).checksum());
    }

    @Test
    void HU29_withoutAGameThereIsNothingToSave() {
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());

        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.save(1).code());
    }

    @Test
    void HU29_aSlotOutsideTheThreeIsAnInternalErrorAndWritesNothing() {
        TestApp app = appWith(mongo);

        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.save(4).code());
        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.save(0).code());
    }

    @Test
    void HU29_aRichGameIsWrittenAsOneValidDocument() {
        TestApp app = RichGame.play();
        InMemoryMongo database = new InMemoryMongo();
        TestApp saving = TestApp.create(app.session, DayPipeline.standard(), database.repository());

        assertTrue(saving.controller.save(1).isOk());

        assertTrue(database.document(1).state().contains("\"buildings\""));
        assertTrue(database.document(1).state().length() < 200 * 1024);
    }
}
