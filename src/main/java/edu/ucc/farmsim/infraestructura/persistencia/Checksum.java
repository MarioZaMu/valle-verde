package edu.ucc.farmsim.infraestructura.persistencia;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * El SHA-256 del texto de una partida guardada, en hexadecimal (RN-32), para notar cualquier cambio hecho en ella.
 *
 * <p>Stories: HU-29, HU-31.
 */
public final class Checksum {

    private Checksum() {
                // solo métodos de utilidad estáticos
    }

    /** SHA-256 de los bytes UTF-8 del texto, en hexadecimal minúscula. */
    public static String of(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime has SHA-256", e);
        }
    }
}
