package edu.ucc.farmsim.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The SHA-256 of the text of a saved game, in hexadecimal (RN-32), to notice any change made to it.
 *
 * <p>Stories: HU-29, HU-31.
 */
public final class Checksum {

    private Checksum() {
        // static helpers only
    }

    /** SHA-256 of the UTF-8 bytes of the text, in lower-case hexadecimal. */
    public static String of(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime has SHA-256", e);
        }
    }
}
