package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.time.Weather;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-02: regar un cultivo vivo para que crezca ese día (RN-02, RN-07, RN-12). */
class HU02WaterCropTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    private static TestApp gameWithACrop() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 0);
        return app;
    }

    @Test
    void HU02_ca1_wateringMarksTheCropAsWateredAndCostsOneEnergy() {
                // Dado un cultivo vivo sin regar, clima Sunny y 100 de energía
        TestApp app = gameWithACrop();
        assertEquals(Weather.SUNNY, app.state().weather().today());

                // Cuando el jugador lo riega
        Result<PlotView> result = app.controller.water(COL, ROW);

                // Entonces queda regado hasta el final del día y la energía es 99
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertTrue(app.state().farm().plotAt(COL, ROW).crop().isWatered());
        assertTrue(result.value().watered());
        assertEquals(99, app.state().energy().current());
    }

    @Test
    void HU02_ca2_wateringInDroughtCostsTwoEnergy() {
                // Dado que el clima del día es Drought
        TestApp app = gameWithACrop();
        app.state().weather().restore(Weather.DROUGHT, Weather.SUNNY);

                // Cuando el jugador riega
        Result<PlotView> result = app.controller.water(COL, ROW);

                // Entonces la energía es 98
        assertTrue(result.isOk());
        assertEquals(98, app.state().energy().current());
    }

    @Test
    void HU02_ca3_wateringTwiceIsRejectedWithoutSpendingEnergy() {
        TestApp app = gameWithACrop();
        assertTrue(app.controller.water(COL, ROW).isOk());
        assertEquals(99, app.state().energy().current());

        Result<PlotView> second = app.controller.water(COL, ROW);

        assertFalse(second.isOk());
        assertEquals(ErrorCode.YA_REGADO, second.code());
        assertEquals("El cultivo ya está regado hoy.", second.message());
        assertEquals(99, app.state().energy().current());
    }

    @Test
    void HU02_ca4_wateringAPlotWithoutCropOrWithAWitheredCropIsRejected() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 3, false, true);

        // A free plot: SIN_CULTIVO
        Result<PlotView> empty = app.controller.water(COL + 1, ROW);
        assertEquals(ErrorCode.SIN_CULTIVO, empty.code());

        // A withered crop: rejected without changes
        Result<PlotView> withered = app.controller.water(COL, ROW);
        assertEquals(ErrorCode.CULTIVO_MARCHITO, withered.code());
        assertFalse(app.state().farm().plotAt(COL, ROW).crop().isWatered());
        assertEquals(100, app.state().energy().current());
    }

    @Test
    void HU02_wateringWithoutEnoughEnergyIsRejectedAndTheCropStaysDry() {
        TestApp app = gameWithACrop();
        app.state().energy().set(0);

        Result<PlotView> result = app.controller.water(COL, ROW);

        assertEquals(ErrorCode.SIN_ENERGIA, result.code());
        assertEquals("Energía insuficiente: necesitas 1 y tienes 0.", result.message());
        assertFalse(app.state().farm().plotAt(COL, ROW).crop().isWatered());
    }

    @Test
    void HU02_wateringInDroughtWithOneEnergyIsRejectedBecauseItNeedsTwo() {
        TestApp app = gameWithACrop();
        app.state().weather().restore(Weather.DROUGHT, Weather.SUNNY);
        app.state().energy().set(1);

        Result<PlotView> result = app.controller.water(COL, ROW);

        assertEquals("Energía insuficiente: necesitas 2 y tienes 1.", result.message());
    }

    @Test
    void HU02_aSuccessfulWateringPublishesMapAndEnergyEventsAndARejectionPublishesNothing() {
        TestApp app = gameWithACrop();

        app.controller.water(COL, ROW);
        assertTrue(app.events.contains(new GameEvent.MapChanged()));
        assertTrue(app.events.contains(new GameEvent.EnergyChanged(99, 100)));

        app.events.clear();
        app.controller.water(COL, ROW); // already watered
        assertTrue(app.events.isEmpty());
    }

    @Test
    void HU02_withoutAGameInProgressWateringIsRejectedWithSinPartida() {
        assertEquals(ErrorCode.SIN_PARTIDA, TestApp.create().controller.water(COL, ROW).code());
    }

    @Test
    void HU02_coordinatesOutsideTheMapAreRejectedAsNoCrop() {
        TestApp app = TestApp.startedGame();

        assertEquals(ErrorCode.SIN_CULTIVO, app.controller.water(-1, -1).code());
        assertEquals(ErrorCode.SIN_CULTIVO, app.controller.water(99, 3).code());
    }
}
