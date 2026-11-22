package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-24: building a warehouse to widen the inventory (RN-15, RN-20, RN-29). */
class HU24WarehouseTest {

    @Test
    void HU24_ca1_theWarehouseCosts300AndWidensTheInventoryFrom24To36() {
        TestApp app = TestApp.startedGame();
        GameFixtures.unlock(app.state(), Achievement.FIRST_HARVEST);
        assertEquals(24, app.state().inventory().capacity());

        Result<BuildingView> result = app.controller.build("WAREHOUSE", 2, 7);

        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(300, app.state().money()); // 500 - 300 + 100 of LG-06
        assertEquals(36, app.state().inventory().capacity());
        assertEquals(36, app.controller.inventory().value().capacity());
    }

    @Test
    void HU24_ca2_aSecondWarehouseIsRejectedByTheLimit() {
        TestApp app = TestApp.startedGame();
        GameFixtures.unlock(app.state(), Achievement.FIRST_HARVEST);
        GameFixtures.build(app.state(), BuildingType.WAREHOUSE, 2, 7);

        Result<BuildingView> result = app.controller.build("WAREHOUSE", 6, 7);

        assertEquals(ErrorCode.LIMITE_EDIFICIOS, result.code());
        assertEquals("Ya tienes el máximo de Almacenes.", result.message());
    }

    @Test
    void HU24_ca3_withoutTheAchievementItIsBlocked() {
        TestApp app = TestApp.startedGame();

        Result<BuildingView> result = app.controller.build("WAREHOUSE", 2, 7);

        assertEquals(ErrorCode.EDIFICIO_BLOQUEADO, result.code());
        assertEquals("Edificio bloqueado: requiere el logro «Primera cosecha».", result.message());
        assertEquals(24, app.state().inventory().capacity());
    }

    @Test
    void HU24_ca4_theNewSlotIsUsableWhenTheFirst24AreFull() {
        TestApp app = TestApp.startedGame();
        GameFixtures.unlock(app.state(), Achievement.FIRST_HARVEST);
        GameFixtures.fillInventoryExcept(app.state(), ItemType.CARROT, 5);
        assertEquals(false, app.state().inventory().add(ItemType.MEDICINE, 1));

        app.controller.build("WAREHOUSE", 2, 7);

        assertTrue(app.state().inventory().add(ItemType.MEDICINE, 1));
        assertEquals(25, app.state().inventory().usedSlots());
    }

    @Test
    void HU24_theWidenedInventorySurvivesAMementoRestore() {
        TestApp app = TestApp.startedGame();
        GameFixtures.unlock(app.state(), Achievement.FIRST_HARVEST);
        app.controller.build("WAREHOUSE", 2, 7);
        var saved = app.state().createMemento();

        app.state().inventory().addSlots(1);
        app.state().restore(saved);

        assertEquals(36, app.state().inventory().capacity());
        assertEquals(1, app.state().farm().buildings().size());
    }
}
