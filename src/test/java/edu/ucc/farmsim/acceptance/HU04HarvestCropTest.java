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

/** HU-04: cosechar un cultivo maduro hacia el inventario (RN-02, RN-10, RN-15). */
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
                // Dado una zanahoria madura, lugar en el inventario y 100 de energía
        TestApp app = gameWithAMatureCarrot();

                // Cuando el jugador la cosecha
        Result<PlotView> result = app.controller.harvest(COL, ROW);

                // Entonces el jugador recibe 1 zanahoria, la parcela queda libre y la energía es 98
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(1, app.state().inventory().count(ItemType.CARROT));
        assertNull(app.state().farm().plotAt(COL, ROW).crop());
        assertEquals(98, app.state().energy().current());
        assertNull(result.value().crop());
    }

    @Test
    void HU04_ca2_harvestingWithAFullInventoryIsRejectedAndTheCropStaysMature() {
                // Dado que cada casilla del inventario tiene una pila llena de otra cosa (prueba crítica 2)
        TestApp app = gameWithAMatureCarrot();
        app.state().inventory().restoreFrom(new edu.ucc.farmsim.domain.economy.InventorySnapshot(24,
                java.util.Collections.nCopies(24, new edu.ucc.farmsim.domain.economy.Inventory.ItemStack(ItemType.HAY, 99))));

                // Cuando el jugador intenta cosechar
        Result<PlotView> result = app.controller.harvest(COL, ROW);

                // Entonces se rechaza, el cultivo sigue maduro y no se gastó energía
        assertFalse(result.isOk());
        assertEquals(ErrorCode.INVENTARIO_LLENO, result.code());
        assertEquals("Inventario lleno: libera espacio o construye un Almacén.", result.message());
        assertNotNull(app.state().farm().plotAt(COL, ROW).crop());
        assertEquals(CropStage.MATURE, app.state().farm().plotAt(COL, ROW).crop().stage());
        assertEquals(100, app.state().energy().current());
    }

    @Test
    void HU04_ca3_harvestingAnImmatureCropIsRejectedSayingHowManyDaysAreMissing() {
                // Dada una zanahoria en etapa Sprout con d = 1 y T = 4
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 1);

                // Cuando el jugador intenta cosechar
        Result<PlotView> result = app.controller.harvest(COL, ROW);

                // Entonces se rechaza: faltan 3 días, y no se gastó energía
        assertEquals(ErrorCode.NO_MADURO, result.code());
        assertEquals("El cultivo aún no está maduro: faltan 3 días.", result.message());
        assertEquals(100, app.state().energy().current());
    }

    @Test
    void HU04_ca4_harvestCompletesAStackOfNinetyEightAndIsRejectedWhenItIsFull() {
                // Dada una pila de 98 zanahorias y todas las demás casillas llenas
        TestApp app = gameWithAMatureCarrot();
        GameFixtures.fillInventoryExcept(app.state(), ItemType.CARROT, 98);

                // Cuando se cosecha una zanahoria, la pila llega a 99
        assertTrue(app.controller.harvest(COL, ROW).isOk());
        assertEquals(99, app.state().inventory().count(ItemType.CARROT));

                // Y con una pila llena de 99 la siguiente cosecha se rechaza
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
