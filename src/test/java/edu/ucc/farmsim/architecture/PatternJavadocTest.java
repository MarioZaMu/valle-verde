package edu.ucc.farmsim.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * AR-9: toda clase que implementa un patrón de diseño lleva la línea Javadoc {@code Pattern: <nombre> — Role: <rol>}.
 *
 * <p>Las clases se registran en {@code src/test/resources/patterns.txt} (una por línea: {@code paquete.ClaseCompleta |
 * Pattern | Role}). Cada fase agrega allí sus clases de patrón, de modo que una clase que implementa un patrón sin
 * documentarlo rompe el build.
 */
class PatternJavadocTest {

    private static final Path REGISTRY = Path.of("src/test/resources/patterns.txt");
    private static final Path SOURCES = Path.of("src/main/java");

    /** Una entrada del registro. */
    record Entry(String className, String pattern, String role) {

        String expectedJavadoc() {
            return "Pattern: " + pattern + " — Role: " + role;
        }
    }

    static List<Entry> parse(List<String> lines) {
        List<Entry> entries = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Bad registry line (expected 'class | pattern | role'): " + raw);
            }
            entries.add(new Entry(parts[0].strip(), parts[1].strip(), parts[2].strip()));
        }
        return entries;
    }

    @Test
    void AR9_everyRegisteredPatternClassDocumentsItsPatternAndRole() throws IOException {
        List<Entry> entries = parse(Files.readAllLines(REGISTRY, StandardCharsets.UTF_8));
        for (Entry e : entries) {
            Path source = SOURCES.resolve(e.className().replace('.', '/') + ".java");
            assertTrue(Files.exists(source), "Registered class has no source file: " + e.className());
            String text = Files.readString(source, StandardCharsets.UTF_8);
            assertTrue(text.contains(e.expectedJavadoc()),
                    e.className() + " must contain the Javadoc line: " + e.expectedJavadoc());
        }
    }

    @Test
    void AR9_registryParserReadsEntriesAndIgnoresComments() {
        List<Entry> entries = parse(List.of("# comment", "", "a.b.C | Singleton | Game clock"));
        assertEquals(1, entries.size());
        assertEquals("Pattern: Singleton — Role: Game clock", entries.get(0).expectedJavadoc());
    }

    @Test
    void AR9_registryParserRejectsMalformedLines() {
        try {
            parse(List.of("only-one-field"));
        } catch (IllegalArgumentException expected) {
            assertFalse(expected.getMessage().isBlank());
            return;
        }
        throw new AssertionError("A malformed line must be rejected");
    }
}
