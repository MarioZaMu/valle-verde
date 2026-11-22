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

/** HU-01: sow a seed from the inventory in a free soil plot (RN-02, RN-05, RN-06, RN-15). */
class HU01SowCropTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    @Test
    void HU01_ca1_sowingPlantsTheCropInSeedStageAndConsumesOneSeedAndTwoEnergy() {
        // Given Spring, 10 carrot seeds, 100 energy and a free soil plot
        TestApp app = TestApp.startedGame();
        GameState state = app.state();

        // When the player sows carrot there
        Result<PlotView> result = app.controller.plant(COL, ROW, "CARROT_SEED");

        // Then the plot holds a carrot in the Seed stage with 0 days grown, 9 seeds remain and energy is 98
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
        // Given Spring and 5 pumpkin seeds (pumpkin is only valid in Autumn)
        TestApp app = TestApp.startedGame();
        GameState state = app.state();
        state.inventory().add(ItemType.PUMPKIN_SEED, 5);

        // When the player tries to sow them
        Result<PlotView> result = app.controller.plant(COL, ROW, "PUMPKIN_SEED");

        // Then it is rejected with FUERA_DE_ESTACION, the seeds are kept and energy is untouched
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

        // Given a grass plot, sowing is rejected with PARCELA_NO_CULTIVABLE
        Result<PlotView> onGrass = app.controller.plant(1, 1, "CARROT_SEED");
        assertEquals(ErrorCode.PARCELA_NO_CULTIVABLE, onGrass.code());

        // And a soil plot that already has a crop is rejected with PARCELA_OCUPADA
        assertTrue(app.controller.plant(COL, ROW, "CARROT_SEED").isOk());
        Result<PlotView> onCrop = app.controller.plant(COL, ROW, "CARROT_SEED");
        assertEquals(ErrorCode.PARCELA_OCUPADA, onCrop.code());

        // And neither failure spent a seed or energy (only the first successful sowing did)
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
        // Given Summer and 0 corn seeds
        TestApp app = TestApp.startedGame();
        GameFixtures.setDate(1, Season.SUMMER, 1);

        // When the player tries to sow corn
        Result<PlotView> result = app.controller.plant(COL, ROW, "CORN_SEED");

        // Then it is rejected with SIN_SEMILLAS and the plot is still free
        assertEquals(ErrorCode.SIN_SEMILLAS, result.code());
        assertEquals("No tienes semillas de Maíz.", result.message());
        assertNull(app.state().farm().plotAt(COL, ROW).crop());
    }

    @Test
    void HU08_ca2_sowingWithoutEnoughEnergyIsRejectedWithSinEnergiaAndNothingIsSpent() {
        // Given 1 energy (sowing costs 2)
        TestApp app = TestApp.startedGame();
        app.state().energy().set(1);

        // When the player tries to sow
        Result<PlotView> result = app.controller.plant(COL, ROW, "CARROT_SEED");

        // Then it is rejected: "you need 2 and have 1"; the seed and the energy are untouched
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
