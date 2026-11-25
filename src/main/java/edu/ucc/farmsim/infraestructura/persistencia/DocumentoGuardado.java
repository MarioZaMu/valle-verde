package edu.ucc.farmsim.infraestructura.persistencia;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * El documento de una ranura de guardado en la colección {@code partidas} (RN-32). {@code estado} es la partida como
 * texto JSON y {@code checksum} su SHA-256, de modo que un documento dañado se detecta al cargar (RN-33).
 *
 * <p>Stories: HU-33.
 *
 * @param id            {@code slot1}, {@code slot2} or {@code slot3}
 * @param versionEsquema versión del formato de {@code estado}
 * @param archivadoEn       cuándo se guardó la partida
 * @param estado         la partida, como texto JSON
 * @param checksum      SHA-256 de {@code estado}, en hexadecimal
 */
@Document(collection = DocumentoGuardado.COLECCION)
public record DocumentoGuardado(@Id String id, int versionEsquema, Instant archivadoEn, String estado, String checksum) {

    /** Nombre de la colección de MongoDB que contiene las ranuras. */
    public static final String COLECCION = "partidas";

    /** El {@code _id} de una ranura. */
    public static String idDe(int ranura) {
        return "slot" + ranura;
    }
}
