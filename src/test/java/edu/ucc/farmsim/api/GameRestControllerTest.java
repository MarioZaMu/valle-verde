package edu.ucc.farmsim.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.config.ServerApplication;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.support.FakeSaveRepository;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.NoMongoTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * RNF-15 and AR-12: one integration test per endpoint with the HTTP status of the success and of each
 * rejection, against the real Spring context (without MongoDB).
 */
@SpringBootTest(classes = ServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("no-mongo")
@Import(NoMongoTestConfig.class)
class GameRestControllerTest {

    private static final String PLOT = "/api/farm/plots/" + GameFixtures.SOIL_COL + "/" + GameFixtures.SOIL_ROW;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private GameSession session;
    @Autowired
    private FakeSaveRepository saves;

    @BeforeEach
    void cleanServer() {
        GameClock.getInstance().resetForTests();
        session.replace(null);
        saves.reset();
    }

    private ResultActions newGame() throws Exception {
        return mvc.perform(post("/api/game").contentType(MediaType.APPLICATION_JSON).content("{\"seed\": 2026}"));
    }

    private ResultActions sow(String seedId) throws Exception {
        return mvc.perform(post(PLOT + "/crop").contentType(MediaType.APPLICATION_JSON)
                .content("{\"seedId\": \"" + seedId + "\"}"));
    }

    @Test
    void POST_game_createsTheGameAndReturnsItsHudAndTheGameStartedEvent() throws Exception {
        newGame().andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.money").value(500))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.maxEnergy").value(100))
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].type").value("GameStarted"))
                .andExpect(jsonPath("$.events[0].data.hud.money").value(500));
    }

    @Test
    void POST_game_withoutSeedIsABadRequest() throws Exception {
        mvc.perform(post("/api/game").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void POST_game_withABodyThatIsNotJsonIsABadRequest() throws Exception {
        mvc.perform(post("/api/game").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void GET_game_withoutAGameIsNotFoundWithTheRn35Message() throws Exception {
        mvc.perform(get("/api/game")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIN_PARTIDA"))
                .andExpect(jsonPath("$.message").value("No hay una partida en curso."));
    }

    @Test
    void GET_game_returnsTheHudOfTheGameInProgress() throws Exception {
        newGame();
        mvc.perform(get("/api/game")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.money").value(500))
                .andExpect(jsonPath("$.events", hasSize(0)));
    }

    @Test
    void POST_days_endsTheDayAndReturnsTheDayEndedEvent() throws Exception {
        newGame();
        mvc.perform(post("/api/game/days")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.events[?(@.type == 'DayEnded')]").isNotEmpty());
    }

    @Test
    void POST_days_withoutAGameIsNotFound() throws Exception {
        mvc.perform(post("/api/game/days")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIN_PARTIDA"));
    }

    @Test
    void GET_map_returnsThePlots() throws Exception {
        newGame();
        mvc.perform(get("/api/farm/map")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cols").value(16))
                .andExpect(jsonPath("$.data.rows").value(10))
                .andExpect(jsonPath("$.data.plots", hasSize(16 * 10)));
    }

    @Test
    void GET_plot_describesAPlotWithoutChangingIt() throws Exception {
        newGame();
        mvc.perform(get(PLOT)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.terrain").value("SOIL"))
                .andExpect(jsonPath("$.events", hasSize(0)));
    }

    @Test
    void POST_crop_sowsAndReturnsCreatedWithTheEventsOfTheOrder() throws Exception {
        newGame();
        sow("CARROT_SEED").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.crop").value("CARROT"))
                .andExpect(jsonPath("$.data.stage").value("SEED"))
                .andExpect(jsonPath("$.events[?(@.type == 'MapChanged')]").isNotEmpty())
                .andExpect(jsonPath("$.events[?(@.type == 'EnergyChanged')].data.energy").value(98));
    }

    @Test
    void POST_crop_outOfSeasonIsAConflictWithTheRn35Code() throws Exception {
        newGame();
        sow("PUMPKIN_SEED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FUERA_DE_ESTACION"));
    }

    @Test
    void POST_crop_inAnOccupiedPlotIsAConflict() throws Exception {
        newGame();
        sow("CARROT_SEED");
        sow("CARROT_SEED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARCELA_OCUPADA"));
    }

    @Test
    void POST_crop_withAnUnknownSeedIsAnInternalError() throws Exception {
        newGame();
        sow("NOT_A_SEED").andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"));
    }

    @Test
    void POST_crop_withoutSeedIdIsABadRequest() throws Exception {
        newGame();
        mvc.perform(post(PLOT + "/crop").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_watering_watersTheCropAndAGainTheSameDayIsAConflict() throws Exception {
        newGame();
        sow("CARROT_SEED");
        mvc.perform(post(PLOT + "/watering")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.watered").value(true));
        mvc.perform(post(PLOT + "/watering")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("YA_REGADO"));
    }

    @Test
    void POST_watering_anEmptyPlotIsNotFound() throws Exception {
        newGame();
        mvc.perform(post(PLOT + "/watering")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIN_CULTIVO"));
    }

    @Test
    void POST_harvest_aCropThatIsNotMatureIsAConflict() throws Exception {
        newGame();
        sow("CARROT_SEED");
        mvc.perform(post(PLOT + "/harvest")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_MADURO"));
    }

    @Test
    void POST_harvest_anEmptyPlotIsNotFound() throws Exception {
        newGame();
        mvc.perform(post(PLOT + "/harvest")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIN_CULTIVO"));
    }

    @Test
    void GET_savesAny_isFalseWhenNoSlotHoldsAGameAndTrueWhenOneDoes() throws Exception {
        mvc.perform(get("/api/saves/any")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(false));
        saves.withSlot(2);
        mvc.perform(get("/api/saves/any")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void GET_savesAny_whenTheStorageIsDownIsServiceUnavailableAndPlayingStillWorks() throws Exception {
        saves.unreachable();
        mvc.perform(get("/api/saves/any")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ERROR_ES"))
                .andExpect(jsonPath("$.message").value("No se pudo acceder a las partidas guardadas: MongoDB no respondió."));
        newGame().andExpect(status().isCreated());
    }

    @Test
    void anUnknownRouteIsNotFoundAndAWrongVerbIsRejectedWithTheSameBody() throws Exception {
        mvc.perform(get("/api/nothing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
        mvc.perform(get("/api/game/days")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void aPlotCoordinateThatIsNotANumberIsABadRequest() throws Exception {
        mvc.perform(get("/api/farm/plots/x/1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }
}
