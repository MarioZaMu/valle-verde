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
 * Convierte una partida en el documento de una ranura y viceversa (RN-32, DT-11): el estado como texto JSON con
 * Jackson, la versión del formato y la suma de control. La lectura sigue el orden de RN-33: texto bien formado, versión
 * admitida, suma de control coincidente y, al final, una partida que se pueda construir desde el texto. La versión 2
 * agregó el avatar (RN-36); un documento de versión 1 aún se lee, y su partida no tiene avatar, así que empieza en su
 * lugar. La versión 3 amplió el mapa al valle de 48 x 24 (RN-40): los cultivos de un documento más antiguo conservan
 * sus parcelas en su esquina noroeste.
 *
 * <p>Stories: HU-29, HU-30, HU-31, HU-45, HU-50.
 */
public final class SaveCodec {

    /** Versión del formato de {@code state} que se escribe. */
    public static final int SCHEMA_VERSION = 3;

    /** Primera versión con el valle de 48 x 24; los documentos anteriores guardan los cultivos del mapa de 16 x 10. */
    public static final int VALLEY_VERSION = 3;

    /** Versión más antigua que todavía se puede leer: la versión 1 no tiene avatar. */
    public static final int OLDEST_READABLE_VERSION = 1;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    /** El documento de una ranura para la partida, con la versión actual y la suma de control de su texto. */
    public SaveDocument encode(int slot, GameMemento memento, Instant savedAt) {
        try {
            String state = mapper.writeValueAsString(memento);
            return new SaveDocument(SaveDocument.idOf(slot), SCHEMA_VERSION, savedAt, state, Checksum.of(state));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The game cannot be written as JSON", e);
        }
    }

    /**
     * La partida guardada en un documento.
     *
     * @throws SaveLoadException si el texto no es válido, la versión es desconocida, la suma de control difiere o el
     *                           texto no describe una partida
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
