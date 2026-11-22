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
 * AR-9: every class that implements a design pattern carries the Javadoc line
 * {@code Pattern: <name> — Role: <role>}.
 *
 * <p>Classes are registered in {@code src/test/resources/patterns.txt} (one per line:
 * {@code fully.qualified.ClassName | Pattern | Role}). Each phase adds its pattern classes there, so a
 * class that implements a pattern without documenting it fails the build.
 */
class PatternJavadocTest {

    private static final Path REGISTRY = Path.of("src/test/resources/patterns.txt");
    private static final Path SOURCES = Path.of("src/main/java");

    /** One registry entry. */
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
