package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.AnimalView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-19: buying hens and cows and housing them (RN-14, RN-20, RN-23). */
class HU19BuyAnimalTest {

    @Test
    void HU19_ca1_buyingAHenCosts150AndHousesAHealthyHen() {
        // Given an empty coop and 500 m
        TestApp app = TestApp.startedGame();
        GameFixtures.coop(app.state());

        // When the player buys a hen
        Result<BuildingView> result = app.controller.buyAnimal("HEN");

        // Then the balance is 350 m and a healthy hen lives in the coop
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(350, app.state().money());
        AnimalView hen = result.value().animals().get(0);
        assertEquals("HEALTHY", hen.condition());
        assertEquals(0, hen.daysWithoutFeed());
        assertEquals("Gallina", hen.name());
        assertEquals(1, app.state().farm().buildings().get(0).animals().size());
    }

    @Test
    void HU19_ca2_withoutACompatibleBuildingTheAnimalIsRejectedAndTheBalanceDoesNotChange() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 1000);

        Result<BuildingView> hen = app.controller.buyAnimal("HEN");
        Result<BuildingView> cow = app.controller.buyAnimal("COW");

        assertEquals(ErrorCode.SIN_ESPACIO_ANIMALES, hen.code());
        assertEquals("No hay espacio para más Gallinas: construye o amplía el edificio.", hen.message());
        assertEquals(ErrorCode.SIN_ESPACIO_ANIMALES, cow.code());
        assertEquals("No hay espacio para más Vacas: construye o amplía el edificio.", cow.message());
        assertEquals(1000, app.state().money());
    }

    @Test
    void HU19_ca3_aFullCoopRejectsAFifthHenAndASecondCoopTakesIt() {
        TestApp app = TestApp.startedGame();
        Building first = GameFixtures.coop(app.state());
        for (int i = 0; i < 4; i++) {
            GameFixtures.hen(first);
        }

        Result<BuildingView> rejected = app.controller.buyAnimal("HEN");
        assertEquals(ErrorCode.SIN_ESPACIO_ANIMALES, rejected.code());
        assertEquals(500, app.state().money());

        Building second = GameFixtures.build(app.state(), BuildingType.COOP, 5, 7);
        Result<BuildingView> accepted = app.controller.buyAnimal("HEN");

        assertTrue(accepted.isOk());
        assertEquals(second.id(), accepted.value().id());
        assertEquals(1, second.animals().size());
        assertEquals(4, first.animals().size());
    }

    @Test
    void HU19_ca3_aCowGoesToTheBarnAndAHenNeverDoes() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 1000);
        GameFixtures.build(app.state(), BuildingType.BARN, 2, 7);

        assertEquals(ErrorCode.SIN_ESPACIO_ANIMALES, app.controller.buyAnimal("HEN").code());
        assertTrue(app.controller.buyAnimal("COW").isOk());
        assertEquals(200, app.state().money());
    }

    @Test
    void HU19_ca4_notEnoughMoneyIsRejectedWithTheMissingAmount() {
        TestApp app = TestApp.startedGame();
        GameFixtures.coop(app.state());
        GameFixtures.setMoney(app.state(), 149);

        Result<BuildingView> result = app.controller.buyAnimal("HEN");

        assertEquals(ErrorCode.SIN_FONDOS, result.code());
        assertEquals("Fondos insuficientes: faltan 1 m.", result.message());
        assertEquals(149, app.state().money());
        assertTrue(app.state().farm().buildings().get(0).animals().isEmpty());
    }

    @Test
    void HU19_theMarketOffersAnimalsWithTheirFreePlaces() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.hen(coop);

        var animals = app.controller.market().value().animals();

        assertEquals(2, animals.size());
        assertEquals("Gallina", animals.get(0).name());
        assertEquals(150, animals.get(0).price());
        assertEquals(3, animals.get(0).freePlaces());
        assertEquals(800, animals.get(1).price());
        assertEquals(0, animals.get(1).freePlaces());
    }
}
