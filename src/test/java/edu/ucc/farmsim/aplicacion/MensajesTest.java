package edu.ucc.farmsim.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Mensajes;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Q-1: cada código de error tiene su mensaje en español, y los mensajes coinciden con la tabla de RN-35. */
class MensajesTest {

    private static final Path REGLAS = Path.of("docs/REGLAS_DE_NEGOCIO.md");
    private static final Pattern FILA = Pattern.compile("^\\| (`[A-Z_`/ ]+`) \\| (.+) \\|$");
    private static final Pattern CODIGO = Pattern.compile("`([A-Z_]+)`");

    @Test
    void Q1_everyErrorCodeHasASpanishMessageTemplate() {
        for (CodigoError codigo : CodigoError.values()) {
            String plantilla = Mensajes.plantilla(codigo);
            assertFalse(plantilla.isBlank(), "missing message for " + codigo);
        }
    }

    @Test
    void Q1_messagesFormatTheirArguments() {
        assertEquals("Fondos insuficientes: faltan 20 m.", Mensajes.of(CodigoError.SIN_FONDOS, 20));
        assertEquals("No hay una partida en curso.", Mensajes.of(CodigoError.SIN_PARTIDA));
    }

    @Test
    void Q1_everyTemplateMatchesTheRn35Table() throws IOException {
        Map<String, String> desdeDocs = readRn35();
        for (CodigoError codigo : CodigoError.values()) {
            assertTrue(desdeDocs.containsKey(codigo.name()), codigo + " is missing from RN-35 in the business rules");
            assertEquals(normalizar(desdeDocs.get(codigo.name())), normalizar(Mensajes.plantilla(codigo)),
                    "message of " + codigo + " differs from RN-35");
        }
        for (String documentado : desdeDocs.keySet()) {
            CodigoError.valueOf(documentado); // todo código documentado debe existir
        }
    }

    private static Map<String, String> readRn35() throws IOException {
        Map<String, String> resultado = new TreeMap<>();
        boolean enSeccion = false;
        for (String linea : Files.readAllLines(REGLAS, StandardCharsets.UTF_8)) {
            if (linea.startsWith("### RN-35")) {
                enSeccion = true;
                continue;
            }
            if (enSeccion && linea.startsWith("## ")) {
                break;
            }
            Matcher fila = FILA.matcher(linea);
            if (!enSeccion || !fila.matches()) {
                continue;
            }
            List<String> codigos = new ArrayList<>();
            Matcher comparadorCodigo = CODIGO.matcher(fila.group(1));
            while (comparadorCodigo.find()) {
                codigos.add(comparadorCodigo.group(1));
            }
            String celda = fila.group(2);
            List<String> mensajes = new ArrayList<>();
            if (codigos.size() == 2) {
                for (String parte : celda.split("» / «")) {
                    mensajes.add(parte.replaceFirst("^«", "").replaceFirst("»$", ""));
                }
            } else {
                mensajes.add(celda.substring(celda.indexOf('«') + 1, celda.lastIndexOf('»')));
            }
            for (int i = 0; i < codigos.size(); i++) {
                resultado.put(codigos.get(i), mensajes.get(i));
            }
        }
        return resultado;
    }

    /** Los marcadores difieren entre el documento ({n}, {cultivo}) y el código (%d, %s): se compara su forma. */
    private static String normalizar(String texto) {
        return texto.replaceAll("\\{[^}]*\\}", "%s").replace("%d", "%s");
    }
}
