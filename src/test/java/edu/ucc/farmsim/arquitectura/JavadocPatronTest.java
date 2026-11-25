package edu.ucc.farmsim.arquitectura;

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
class JavadocPatronTest {

    private static final Path PADRON = Path.of("src/test/resources/patterns.txt");
    private static final Path FUENTES = Path.of("src/main/java");

    /** Una entrada del registro. */
    record Entry(String nombreClase, String patron, String rol) {

        String javadocEsperado() {
            return "Pattern: " + patron + " — Role: " + rol;
        }
    }

    static List<Entry> parse(List<String> lineas) {
        List<Entry> entradas = new ArrayList<>();
        for (String crudo : lineas) {
            String linea = crudo.strip();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            String[] partes = linea.split("\\|");
            if (partes.length != 3) {
                throw new IllegalArgumentException("Bad registry line (expected 'class | pattern | role'): " + crudo);
            }
            entradas.add(new Entry(partes[0].strip(), partes[1].strip(), partes[2].strip()));
        }
        return entradas;
    }

    @Test
    void AR9_cada_registrado_patron_clase_documentos_su_patron_y_rol() throws IOException {
        List<Entry> entradas = parse(Files.readAllLines(PADRON, StandardCharsets.UTF_8));
        for (Entry e : entradas) {
            Path fuente = FUENTES.resolve(e.nombreClase().replace('.', '/') + ".java");
            assertTrue(Files.exists(fuente), "Registered class has no source file: " + e.nombreClase());
            String texto = Files.readString(fuente, StandardCharsets.UTF_8);
            assertTrue(texto.contains(e.javadocEsperado()),
                    e.nombreClase() + " must contain the Javadoc line: " + e.javadocEsperado());
        }
    }

    @Test
    void AR9_padron_analizador_lee_entradas_y_ignora_comentarios() {
        List<Entry> entradas = parse(List.of("# comment", "", "a.b.C | Singleton | Game clock"));
        assertEquals(1, entradas.size());
        assertEquals("Pattern: Singleton — Role: Game clock", entradas.get(0).javadocEsperado());
    }

    @Test
    void AR9_padron_analizador_rechaza_malformado_lineas() {
        try {
            parse(List.of("only-one-field"));
        } catch (IllegalArgumentException esperado) {
            assertFalse(esperado.getMessage().isBlank());
            return;
        }
        throw new AssertionError("A malformed line must be rejected");
    }
}
