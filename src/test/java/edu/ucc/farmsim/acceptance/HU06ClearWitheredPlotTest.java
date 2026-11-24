package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/**
 * HU-06: limpiar una parcela marchita con la herramienta de cosechar para poder sembrarla de nuevo (RN-02, RN-10,
 * RN-34).
 */
class HU06ClearWitheredPlotTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    private static TestApp gameWithAWitheredCrop() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 3, false, true);
        return app;
    }

    @Test
    void HU06_ca1_clearingFreesThePlotCostsOneEnergyAndGivesNothing() {
                // Dado un cultivo marchito y 100 de energía
        TestApp app = gameWithAWitheredCrop();

                // Cuando el jugador usa la herramienta de cosechar sobre él
        Result<PlotView> result = app.controller.harvest(COL, ROW);

                // Entonces la parcela queda libre, la energía es 99 y el inventario no tiene zanahoria
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertNull(app.state().farm().plotAt(COL, ROW).crop());
        assertEquals(99, app.state().energy().current());
        assertEquals(0, app.state().inventory().count(ItemType.CARROT));
        assertEquals(10, app.state().inventory().count(ItemType.CARROT_SEED));
    }

    @Test
    void HU06_ca2_clearingWithoutEnergyIsRejectedAndTheCropStaysWithered() {
        TestApp app = gameWithAWitheredCrop();
        app.state().energy().set(0);

        Result<PlotView> result = app.controller.harvest(COL, ROW);

        assertEquals(ErrorCode.SIN_ENERGIA, result.code());
        assertEquals("Energía insuficiente: necesitas 1 y tienes 0.", result.message());
        assertNotNull(app.state().farm().plotAt(COL, ROW).crop());
    }

    @Test
    void HU06_ca3_usingTheHarvestToolOnAFreePlotIsRejectedWithSinCultivo() {
        TestApp app = TestApp.startedGame();

        assertEquals(ErrorCode.SIN_CULTIVO, app.controller.harvest(COL, ROW).code());
    }

    @Test
    void HU06_ca4_aClearedPlotCanBeSownAgain() {
        TestApp app = gameWithAWitheredCrop();
        assertTrue(app.controller.harvest(COL, ROW).isOk());

        Result<PlotView> sowing = app.controller.plant(COL, ROW, "CARROT_SEED");

        assertTrue(sowing.isOk(), String.valueOf(sowing.message()));
        assertEquals("SEED", sowing.value().stage());
    }

    @Test
    void HU06_clearingPublishesTheMapAndEnergyEvents() {
        TestApp app = gameWithAWitheredCrop();

        app.controller.harvest(COL, ROW);

        assertTrue(app.events.contains(new GameEvent.MapChanged()));
        assertTrue(app.events.contains(new GameEvent.EnergyChanged(99, 100)));
    }

    @Test
    void HU06_aFullInventoryDoesNotPreventClearingBecauseNothingIsCollected() {
        TestApp app = gameWithAWitheredCrop();
        GameFixtures.fillInventoryExcept(app.state(), ItemType.HAY, 99);

        assertTrue(app.controller.harvest(COL, ROW).isOk());
    }
}
