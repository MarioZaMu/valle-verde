package edu.ucc.farmsim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** RNF-16: one jar starts the server and the window, or each one on its own. */
class LaunchOptionsTest {

    @Test
    void noArgumentsStartsTheServerAndTheWindow() {
        LaunchOptions options = LaunchOptions.parse();

        assertEquals(LaunchOptions.Mode.BOTH, options.mode());
        assertTrue(options.needsServer());
        assertTrue(options.needsWindow());
        assertNull(options.port());
        assertNull(options.url());
    }

    @Test
    void serverOnlyDoesNotOpenAWindow() {
        LaunchOptions options = LaunchOptions.parse("--server", "--port", "9000");

        assertEquals(LaunchOptions.Mode.SERVER_ONLY, options.mode());
        assertTrue(options.needsServer());
        assertFalse(options.needsWindow());
        assertEquals(9000, options.port());
        assertArrayEquals(new String[] {"--server.port=9000"}, options.serverArguments());
    }

    @Test
    void clientOnlyDoesNotStartTheServerAndTakesTheUrl() {
        LaunchOptions options = LaunchOptions.parse("--client", "--url=http://127.0.0.1:9000");

        assertEquals(LaunchOptions.Mode.CLIENT_ONLY, options.mode());
        assertFalse(options.needsServer());
        assertTrue(options.needsWindow());
        assertEquals("http://127.0.0.1:9000", options.url());
    }

    @Test
    void bothFormsOfEachOptionAreAccepted() {
        assertEquals(8080, LaunchOptions.parse("--port=8080").port());
        assertEquals("http://x:1", LaunchOptions.parse("--url", "http://x:1").url());
    }

    @Test
    void otherArgumentsGoToSpringUntouched() {
        LaunchOptions options = LaunchOptions.parse("--spring.profiles.active=demo", "--server");

        assertArrayEquals(new String[] {"--spring.profiles.active=demo"}, options.serverArguments());
    }

    @Test
    void serverAndClientTogetherAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--server", "--client"));
    }

    @Test
    void aPortThatIsNotANumberOrOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--port=abc"));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--port=70000"));
    }
}
