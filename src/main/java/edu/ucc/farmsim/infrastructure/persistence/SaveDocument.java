package edu.ucc.farmsim.infrastructure.persistence;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The document of one save slot in the {@code partidas} collection (RN-32). {@code state} is the game as
 * JSON text and {@code checksum} its SHA-256, so a damaged document is detected on load (RN-33).
 *
 * <p>Stories: HU-33.
 *
 * @param id            {@code slot1}, {@code slot2} or {@code slot3}
 * @param schemaVersion version of the format of {@code state}
 * @param savedAt       when the game was saved
 * @param state         the game, as JSON text
 * @param checksum      SHA-256 of {@code state}, in hexadecimal
 */
@Document(collection = SaveDocument.COLLECTION)
public record SaveDocument(@Id String id, int schemaVersion, Instant savedAt, String state, String checksum) {

    /** Name of the MongoDB collection that holds the slots. */
    public static final String COLLECTION = "partidas";

    /** The {@code _id} of a slot. */
    public static String idOf(int slot) {
        return "slot" + slot;
    }
}
