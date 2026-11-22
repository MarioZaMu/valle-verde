package edu.ucc.farmsim.infrastructure.persistence;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.save.SaveLoadException;
import edu.ucc.farmsim.domain.save.SaveLoadException.Reason;
import edu.ucc.farmsim.domain.save.SaveRepository;
import edu.ucc.farmsim.domain.save.SaveStorageException;
import edu.ucc.farmsim.domain.save.SlotInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * Stores each game as a document of the {@code partidas} collection, one per slot (RN-31, DT-21). Saving
 * replaces the whole document in one operation, so a failed write leaves the previous one intact. Every
 * technical failure of MongoDB becomes a {@link SaveStorageException}, so nothing above this class knows about
 * MongoDB.
 *
 * <p>The {@code no-mongo} profile leaves it out, so the tests of the API and the client run without a
 * database.
 *
 * <p>Stories: HU-29, HU-30, HU-31, HU-32, HU-33.
 */
@Repository
@Profile("!no-mongo")
public class MongoSaveRepository implements SaveRepository {

    private static final Logger LOG = Logger.getLogger(MongoSaveRepository.class.getName());
    private static final String UNREACHABLE = "MongoDB no respondió";

    private final MongoTemplate mongo;
    private final SaveCodec codec = new SaveCodec();

    /**
     * Creates the repository.
     *
     * @param mongo Spring Data's access to the database configured in {@code valleverde.mongo.uri}
     */
    public MongoSaveRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public boolean slotExists(int slot) {
        requireSlot(slot);
        return guarded("looking for slot " + slot,
                () -> mongo.exists(query(where("_id").is(SaveDocument.idOf(slot))), SaveDocument.class));
    }

    @Override
    public void save(int slot, GameMemento memento, Instant savedAt) {
        requireSlot(slot);
        SaveDocument document = codec.encode(slot, memento, savedAt);
        guarded("saving slot " + slot, () -> mongo.save(document));
    }

    @Override
    public GameMemento load(int slot) {
        requireSlot(slot);
        SaveDocument document = guarded("reading slot " + slot, () -> mongo.findById(SaveDocument.idOf(slot), SaveDocument.class));
        if (document == null) {
            throw new SaveLoadException(Reason.MISSING, "slot " + slot + " holds no document");
        }
        return codec.decode(document);
    }

    @Override
    public List<SlotInfo> slots() {
        List<SlotInfo> slots = new ArrayList<>();
        for (int slot = 1; slot <= GameRules.SAVE_SLOTS; slot++) {
            int number = slot;
            SaveDocument document = guarded("reading slot " + number,
                    () -> mongo.findById(SaveDocument.idOf(number), SaveDocument.class));
            slots.add(document == null ? SlotInfo.empty(slot) : describe(slot, document));
        }
        return slots;
    }

    private SlotInfo describe(int slot, SaveDocument document) {
        try {
            GameMemento memento = codec.decode(document);
            return SlotInfo.ok(slot, memento.date(), memento.money(), document.savedAt());
        } catch (SaveLoadException e) {
            LOG.log(Level.WARNING, "Slot " + slot + " is damaged: " + e.getMessage());
            return SlotInfo.damaged(slot);
        }
    }

    private static void requireSlot(int slot) {
        if (slot < 1 || slot > GameRules.SAVE_SLOTS) {
            throw new IllegalArgumentException("slot must be between 1 and " + GameRules.SAVE_SLOTS + ": " + slot);
        }
    }

    private <T> T guarded(String what, Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "MongoDB failed while " + what, e);
            throw new SaveStorageException(UNREACHABLE, e);
        }
    }
}
