package edu.ucc.farmsim.infrastructure.persistence;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * El documento de una ranura de guardado en la colección {@code partidas} (RN-32). {@code state} es la partida como
 * texto JSON y {@code checksum} su SHA-256, de modo que un documento dañado se detecta al cargar (RN-33).
 *
 * <p>Stories: HU-33.
 *
 * @param id            {@code slot1}, {@code slot2} or {@code slot3}
 * @param schemaVersion versión del formato de {@code state}
 * @param savedAt       cuándo se guardó la partida
 * @param state         la partida, como texto JSON
 * @param checksum      SHA-256 de {@code state}, en hexadecimal
 */
@Document(collection = SaveDocument.COLLECTION)
public record SaveDocument(@Id String id, int schemaVersion, Instant savedAt, String state, String checksum) {

    /** Nombre de la colección de MongoDB que contiene las ranuras. */
    public static final String COLLECTION = "partidas";

    /** El {@code _id} de una ranura. */
    public static String idOf(int slot) {
        return "slot" + slot;
    }
}
