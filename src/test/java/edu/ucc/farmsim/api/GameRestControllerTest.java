package edu.ucc.farmsim.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.config.ServerApplication;
import edu.ucc.farmsim.domain.economy.ItemType;
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

    private ResultActions trade(String route, String itemId, int quantity) throws Exception {
        return mvc.perform(post("/api/market/" + route).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\": \"" + itemId + "\", \"quantity\": " + quantity + "}"));
    }

    @Test
    void GET_market_listsWhatCanBeSoldWithTheirTrendAndWhatCanBeBought() throws Exception {
        newGame();
        mvc.perform(get("/api/market")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sells", hasSize(7)))
                .andExpect(jsonPath("$.data.sells[?(@.itemId == 'CARROT')].unitPrice").value(27))
                .andExpect(jsonPath("$.data.sells[?(@.itemId == 'CARROT')].trend").value("SAME"))
                .andExpect(jsonPath("$.data.buys", hasSize(7)));
    }

    @Test
    void GET_market_withoutAGameIsNotFound() throws Exception {
        mvc.perform(get("/api/market")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIN_PARTIDA"));
    }

    @Test
    void POST_purchases_buysAndReportsTheMoneyAndInventoryEvents() throws Exception {
        newGame();
        trade("purchases", "CORN_SEED", 5).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(150))
                .andExpect(jsonPath("$.events[?(@.type == 'MoneyChanged')].data.money").value(350))
                .andExpect(jsonPath("$.events[?(@.type == 'InventoryChanged')]").isNotEmpty());
    }

    @Test
    void POST_purchases_withoutEnoughMoneyIsAConflict() throws Exception {
        newGame();
        trade("purchases", "MEDICINE", 99).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIN_FONDOS"));
    }

    @Test
    void POST_purchases_withAQuantityOutOfRangeIsABadRequest() throws Exception {
        newGame();
        trade("purchases", "HAY", 100).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANTIDAD_INVALIDA"));
    }

    @Test
    void POST_sales_sellsAtTheDynamicPriceAndQuotesItBeforehand() throws Exception {
        newGame();
        session.current().inventory().add(ItemType.CARROT, 3);

        mvc.perform(get("/api/market/quotes").param("itemId", "CARROT").param("quantity", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(79));
        trade("sales", "CARROT", 3).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(79))
                .andExpect(jsonPath("$.events[?(@.type == 'MoneyChanged')].data.money").value(579));
    }

    @Test
    void POST_sales_moreThanOwnedIsABadRequestWithTheRange() throws Exception {
        newGame();
        session.current().inventory().add(ItemType.CARROT, 2);

        trade("sales", "CARROT", 5).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANTIDAD_INVALIDA"))
                .andExpect(jsonPath("$.message").value("La cantidad debe estar entre 1 y 2."));
    }

    @Test
    void POST_sales_withoutBodyFieldsIsABadRequest() throws Exception {
        newGame();
        mvc.perform(post("/api/market/sales").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void GET_inventory_returnsTheSlotsOfTheGrid() throws Exception {
        newGame();
        mvc.perform(get("/api/inventory")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(24))
                .andExpect(jsonPath("$.data.slots", hasSize(2)))
                .andExpect(jsonPath("$.data.slots[0].itemId").value("CARROT_SEED"))
                .andExpect(jsonPath("$.data.slots[0].amount").value(10));
    }

    private ResultActions build(String buildingId, int col, int row) throws Exception {
        return mvc.perform(post("/api/buildings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"buildingId\": \"" + buildingId + "\", \"col\": " + col + ", \"row\": " + row + "}"));
    }

    private ResultActions buyAnimal(String animalId) throws Exception {
        return mvc.perform(post("/api/animals").contentType(MediaType.APPLICATION_JSON)
                .content("{\"animalId\": \"" + animalId + "\"}"));
    }

    @Test
    void POST_buildings_buildsAndReturnsCreatedWithTheEventsOfTheOrder() throws Exception {
        newGame();
        build("COOP", 2, 7).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Gallinero"))
                .andExpect(jsonPath("$.data.capacity").value(4))
                .andExpect(jsonPath("$.events[?(@.type == 'MoneyChanged')].data.money").value(org.hamcrest.Matchers.contains(100, 200)))
                .andExpect(jsonPath("$.events[?(@.type == 'AchievementUnlocked')].data.code").value("LG-06"))
                .andExpect(jsonPath("$.events[?(@.type == 'MapChanged')]").isNotEmpty());
    }

    @Test
    void POST_buildings_rejectionsKeepTheirRn35CodesAndStatus() throws Exception {
        newGame();
        build("COOP", 3, 2).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("UBICACION_INVALIDA"));
        build("BARN", 2, 7).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EDIFICIO_BLOQUEADO"))
                .andExpect(jsonPath("$.message").value("Edificio bloqueado: requiere el logro «Manos a la tierra»."));
        build("COOP", 2, 7);
        build("COOP", 8, 7).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIN_FONDOS"));
    }

    @Test
    void POST_buildings_withoutBodyFieldsIsABadRequestAndWithoutAGameIsNotFound() throws Exception {
        mvc.perform(post("/api/buildings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
        build("COOP", 2, 7).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SIN_PARTIDA"));
    }

    @Test
    void GET_buildings_listsWhatIsBuiltAndTheCatalogue() throws Exception {
        newGame();
        session.current().farm().place(edu.ucc.farmsim.domain.building.BuildingType.COOP, 2, 7);
        mvc.perform(get("/api/buildings")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buildings", hasSize(1)))
                .andExpect(jsonPath("$.data.catalog", hasSize(4)))
                .andExpect(jsonPath("$.data.catalog[1].unlocked").value(false))
                .andExpect(jsonPath("$.data.catalog[1].requirement").value("Manos a la tierra"));
    }

    @Test
    void POST_animals_buysAHenAndReturnsTheBuildingThatHousesIt() throws Exception {
        newGame();
        build("COOP", 2, 7);
        session.current().earn(2000);
        buyAnimal("HEN").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.animals", hasSize(1)))
                .andExpect(jsonPath("$.data.animals[0].condition").value("HEALTHY"));
        buyAnimal("COW").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIN_ESPACIO_ANIMALES"));
    }

    @Test
    void POST_feeding_collection_and_treatment_workOnTheBuildingAndItsAnimals() throws Exception {
        newGame();
        build("COOP", 2, 7);
        var coop = session.current().farm().buildings().get(0);
        coop.addAnimal(new edu.ucc.farmsim.domain.animal.Animal(edu.ucc.farmsim.domain.animal.AnimalType.HEN));
        coop.addAnimal(edu.ucc.farmsim.domain.animal.Animal.fromSnapshot(new edu.ucc.farmsim.domain.animal.AnimalSnapshot(
                edu.ucc.farmsim.domain.animal.AnimalType.HEN, edu.ucc.farmsim.domain.animal.AnimalCondition.SICK, 3, false, 0)));
        session.current().inventory().add(ItemType.HAY, 2);
        session.current().inventory().add(ItemType.MEDICINE, 1);

        mvc.perform(post("/api/buildings/0/feeding")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(1))
                .andExpect(jsonPath("$.data.notice").value("Hay animales enfermos: cúralos con Medicina."));
        mvc.perform(post("/api/buildings/0/animals/1/treatment")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.building.animals[1].condition").value("HEALTHY"));
        mvc.perform(post("/api/buildings/0/collection")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(0))
                .andExpect(jsonPath("$.data.notice").value("No hay productos que recoger."));
    }

    @Test
    void POST_feeding_rejectionsAreConflictsAndAnUnknownBuildingIsAnInternalError() throws Exception {
        newGame();
        build("COOP", 2, 7);
        session.current().farm().buildings().get(0).addAnimal(
                new edu.ucc.farmsim.domain.animal.Animal(edu.ucc.farmsim.domain.animal.AnimalType.HEN));

        mvc.perform(post("/api/buildings/0/feeding")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIN_HENO"));
        mvc.perform(post("/api/buildings/9/feeding")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"));
        mvc.perform(post("/api/buildings/0/animals/0/treatment")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notice").value("Este animal no está enfermo."));
    }

    @Test
    void POST_days_alsoReturnsTheDaySummaryAsAnEvent() throws Exception {
        newGame();
        mvc.perform(post("/api/game/days")).andExpect(status().isOk())
                .andExpect(jsonPath("$.events[?(@.type == 'DaySummaryReady')].data.summary.highlights[0]")
                        .value("Sin novedades"));
    }

    @Test
    void GET_market_includesTheAnimalsForSale() throws Exception {
        newGame();
        mvc.perform(get("/api/market")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.animals", hasSize(2)))
                .andExpect(jsonPath("$.data.animals[1].name").value("Vaca"))
                .andExpect(jsonPath("$.data.animals[1].price").value(800));
    }

    @Test
    void GET_achievements_listsTheNineWithTheirProgress() throws Exception {
        newGame();
        mvc.perform(get("/api/achievements")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(9)))
                .andExpect(jsonPath("$.data[0].code").value("LG-01"))
                .andExpect(jsonPath("$.data[0].unlocked").value(false))
                .andExpect(jsonPath("$.data[1].target").value(25));
    }

    @Test
    void PUT_saves_savesAndGET_saves_listsTheSlotsAndPOST_load_loadsIt() throws Exception {
        newGame();
        mvc.perform(put("/api/saves/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slot").value(2))
                .andExpect(jsonPath("$.data.status").value("OK"));
        mvc.perform(post("/api/game/days"));
        mvc.perform(get("/api/saves")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].status").value("EMPTY"))
                .andExpect(jsonPath("$.data[1].status").value("OK"));
        mvc.perform(post("/api/saves/2/load")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.events[?(@.type == 'GameStarted')]").isNotEmpty());
    }

    @Test
    void POST_load_ofAnEmptyDamagedOrUnreachableSlotKeepsItsRn35CodeAndStatus() throws Exception {
        newGame();
        mvc.perform(post("/api/saves/3/load")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARCHIVO_INEXISTENTE"))
                .andExpect(jsonPath("$.message").value("No existe una partida en la ranura 3."));
        saves.withDamagedSlot(1);
        mvc.perform(post("/api/saves/1/load")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ARCHIVO_CORRUPTO"));
        saves.unreachable();
        mvc.perform(put("/api/saves/1")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ERROR_ES"));
        mvc.perform(get("/api/saves")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void PUT_saves_withoutAGameIsNotFoundAndASlotOutOfRangeIsAnInternalError() throws Exception {
        mvc.perform(put("/api/saves/1")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIN_PARTIDA"));
        newGame();
        mvc.perform(put("/api/saves/7")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"));
    }
}
