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
import edu.ucc.farmsim.domain.crop.CropStage;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-04: harvest a mature crop into the inventory (RN-02, RN-10, RN-15). */
class HU04HarvestCropTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    private static TestApp gameWithAMatureCarrot() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 4);
        return app;
    }

    @Test
    void HU04_ca1_harvestingAMatureCropGivesOneUnitFreesThePlotAndCostsTwoEnergy() {
        // Given a mature carrot, room in the inventory and 100 energy
        TestApp app = gameWithAMatureCarrot();

        // When the player harvests it
        Result<PlotView> result = app.controller.harvest(COL, ROW);

        // Then the player receives 1 carrot, the plot is free and energy is 98
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(1, app.state().inventory().count(ItemType.CARROT));
        assertNull(app.state().farm().plotAt(COL, ROW).crop());
        assertEquals(98, app.state().energy().current());
        assertNull(result.value().crop());
    }

    @Test
    void HU04_ca2_harvestingWithAFullInventoryIsRejectedAndTheCropStaysMature() {
        // Given every inventory slot holds a full stack of something else (critical test 2)
        TestApp app = gameWithAMatureCarrot();
        app.state().inventory().restoreFrom(new edu.ucc.farmsim.domain.economy.InventorySnapshot(24,
                java.util.Collections.nCopies(24, new edu.ucc.farmsim.domain.economy.Inventory.ItemStack(ItemType.HAY, 99))));

        // When the player tries to harvest
        Result<PlotView> result = app.controller.harvest(COL, ROW);

        // Then it is rejected, the crop is still mature and no energy was spent
        assertFalse(result.isOk());
        assertEquals(ErrorCode.INVENTARIO_LLENO, result.code());
        assertEquals("Inventario lleno: libera espacio o construye un Almacén.", result.message());
        assertNotNull(app.state().farm().plotAt(COL, ROW).crop());
        assertEquals(CropStage.MATURE, app.state().farm().plotAt(COL, ROW).crop().stage());
        assertEquals(100, app.state().energy().current());
    }

    @Test
    void HU04_ca3_harvestingAnImmatureCropIsRejectedSayingHowManyDaysAreMissing() {
        // Given a carrot in Sprout stage with d = 1 and T = 4
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 1);

        // When the player tries to harvest
        Result<PlotView> result = app.controller.harvest(COL, ROW);

        // Then it is rejected: 3 days are missing, and no energy was spent
        assertEquals(ErrorCode.NO_MADURO, result.code());
        assertEquals("El cultivo aún no está maduro: faltan 3 días.", result.message());
        assertEquals(100, app.state().energy().current());
    }

    @Test
    void HU04_ca4_harvestCompletesAStackOfNinetyEightAndIsRejectedWhenItIsFull() {
        // Given a stack of 98 carrots and every other slot full
        TestApp app = gameWithAMatureCarrot();
        GameFixtures.fillInventoryExcept(app.state(), ItemType.CARROT, 98);

        // When a carrot is harvested, the stack reaches 99
        assertTrue(app.controller.harvest(COL, ROW).isOk());
        assertEquals(99, app.state().inventory().count(ItemType.CARROT));

        // And with a full stack of 99 the next harvest is rejected
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 4);
        Result<PlotView> second = app.controller.harvest(COL, ROW);
        assertEquals(ErrorCode.INVENTARIO_LLENO, second.code());
        assertEquals(99, app.state().inventory().count(ItemType.CARROT));
    }

    @Test
    void HU04_harvestingWithoutEnoughEnergyIsRejectedAndTheCropIsKept() {
        TestApp app = gameWithAMatureCarrot();
        app.state().energy().set(1);

        Result<PlotView> result = app.controller.harvest(COL, ROW);

        assertEquals(ErrorCode.SIN_ENERGIA, result.code());
        assertEquals("Energía insuficiente: necesitas 2 y tienes 1.", result.message());
        assertNotNull(app.state().farm().plotAt(COL, ROW).crop());
        assertEquals(0, app.state().inventory().count(ItemType.CARROT));
    }

    @Test
    void HU04_harvestingAPlotWithoutACropIsRejectedWithSinCultivo() {
        TestApp app = TestApp.startedGame();

        assertEquals(ErrorCode.SIN_CULTIVO, app.controller.harvest(COL, ROW).code());
        assertEquals(ErrorCode.SIN_CULTIVO, app.controller.harvest(-3, 99).code());
    }

    @Test
    void HU04_aSuccessfulHarvestPublishesEventsAndARejectionPublishesNone() {
        TestApp app = gameWithAMatureCarrot();

        app.controller.harvest(COL, ROW);
        assertTrue(app.events.contains(new GameEvent.MapChanged()));
        assertTrue(app.events.contains(new GameEvent.EnergyChanged(98, 100)));

        app.events.clear();
        app.controller.harvest(COL, ROW); // now empty
        assertTrue(app.events.isEmpty());
    }

    @Test
    void HU04_aCropGrownFromSeedCanBeHarvestedAfterFourWateredDays() {
        TestApp app = TestApp.startedGame();
        assertTrue(app.controller.plant(COL, ROW, "CARROT_SEED").isOk());
        for (int day = 1; day <= 4; day++) {
            app.controller.water(COL, ROW);
            app.controller.endDay();
        }

        assertTrue(app.controller.harvest(COL, ROW).isOk());
        assertEquals(1, app.state().inventory().count(ItemType.CARROT));
    }

    @Test
    void HU04_withoutAGameInProgressHarvestingIsRejectedWithSinPartida() {
        assertEquals(ErrorCode.SIN_PARTIDA, TestApp.create().controller.harvest(COL, ROW).code());
    }
}
