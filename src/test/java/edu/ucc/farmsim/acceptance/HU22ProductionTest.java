package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-22: los animales sanos y alimentados producen y el jugador recoge (RN-02, RN-15, RN-26). */
class HU22ProductionTest {

    @Test
    void HU22_ca1_aFedHenLaysOneEggAndCollectingMovesItToTheInventory() {
        // Given a healthy hen fed today
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.hen(coop);
        app.state().inventory().add(ItemType.HAY, 1);
        app.controller.feed(coop.id());

                // Cuando termina el día el gallinero tiene 1 huevo
        app.controller.endDay();
        assertEquals(1, coop.pendingProducts());

                // Y cuando el jugador recoge, el inventario gana 1 huevo y la energía baja 1
        Result<BuildingActionView> result = app.controller.collect(coop.id());
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(1, result.value().amount());
        assertEquals(1, app.state().inventory().count(ItemType.EGG));
        assertEquals(99, app.state().energy().current());
        assertEquals(0, coop.pendingProducts());
    }

    @Test
    void HU22_ca2_aSickHenProducesNothing_criticalTest() {
        // Given a sick hen
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.SICK, 3, false, 0);

                // Cuando termina el día
        app.controller.endDay();

                // Entonces el gallinero no tiene producto pendiente
        assertEquals(0, coop.pendingProducts());
    }

    @Test
    void HU22_ca2_aHungryHenThatDidNotEatProducesNothingEither() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.hen(coop);

        app.controller.endDay();

        assertEquals(0, coop.pendingProducts());
    }

    @Test
    void HU22_ca3_aHenFedThreeDaysWithoutCollectingKeepsOnlyTwoEggs() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.hen(coop);
        app.state().inventory().add(ItemType.HAY, 3);
        for (int day = 0; day < 3; day++) {
            app.controller.feed(coop.id());
            app.controller.endDay();
        }

        assertEquals(2, coop.pendingProducts());
    }

    @Test
    void HU22_ca3_aCowFollowsTheSameRuleAndGivesMilk() {
        TestApp app = TestApp.startedGame();
        Building barn = GameFixtures.build(app.state(), BuildingType.BARN, 2, 7);
        GameFixtures.house(barn, AnimalType.COW, AnimalCondition.HEALTHY, 0, false, 0);
        app.state().inventory().add(ItemType.HAY, 3);
        for (int day = 0; day < 3; day++) {
            app.controller.feed(barn.id());
            app.controller.endDay();
        }

        assertEquals(2, barn.pendingProducts());
        app.controller.collect(barn.id());
        assertEquals(2, app.state().inventory().count(ItemType.MILK));
    }

    @Test
    void HU22_ca4_withoutRoomInTheInventoryNothingMovesAndNothingIsCharged() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.HEALTHY, 0, false, 2);
        GameFixtures.fillInventoryExcept(app.state(), ItemType.CARROT, 5);

        Result<BuildingActionView> result = app.controller.collect(coop.id());

        assertEquals(ErrorCode.INVENTARIO_LLENO, result.code());
        assertEquals(2, coop.pendingProducts());
        assertEquals(100, app.state().energy().current());
    }

    @Test
    void HU22_ca4_withNothingWaitingThePlayerIsToldAndNothingIsCharged() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.hen(coop);

        Result<BuildingActionView> result = app.controller.collect(coop.id());

        assertTrue(result.isOk());
        assertEquals(0, result.value().amount());
        assertEquals("No hay productos que recoger.", result.value().notice());
        assertEquals(100, app.state().energy().current());
    }

    @Test
    void HU22_withoutEnergyCollectingIsRejectedAndTheProductsStay() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.HEALTHY, 0, false, 1);
        app.state().energy().trySpend(100);

        Result<BuildingActionView> result = app.controller.collect(coop.id());

        assertEquals(ErrorCode.SIN_ENERGIA, result.code());
        assertEquals(1, coop.pendingProducts());
    }
}
