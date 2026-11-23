package edu.ucc.farmsim.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.save.SaveLoadException;
import edu.ucc.farmsim.domain.save.SaveLoadException.Reason;
import java.time.Instant;

/**
 * Turns a game into the document of a slot and back (RN-32, DT-11): the state as JSON text with Jackson, the
 * format version and the checksum. Reading follows the order of RN-33: well-formed text, supported version,
 * matching checksum and, last, a game that can be built from the text. Version 2 added the avatar (RN-36); a
 * document of version 1 is still read, and its game has no avatar, so it starts at its place. Version 3 grew the
 * map to the valley of 48 x 24 (RN-40): the crops of an older document keep their plots in its north-west corner.
 *
 * <p>Stories: HU-29, HU-30, HU-31, HU-45, HU-50.
 */
public final class SaveCodec {

    /** Version of the format of {@code state} that is written. */
    public static final int SCHEMA_VERSION = 3;

    /** First version with the valley of 48 x 24; older documents hold the crops of the 16 x 10 map. */
    public static final int VALLEY_VERSION = 3;

    /** Oldest version that can still be read: version 1 has no avatar. */
    public static final int OLDEST_READABLE_VERSION = 1;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    /** The document of a slot for the game, with the current version and the checksum of its text. */
    public SaveDocument encode(int slot, GameMemento memento, Instant savedAt) {
        try {
            String state = mapper.writeValueAsString(memento);
            return new SaveDocument(SaveDocument.idOf(slot), SCHEMA_VERSION, savedAt, state, Checksum.of(state));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The game cannot be written as JSON", e);
        }
    }

    /**
     * The game kept in a document.
     *
     * @throws SaveLoadException if the text is not valid, the version is unknown, the checksum differs or the
     *                           text does not describe a game
     */
    public GameMemento decode(SaveDocument document) {
        String state = document.state();
        if (state == null) {
            throw new SaveLoadException(Reason.CORRUPT, "the document has no state");
        }
        try {
            mapper.readTree(state);
        } catch (JsonProcessingException e) {
            throw new SaveLoadException(Reason.CORRUPT, "the state is not well-formed JSON: " + e.getOriginalMessage());
        }
        if (document.schemaVersion() < OLDEST_READABLE_VERSION || document.schemaVersion() > SCHEMA_VERSION) {
            throw new SaveLoadException(Reason.UNSUPPORTED_VERSION, "schemaVersion " + document.schemaVersion());
        }
        if (!Checksum.of(state).equals(document.checksum())) {
            throw new SaveLoadException(Reason.CORRUPT, "the checksum does not match");
        }
        try {
            GameMemento memento = mapper.readValue(state, GameMemento.class);
            if (memento == null) {
                throw new SaveLoadException(Reason.CORRUPT, "the state is empty");
            }
            if (document.schemaVersion() < VALLEY_VERSION && memento.farm() != null && memento.farm().crops() != null
                    && memento.farm().crops().size() == GameRules.LEGACY_MAP_COLS * GameRules.LEGACY_MAP_ROWS) {
                return memento.withFarm(memento.farm().widened(GameRules.LEGACY_MAP_COLS, GameRules.MAP_COLS,
                        GameRules.MAP_ROWS));
            }
            return memento;
        } catch (JsonProcessingException | RuntimeException e) {
            if (e instanceof SaveLoadException failure) {
                throw failure;
            }
            throw new SaveLoadException(Reason.CORRUPT, "the state does not describe a game: " + e.getMessage());
        }
    }
}
