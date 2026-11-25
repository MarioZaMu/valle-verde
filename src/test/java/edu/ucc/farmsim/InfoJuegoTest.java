package edu.ucc.farmsim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** RNF-12: el nombre visible del juego vive en una sola constante. */
class InfoJuegoTest {

    @Test
    void RNF12_juego_nombre_es_definido_en_uno_no_en_blanco_constante() {
        assertFalse(InfoJuego.NOMBRE.isBlank());
    }

    @Test
    void RNF12_version_mira_como_semantico_version() {
        assertTrue(InfoJuego.VERSION.matches("\\d+\\.\\d+\\.\\d+"));
    }
}
