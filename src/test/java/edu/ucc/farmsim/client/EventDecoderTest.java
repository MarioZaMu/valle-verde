package edu.ucc.farmsim.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.GameEvent;
import java.util.List;
import org.junit.jupiter.api.Test;

/** DT-19: los eventos viajan como {type, data} y regresan como los mismos records. */
class EventDecoderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final EventDecoder decoder = new EventDecoder(mapper);

    private List<GameEvent> decode(String json) throws Exception {
        return decoder.decode(mapper.readTree(json));
    }

    @Test
    void everyEventTypeIsRebuiltFromItsFields() throws Exception {
        HudView hud = new HudView(500, 1, "SPRING", 1, "SUNNY", "SUNNY", 100, 100);
        String hudJson = mapper.writeValueAsString(hud);

        List<GameEvent> events = decode("[{\"type\":\"GameStarted\",\"data\":{\"hud\":" + hudJson + "}},"
                + "{\"type\":\"DayEnded\",\"data\":{\"hud\":" + hudJson + "}},"
                + "{\"type\":\"MapChanged\",\"data\":{}},"
                + "{\"type\":\"EnergyChanged\",\"data\":{\"energy\":98,\"maxEnergy\":100}}]");

        assertEquals(List.of(new GameEvent.GameStarted(hud), new GameEvent.DayEnded(hud),
                new GameEvent.MapChanged(), new GameEvent.EnergyChanged(98, 100)), events);
    }

    @Test
    void anEventOfAnUnknownTypeIsSkippedAndTheOthersSurvive() throws Exception {
        List<GameEvent> events = decode("[{\"type\":\"FromTheFuture\",\"data\":{}},{\"type\":\"MapChanged\",\"data\":{}}]");

        assertEquals(List.of(new GameEvent.MapChanged()), events);
    }

    @Test
    void anEventWhoseFieldsCannotBeReadIsSkipped() throws Exception {
        List<GameEvent> events = decode("[{\"type\":\"EnergyChanged\",\"data\":{\"energy\":\"lots\"}}]");

        assertTrue(events.isEmpty());
    }

    @Test
    void aMissingOrMalformedEventsFieldMeansNoEvents() throws Exception {
        assertTrue(decoder.decode(null).isEmpty());
        assertTrue(decode("{\"not\":\"an array\"}").isEmpty());
    }
}
