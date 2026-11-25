package edu.ucc.farmsim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** RNF-16: un solo jar arranca el servidor y la ventana, o cada uno por separado. */
class OpcionesLanzamientoTest {

    @Test
    void noServidorElIniciaArgumentosYVentanaEl() {
        OpcionesLanzamiento opciones = OpcionesLanzamiento.parse();

        assertEquals(OpcionesLanzamiento.Modo.BOTH, opciones.modo());
        assertTrue(opciones.servidorNecesita());
        assertTrue(opciones.ventanaNecesita());
        assertNull(opciones.puerto());
        assertNull(opciones.url());
    }

    @Test
    void serverOnlyDoesNotOpenAWindow() {
        OpcionesLanzamiento opciones = OpcionesLanzamiento.parse("--server", "--port", "9000");

        assertEquals(OpcionesLanzamiento.Modo.SERVER_ONLY, opciones.modo());
        assertTrue(opciones.servidorNecesita());
        assertFalse(opciones.ventanaNecesita());
        assertEquals(9000, opciones.puerto());
        assertArrayEquals(new String[] {"--server.port=9000"}, opciones.argumentosServidor());
    }

    @Test
    void haceSoloClienteNoServidorElInicioYUrlElToma() {
        OpcionesLanzamiento opciones = OpcionesLanzamiento.parse("--client", "--url=http://127.0.0.1:9000");

        assertEquals(OpcionesLanzamiento.Modo.CLIENT_ONLY, opciones.modo());
        assertFalse(opciones.servidorNecesita());
        assertTrue(opciones.ventanaNecesita());
        assertEquals("http://127.0.0.1:9000", opciones.url());
    }

    @Test
    void formasAmbosDeSonOpcionCadaAceptado() {
        assertEquals(8080, OpcionesLanzamiento.parse("--port=8080").puerto());
        assertEquals("http://x:1", OpcionesLanzamiento.parse("--url", "http://x:1").url());
    }

    @Test
    void irArgumentosOtroAIntactoPrimavera() {
        OpcionesLanzamiento opciones = OpcionesLanzamiento.parse("--spring.profiles.active=demo", "--server");

        assertArrayEquals(new String[] {"--spring.profiles.active=demo"}, opciones.argumentosServidor());
    }

    @Test
    void servidorYSonJuntosClienteRechazado() {
        assertThrows(IllegalArgumentException.class, () -> OpcionesLanzamiento.parse("--server", "--client"));
    }

    @Test
    void aPortThatIsNotANumberOrOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> OpcionesLanzamiento.parse("--port=abc"));
        assertThrows(IllegalArgumentException.class, () -> OpcionesLanzamiento.parse("--port=70000"));
    }
}
