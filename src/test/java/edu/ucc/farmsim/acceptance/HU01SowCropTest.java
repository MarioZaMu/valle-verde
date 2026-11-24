package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropStage;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-01: sembrar una semilla del inventario en una parcela de tierra libre (RN-02, RN-05, RN-06, RN-15). */
class HU01SowCropTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    @Test
    void HU01_ca1_sowingPlantsTheCropInSeedStageAndConsumesOneSeedAndTwoEnergy() {
                // Dado Primavera, 10 semillas de zanahoria, 100 de energía y una parcela de tierra libre
        TestApp app = TestApp.startedGame();
        GameState state = app.state();

                // Cuando el jugador siembra zanahoria allí
        Result<PlotView> result = app.controller.plant(COL, ROW, "CARROT_SEED");

                // Entonces la parcela tiene una zanahoria en la etapa Seed con 0 días de crecimiento, quedan 9 semillas y la
        // energía es 98
        assertTrue(result.isOk(), String.valueOf(result.message()));
        Crop crop = state.farm().plotAt(COL, ROW).crop();
        assertNotNull(crop);
        assertEquals(CropType.CARROT, crop.type());
        assertEquals(CropStage.SEED, crop.stage());
        assertEquals(0, crop.daysGrown());
        assertEquals(9, state.inventory().count(ItemType.CARROT_SEED));
        assertEquals(98, state.energy().current());
        assertEquals("CARROT", result.value().crop());
        assertEquals("SEED", result.value().stage());
    }

    @Test
    void HU01_ca2_sowingOutOfSeasonIsRejectedAndNothingChanges() {
                // Dado Primavera y 5 semillas de calabaza (la calabaza solo es válida en Otoño)
        TestApp app = TestApp.startedGame();
        GameState state = app.state();
        state.inventory().add(ItemType.PUMPKIN_SEED, 5);

                // Cuando el jugador intenta sembrarlas
        Result<PlotView> result = app.controller.plant(COL, ROW, "PUMPKIN_SEED");

                // Entonces se rechaza con FUERA_DE_ESTACION, las semillas se conservan y la energía queda intacta
        assertFalse(result.isOk());
        assertEquals(ErrorCode.FUERA_DE_ESTACION, result.code());
        assertEquals("No se puede sembrar Calabaza en Primavera: solo en Otoño.", result.message());
        assertEquals(5, state.inventory().count(ItemType.PUMPKIN_SEED));
        assertEquals(100, state.energy().current());
        assertNull(state.farm().plotAt(COL, ROW).crop());
    }

    @Test
    void HU01_ca2_aCropValidInTwoSeasonsNamesBothInTheMessage() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CORN_SEED, 1);

        Result<PlotView> result = app.controller.plant(COL, ROW, "CORN_SEED");

        assertEquals("No se puede sembrar Maíz en Primavera: solo en Verano y Otoño.", result.message());
    }

    @Test
    void HU01_ca3_aPlotThatIsNotSoilOrIsAlreadyOccupiedIsRejected() {
        TestApp app = TestApp.startedGame();
        GameState state = app.state();

                // Dada una parcela de pasto, sembrar se rechaza con PARCELA_NO_CULTIVABLE
        Result<PlotView> onGrass = app.controller.plant(1, 1, "CARROT_SEED");
        assertEquals(ErrorCode.PARCELA_NO_CULTIVABLE, onGrass.code());

                // Y una parcela de tierra que ya tiene un cultivo se rechaza con PARCELA_OCUPADA
        assertTrue(app.controller.plant(COL, ROW, "CARROT_SEED").isOk());
        Result<PlotView> onCrop = app.controller.plant(COL, ROW, "CARROT_SEED");
        assertEquals(ErrorCode.PARCELA_OCUPADA, onCrop.code());

                // Y ninguno de los dos fallos gastó una semilla ni energía (solo lo hizo la primera siembra exitosa)
        assertEquals(9, state.inventory().count(ItemType.CARROT_SEED));
        assertEquals(98, state.energy().current());
    }

    @Test
    void HU01_ca3_coordinatesOutsideTheMapAreRejectedWithoutFailing() {
        TestApp app = TestApp.startedGame();

        assertEquals(ErrorCode.PARCELA_NO_CULTIVABLE, app.controller.plant(-1, 0, "CARROT_SEED").code());
        assertEquals(ErrorCode.PARCELA_NO_CULTIVABLE, app.controller.plant(16, 3, "CARROT_SEED").code());
        assertEquals(ErrorCode.PARCELA_NO_CULTIVABLE, app.controller.plant(3, 10, "CARROT_SEED").code());
    }

    @Test
    void HU01_ca4_sowingWithoutSeedsIsRejectedAndThePlotStaysFree() {
                // Dado Verano y 0 semillas de maíz
        TestApp app = TestApp.startedGame();
        GameFixtures.setDate(1, Season.SUMMER, 1);

                // Cuando el jugador intenta sembrar maíz
        Result<PlotView> result = app.controller.plant(COL, ROW, "CORN_SEED");

                // Entonces se rechaza con SIN_SEMILLAS y la parcela sigue libre
        assertEquals(ErrorCode.SIN_SEMILLAS, result.code());
        assertEquals("No tienes semillas de Maíz.", result.message());
        assertNull(app.state().farm().plotAt(COL, ROW).crop());
    }

    @Test
    void HU08_ca2_sowingWithoutEnoughEnergyIsRejectedWithSinEnergiaAndNothingIsSpent() {
        // Given 1 energy (sowing costs 2)
        TestApp app = TestApp.startedGame();
        app.state().energy().set(1);

                // Cuando el jugador intenta sembrar
        Result<PlotView> result = app.controller.plant(COL, ROW, "CARROT_SEED");

                // Entonces se rechaza: «necesitas 2 y tienes 1»; la semilla y la energía quedan intactas
        assertEquals(ErrorCode.SIN_ENERGIA, result.code());
        assertEquals("Energía insuficiente: necesitas 2 y tienes 1.", result.message());
        assertEquals(1, app.state().energy().current());
        assertEquals(10, app.state().inventory().count(ItemType.CARROT_SEED));
    }

    @Test
    void HU34_ca2_sowingPublishesTheEnergyAndMapEventsSoTheScreenUpdatesWithoutPolling() {
        TestApp app = TestApp.startedGame();

        app.controller.plant(COL, ROW, "CARROT_SEED");

        assertTrue(app.events.contains(new GameEvent.EnergyChanged(98, 100)), app.events.toString());
        assertTrue(app.events.contains(new GameEvent.MapChanged()), app.events.toString());
    }

    @Test
    void HU01_aRejectedSowingPublishesNoEvents() {
        TestApp app = TestApp.startedGame();

        app.controller.plant(1, 1, "CARROT_SEED");

        assertTrue(app.events.isEmpty());
    }

    @Test
    void HU01_withoutAGameInProgressSowingIsRejectedWithSinPartida() {
        TestApp app = TestApp.create();

        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.plant(COL, ROW, "CARROT_SEED").code());
    }

    @Test
    void HU01_theFactoryMethodCreatesEachCropTypeInSeedStage() {
        for (CropType type : CropType.values()) {
            Crop crop = edu.ucc.farmsim.domain.crop.CropFactory.forType(type).createCrop();
            assertEquals(type, crop.type());
            assertEquals(CropStage.SEED, crop.stage());
            assertEquals(0, crop.daysGrown());
        }
    }

    @Test
    void HU01_cropTypesFollowTheCatalogOfRn05() {
        assertEquals(4, CropType.CARROT.daysToMature());
        assertEquals(6, CropType.POTATO.daysToMature());
        assertEquals(7, CropType.TOMATO.daysToMature());
        assertEquals(8, CropType.CORN.daysToMature());
        assertEquals(10, CropType.PUMPKIN.daysToMature());
        assertEquals(java.util.Set.of(Season.SPRING), CropType.CARROT.validSeasons());
        assertEquals(java.util.Set.of(Season.SUMMER, Season.AUTUMN), CropType.CORN.validSeasons());
        assertEquals(ItemType.CARROT_SEED, ItemType.seedOf(CropType.CARROT));
        assertEquals(ItemType.PUMPKIN, ItemType.produceOf(CropType.PUMPKIN));
        assertEquals(CropType.TOMATO, ItemType.TOMATO_SEED.cropType());
        assertNull(ItemType.EGG.cropType());
    }
}
