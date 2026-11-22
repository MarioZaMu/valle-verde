package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-20: feeding every animal of a building at once (RN-02, RN-15, RN-24). */
class HU20FeedAnimalsTest {

    private static Building coopWithHens(TestApp app, int healthy, int sick) {
        Building coop = GameFixtures.coop(app.state());
        for (int i = 0; i < healthy; i++) {
            GameFixtures.hen(coop);
        }
        for (int i = 0; i < sick; i++) {
            GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.SICK, 3, false, 0);
        }
        return coop;
    }

    @Test
    void HU20_ca1_feedingThreeHensCostsThreeHayAndThreeEnergy() {
        // Given a coop with 3 healthy hens that have not eaten, 5 hay and 100 energy
        TestApp app = TestApp.startedGame();
        Building coop = coopWithHens(app, 3, 0);
        app.state().inventory().add(ItemType.HAY, 5);

        // When the player feeds the building
        Result<BuildingActionView> result = app.controller.feed(coop.id());

        // Then 2 hay and 97 energy are left and the 3 hens ate today
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(3, result.value().amount());
        assertNull(result.value().notice());
        assertEquals(2, app.state().inventory().count(ItemType.HAY));
        assertEquals(97, app.state().energy().current());
        assertTrue(coop.animals().stream().allMatch(Animal::isFedToday));
    }

    @Test
    void HU20_ca2_notEnoughHayRejectsEverythingAndConsumesNothing() {
        TestApp app = TestApp.startedGame();
        Building coop = coopWithHens(app, 3, 0);
        app.state().inventory().add(ItemType.HAY, 2);

        Result<BuildingActionView> result = app.controller.feed(coop.id());

        assertEquals(ErrorCode.SIN_HENO, result.code());
        assertEquals("Heno insuficiente: necesitas 3.", result.message());
        assertEquals(2, app.state().inventory().count(ItemType.HAY));
        assertEquals(100, app.state().energy().current());
        assertTrue(coop.animals().stream().noneMatch(Animal::isFedToday));
    }

    @Test
    void HU20_ca3_notEnoughEnergyRejectsEverythingAndConsumesNothing() {
        TestApp app = TestApp.startedGame();
        Building coop = coopWithHens(app, 3, 0);
        app.state().inventory().add(ItemType.HAY, 5);
        app.state().energy().trySpend(98);

        Result<BuildingActionView> result = app.controller.feed(coop.id());

        assertEquals(ErrorCode.SIN_ENERGIA, result.code());
        assertEquals("Energía insuficiente: necesitas 3 y tienes 2.", result.message());
        assertEquals(5, app.state().inventory().count(ItemType.HAY));
        assertEquals(2, app.state().energy().current());
        assertTrue(coop.animals().stream().noneMatch(Animal::isFedToday));
    }

    @Test
    void HU20_ca4_aSickHenIsSkippedAndThePlayerIsWarned() {
        TestApp app = TestApp.startedGame();
        Building coop = coopWithHens(app, 2, 1);
        app.state().inventory().add(ItemType.HAY, 5);

        Result<BuildingActionView> result = app.controller.feed(coop.id());

        assertTrue(result.isOk());
        assertEquals(2, result.value().amount());
        assertEquals("Hay animales enfermos: cúralos con Medicina.", result.value().notice());
        assertEquals(3, app.state().inventory().count(ItemType.HAY));
        assertEquals(98, app.state().energy().current());
        assertEquals(2, coop.animals().stream().filter(Animal::isFedToday).count());
        assertEquals(AnimalCondition.SICK, coop.animals().get(2).condition());
    }

    @Test
    void HU20_ca5_ifEveryoneAlreadyAteThereIsNothingToFeedAndNothingIsConsumed() {
        TestApp app = TestApp.startedGame();
        Building coop = coopWithHens(app, 2, 0);
        app.state().inventory().add(ItemType.HAY, 5);
        app.controller.feed(coop.id());

        Result<BuildingActionView> again = app.controller.feed(coop.id());

        assertTrue(again.isOk());
        assertEquals(0, again.value().amount());
        assertEquals("No hay animales por alimentar.", again.value().notice());
        assertEquals(3, app.state().inventory().count(ItemType.HAY));
        assertEquals(98, app.state().energy().current());
    }

    @Test
    void HU20_aBuildingWithoutAnimalsHasNothingToFeed() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());

        Result<BuildingActionView> result = app.controller.feed(coop.id());

        assertEquals(0, result.value().amount());
        assertEquals("No hay animales por alimentar.", result.value().notice());
    }
}
