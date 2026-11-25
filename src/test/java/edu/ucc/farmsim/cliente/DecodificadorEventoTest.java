package edu.ucc.farmsim.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import java.util.List;
import org.junit.jupiter.api.Test;

/** DT-19: los eventos viajan como {type, data} y regresan como los mismos records. */
class DecodificadorEventoTest {

    private final ObjectMapper mapeador = new ObjectMapper();
    private final DecodificadorEvento decodificador = new DecodificadorEvento(mapeador);

    private List<EventoJuego> decodificar(String json) throws Exception {
        return decodificador.decodificar(mapeador.readTree(json));
    }

    @Test
    void esTipoEventoCadaReconstruidoDesdeCamposSu() throws Exception {
        VistaHud hud = new VistaHud(500, 1, "SPRING", 1, "SUNNY", "SUNNY", 100, 100);
        String hudJson = mapeador.writeValueAsString(hud);

        List<EventoJuego> eventos = decodificar("[{\"tipo\":\"JuegoIniciado\",\"datos\":{\"hud\":" + hudJson + "}},"
                + "{\"tipo\":\"DiaTerminado\",\"datos\":{\"hud\":" + hudJson + "}},"
                + "{\"tipo\":\"MapaCambiado\",\"datos\":{}},"
                + "{\"tipo\":\"EnergiaCambiada\",\"datos\":{\"energia\":98,\"energiaMaxima\":100}}]");

        assertEquals(List.of(new EventoJuego.JuegoIniciado(hud), new EventoJuego.DiaTerminado(hud),
                new EventoJuego.MapaCambiado(), new EventoJuego.EnergiaCambiada(98, 100)), eventos);
    }

    @Test
    void eventoUnDeEsTipoUnDesconocidoOmitidoYSobrevivirOtrosEl() throws Exception {
        List<EventoJuego> eventos = decodificar("[{\"tipo\":\"FromTheFuture\",\"datos\":{}},{\"tipo\":\"MapaCambiado\",\"datos\":{}}]");

        assertEquals(List.of(new EventoJuego.MapaCambiado()), eventos);
    }

    @Test
    void esLeerSerNoPuedeCamposCuyoEventoUnOmitido() throws Exception {
        List<EventoJuego> eventos = decodificar("[{\"tipo\":\"EnergiaCambiada\",\"datos\":{\"energia\":\"lots\"}}]");

        assertTrue(eventos.isEmpty());
    }

    @Test
    void aMissingOrMalformedEventsFieldMeansNoEvents() throws Exception {
        assertTrue(decodificador.decodificar(null).isEmpty());
        assertTrue(decodificar("{\"not\":\"an array\"}").isEmpty());
    }
}
