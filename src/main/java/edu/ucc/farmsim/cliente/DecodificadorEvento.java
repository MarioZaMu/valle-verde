package edu.ucc.farmsim.cliente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reconstruye los {@link EventoJuego} que trae una respuesta (DT-19). La clase de cada evento se halla por el nombre que
 * envió el servidor, entre los subtipos permitidos de {@link EventoJuego}, así que un evento nuevo no pide ningún cambio
 * aquí.
 *
 * <p>Stories: none.
 */
final class DecodificadorEvento {

    private static final Logger BITACORA = Logger.getLogger(DecodificadorEvento.class.getName());

    private final ObjectMapper mapeador;
    private final Map<String, Class<? extends EventoJuego>> tipos = new HashMap<>();

    DecodificadorEvento(ObjectMapper mapeador) {
        this.mapeador = mapeador;
        for (Class<?> permitido : EventoJuego.class.getPermittedSubclasses()) {
            tipos.put(permitido.getSimpleName(), permitido.asSubclass(EventoJuego.class));
        }
    }

    /** Decodifica el arreglo {@code eventos} de una respuesta; un evento de tipo desconocido se omite. */
    List<EventoJuego> decodificar(JsonNode eventos) {
        List<EventoJuego> decodificado = new ArrayList<>();
        if (eventos == null || !eventos.isArray()) {
            return decodificado;
        }
        for (JsonNode nodo : eventos) {
            Class<? extends EventoJuego> tipo = tipos.get(nodo.path("tipo").asText());
            if (tipo == null) {
                BITACORA.warning("Skipping an event of an unknown type: " + nodo.path("tipo").asText());
                continue;
            }
            try {
                decodificado.add(mapeador.treeToValue(nodo.path("datos"), tipo));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                BITACORA.log(Level.WARNING, "Skipping an event that cannot be read", e);
            }
        }
        return decodificado;
    }
}
