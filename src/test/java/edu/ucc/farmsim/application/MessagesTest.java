package edu.ucc.farmsim.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Messages;
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

/** Q-1: every error code has its Spanish message, and the messages match the RN-35 table. */
class MessagesTest {

    private static final Path RULES = Path.of("docs/REGLAS_DE_NEGOCIO.md");
    private static final Pattern ROW = Pattern.compile("^\\| (`[A-Z_`/ ]+`) \\| (.+) \\|$");
    private static final Pattern CODE = Pattern.compile("`([A-Z_]+)`");

    @Test
    void Q1_everyErrorCodeHasASpanishMessageTemplate() {
        for (ErrorCode code : ErrorCode.values()) {
            String template = Messages.template(code);
            assertFalse(template.isBlank(), "missing message for " + code);
        }
    }

    @Test
    void Q1_messagesFormatTheirArguments() {
        assertEquals("Fondos insuficientes: faltan 20 m.", Messages.of(ErrorCode.SIN_FONDOS, 20));
        assertEquals("No hay una partida en curso.", Messages.of(ErrorCode.SIN_PARTIDA));
    }

    @Test
    void Q1_everyTemplateMatchesTheRn35Table() throws IOException {
        Map<String, String> fromDocs = readRn35();
        for (ErrorCode code : ErrorCode.values()) {
            assertTrue(fromDocs.containsKey(code.name()), code + " is missing from RN-35 in the business rules");
            assertEquals(normalize(fromDocs.get(code.name())), normalize(Messages.template(code)),
                    "message of " + code + " differs from RN-35");
        }
        for (String documented : fromDocs.keySet()) {
            ErrorCode.valueOf(documented); // every documented code must exist
        }
    }

    private static Map<String, String> readRn35() throws IOException {
        Map<String, String> result = new TreeMap<>();
        boolean inSection = false;
        for (String line : Files.readAllLines(RULES, StandardCharsets.UTF_8)) {
            if (line.startsWith("### RN-35")) {
                inSection = true;
                continue;
            }
            if (inSection && line.startsWith("## ")) {
                break;
            }
            Matcher row = ROW.matcher(line);
            if (!inSection || !row.matches()) {
                continue;
            }
            List<String> codes = new ArrayList<>();
            Matcher codeMatcher = CODE.matcher(row.group(1));
            while (codeMatcher.find()) {
                codes.add(codeMatcher.group(1));
            }
            String cell = row.group(2);
            List<String> messages = new ArrayList<>();
            if (codes.size() == 2) {
                for (String part : cell.split("» / «")) {
                    messages.add(part.replaceFirst("^«", "").replaceFirst("»$", ""));
                }
            } else {
                messages.add(cell.substring(cell.indexOf('«') + 1, cell.lastIndexOf('»')));
            }
            for (int i = 0; i < codes.size(); i++) {
                result.put(codes.get(i), messages.get(i));
            }
        }
        return result;
    }

    /** Placeholders differ between the document ({n}, {cultivo}) and the code (%d, %s): compare their shape. */
    private static String normalize(String text) {
        return text.replaceAll("\\{[^}]*\\}", "%s").replace("%d", "%s");
    }
}
