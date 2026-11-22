package edu.ucc.farmsim.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.save.SaveLoadException;
import edu.ucc.farmsim.domain.save.SaveLoadException.Reason;
import edu.ucc.farmsim.domain.save.SaveStorageException;
import edu.ucc.farmsim.infrastructure.persistence.Checksum;
import edu.ucc.farmsim.infrastructure.persistence.SaveCodec;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.RichGame;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** The format of a saved game (RN-32) and the order of the checks of RN-33, without any database. */
class SaveCodecTest {

    private final SaveCodec codec = new SaveCodec();
    private final GameMemento memento = RichGame.play().state().createMemento();

    @Test
    void aGameSurvivesTheRoundTripAndTheDocumentHasTheFieldsOfRn32() {
        SaveDocument document = codec.encode(3, memento, Instant.parse("2026-11-22T10:30:00Z"));

        assertEquals("slot3", document.id());
        assertEquals(1, document.schemaVersion());
        assertEquals(Checksum.of(document.state()), document.checksum());
        assertEquals(memento, codec.decode(document));
    }

    @Test
    void theStateTextIsStableForTheSameGame() {
        assertEquals(codec.encode(1, memento, Instant.EPOCH).state(), codec.encode(1, memento, Instant.EPOCH).state());
    }

    private Reason reasonOf(SaveDocument document) {
        return assertThrows(SaveLoadException.class, () -> codec.decode(document)).reason();
    }

    @Test
    void theChecksOfRn33RunInOrderAndStopAtTheFirstFailure() {
        SaveDocument good = codec.encode(1, memento, Instant.EPOCH);

        // 2. not well-formed wins over a wrong version and a wrong checksum
        assertEquals(Reason.CORRUPT, reasonOf(new SaveDocument("slot1", 99, Instant.EPOCH, "{oops", "x")));
        // 3. the version wins over a wrong checksum
        assertEquals(Reason.UNSUPPORTED_VERSION, reasonOf(new SaveDocument("slot1", 99, Instant.EPOCH, "{}", "x")));
        // 4. the checksum
        assertEquals(Reason.CORRUPT, reasonOf(new SaveDocument("slot1", 1, Instant.EPOCH, good.state() + " ", "x")));
        // a state that is valid JSON with a valid checksum but is not a game
        String notAGame = "{\"unknown\":1}";
        assertEquals(Reason.CORRUPT,
                reasonOf(new SaveDocument("slot1", 1, Instant.EPOCH, notAGame, Checksum.of(notAGame))));
        // a document without state
        assertEquals(Reason.CORRUPT, reasonOf(new SaveDocument("slot1", 1, Instant.EPOCH, null, null)));
        // JSON null
        assertEquals(Reason.CORRUPT, reasonOf(new SaveDocument("slot1", 1, Instant.EPOCH, "null", Checksum.of("null"))));
        // a date that does not exist (day 40)
        String badDay = good.state().replaceFirst("\"day\":\\d+", "\"day\":40");
        assertEquals(Reason.CORRUPT, reasonOf(new SaveDocument("slot1", 1, Instant.EPOCH, badDay, Checksum.of(badDay))));
    }

    @Test
    void theRepositoryReadsWhatItWroteAndReportsTheSlots() {
        InMemoryMongo mongo = new InMemoryMongo();
        var repository = mongo.repository();
        assertEquals(false, repository.slotExists(1));

        repository.save(1, memento, Instant.parse("2026-11-22T10:30:00Z"));

        assertTrue(repository.slotExists(1));
        assertEquals(memento, repository.load(1));
        var slots = repository.slots();
        assertEquals(3, slots.size());
        assertEquals("OK", slots.get(0).status().name());
        assertEquals(memento.money(), slots.get(0).money());
        assertEquals("EMPTY", slots.get(1).status().name());
        assertEquals(Reason.MISSING, assertThrows(SaveLoadException.class, () -> repository.load(2)).reason());
    }

    @Test
    void anOutageOnSavingLoadingOrListingBecomesASaveStorageException() {
        InMemoryMongo mongo = new InMemoryMongo();
        var repository = mongo.repository();
        mongo.goDown();

        assertThrows(SaveStorageException.class, () -> repository.save(1, memento, Instant.EPOCH));
        assertThrows(SaveStorageException.class, () -> repository.load(1));
        assertThrows(SaveStorageException.class, repository::slots);
        assertThrows(IllegalArgumentException.class, () -> repository.save(4, memento, Instant.EPOCH));
        assertThrows(IllegalArgumentException.class, () -> repository.load(0));
    }
}
