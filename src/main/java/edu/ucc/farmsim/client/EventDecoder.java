package edu.ucc.farmsim.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucc.farmsim.application.event.GameEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reconstruye los {@link GameEvent} que trae una respuesta (DT-19). La clase de cada evento se halla por el nombre que
 * envió el servidor, entre los subtipos permitidos de {@link GameEvent}, así que un evento nuevo no pide ningún cambio
 * aquí.
 *
 * <p>Stories: none.
 */
final class EventDecoder {

    private static final Logger LOG = Logger.getLogger(EventDecoder.class.getName());

    private final ObjectMapper mapper;
    private final Map<String, Class<? extends GameEvent>> types = new HashMap<>();

    EventDecoder(ObjectMapper mapper) {
        this.mapper = mapper;
        for (Class<?> permitted : GameEvent.class.getPermittedSubclasses()) {
            types.put(permitted.getSimpleName(), permitted.asSubclass(GameEvent.class));
        }
    }

    /** Decodifica el arreglo {@code events} de una respuesta; un evento de tipo desconocido se omite. */
    List<GameEvent> decode(JsonNode events) {
        List<GameEvent> decoded = new ArrayList<>();
        if (events == null || !events.isArray()) {
            return decoded;
        }
        for (JsonNode node : events) {
            Class<? extends GameEvent> type = types.get(node.path("type").asText());
            if (type == null) {
                LOG.warning("Skipping an event of an unknown type: " + node.path("type").asText());
                continue;
            }
            try {
                decoded.add(mapper.treeToValue(node.path("data"), type));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                LOG.log(Level.WARNING, "Skipping an event that cannot be read", e);
            }
        }
        return decoded;
    }
}
