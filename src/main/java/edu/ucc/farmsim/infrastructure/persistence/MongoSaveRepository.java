package edu.ucc.farmsim.infrastructure.persistence;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.save.SaveRepository;
import edu.ucc.farmsim.domain.save.SaveStorageException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * Stores each game as a document of the {@code partidas} collection, one per slot (RN-31, DT-21). For now it
 * only reports which slots hold a document; writing and reading come with HU-29 and HU-30. Every technical
 * failure of MongoDB becomes a {@link SaveStorageException}, so nothing above this class knows about MongoDB.
 *
 * <p>The {@code no-mongo} profile leaves it out, so the tests of the API and the client run without a
 * database.
 *
 * <p>Stories: HU-33.
 */
@Repository
@Profile("!no-mongo")
public class MongoSaveRepository implements SaveRepository {

    private static final Logger LOG = Logger.getLogger(MongoSaveRepository.class.getName());
    private static final String UNREACHABLE = "MongoDB no respondió";

    private final MongoTemplate mongo;

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
        if (slot < 1 || slot > GameRules.SAVE_SLOTS) {
            throw new IllegalArgumentException("slot must be between 1 and " + GameRules.SAVE_SLOTS + ": " + slot);
        }
        try {
            return mongo.exists(query(where("_id").is(SaveDocument.idOf(slot))), SaveDocument.class);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "MongoDB failed while looking for slot " + slot, e);
            throw new SaveStorageException(UNREACHABLE, e);
        }
    }
}
