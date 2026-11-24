package edu.ucc.farmsim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** RNF-12: el nombre visible del juego vive en una sola constante. */
class GameInfoTest {

    @Test
    void RNF12_gameNameIsDefinedInOneNonBlankConstant() {
        assertFalse(GameInfo.NAME.isBlank());
    }

    @Test
    void RNF12_versionLooksLikeSemanticVersion() {
        assertTrue(GameInfo.VERSION.matches("\\d+\\.\\d+\\.\\d+"));
    }
}
