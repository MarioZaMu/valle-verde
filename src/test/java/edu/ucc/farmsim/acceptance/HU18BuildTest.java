package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-18: building coops, barns, warehouses and greenhouses on free grass (RN-20, RN-21, RN-29). */
class HU18BuildTest {

    @Test
    void HU18_ca1_buildingACoopCosts400AndOccupiesFourPlots() {
        // Given 500 m and a free 2 x 2 grass area with its corner at (2,7)
        TestApp app = TestApp.startedGame();

        // When the player builds a coop there
        Result<BuildingView> result = app.controller.build("COOP", 2, 7);

        // Then the balance is 100 m and the four plots are occupied
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals("Gallinero", result.value().name());
        assertEquals(100, app.state().money());
        for (int[] plot : new int[][] {{2, 7}, {3, 7}, {2, 8}, {3, 8}}) {
            assertNotNull(app.state().farm().buildingAt(plot[0], plot[1]));
        }
        assertNull(app.state().farm().buildingAt(4, 7));
        assertEquals(List.of(new GameEvent.MoneyChanged(100), new GameEvent.MapChanged()), app.events);
    }

    @Test
    void HU18_ca2_anAreaWithSoilOrOutsideTheMapIsRejectedAndNothingChanges() {
        TestApp app = TestApp.startedGame();

        // When the area includes soil (4,2)
        Result<BuildingView> onSoil = app.controller.build("COOP", 3, 2);
        // When it sticks out of the map (column 15, width 2)
        Result<BuildingView> outside = app.controller.build("COOP", 15, 3);
        // When it covers the pond
        Result<BuildingView> onWater = app.controller.build("COOP", 13, 0);

        for (Result<BuildingView> result : List.of(onSoil, outside, onWater)) {
            assertEquals(ErrorCode.UBICACION_INVALIDA, result.code());
        }
        assertEquals("No se puede construir aquí: el área debe ser hierba libre dentro del mapa.", onSoil.message());
        assertEquals(500, app.state().money());
        assertTrue(app.state().farm().buildings().isEmpty());
        assertTrue(app.events.isEmpty());
    }

    @Test
    void HU18_ca3_notEnoughMoneyIsRejectedWithTheMissingAmount() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 399);

        Result<BuildingView> result = app.controller.build("COOP", 2, 7);

        assertEquals(ErrorCode.SIN_FONDOS, result.code());
        assertEquals("Fondos insuficientes: faltan 1 m.", result.message());
        assertEquals(399, app.state().money());
        assertTrue(app.state().farm().buildings().isEmpty());
    }

    @Test
    void HU18_ca4_aThirdCoopIsRejectedByTheLimit() {
        TestApp app = TestApp.startedGame();
        GameFixtures.build(app.state(), BuildingType.COOP, 2, 7);
        GameFixtures.build(app.state(), BuildingType.COOP, 5, 7);

        Result<BuildingView> result = app.controller.build("COOP", 8, 7);

        assertEquals(ErrorCode.LIMITE_EDIFICIOS, result.code());
        assertEquals("Ya tienes el máximo de Gallineros.", result.message());
        assertEquals(500, app.state().money());
        assertEquals(2, app.state().farm().buildings().size());
    }

    @Test
    void HU18_ca5_aBarnWithoutTheAchievementIsBlocked() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 1000);

        Result<BuildingView> result = app.controller.build("BARN", 2, 7);

        assertEquals(ErrorCode.EDIFICIO_BLOQUEADO, result.code());
        assertEquals("Edificio bloqueado: requiere el logro «Manos a la tierra».", result.message());
        assertEquals(1000, app.state().money());
    }

    @Test
    void HU18_ca5_withTheAchievementTheBarnCanBeBuilt() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 1000);
        GameFixtures.unlock(app.state(), Achievement.GREEN_THUMB);

        Result<BuildingView> result = app.controller.build("BARN", 2, 7);

        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(0, app.state().money());
        assertEquals(3, result.value().width());
        assertEquals(2, result.value().height());
    }

    @Test
    void HU18_ca6_aBuildingThatTouchesAnotherIsRejected() {
        TestApp app = TestApp.startedGame();
        GameFixtures.build(app.state(), BuildingType.COOP, 2, 7);

        // The new area (3,8)-(4,9) shares the plot (3,8) with the first coop
        Result<BuildingView> result = app.controller.build("COOP", 3, 8);

        assertEquals(ErrorCode.UBICACION_INVALIDA, result.code());
        assertEquals(500, app.state().money());
        assertFalse(app.state().farm().canPlace(BuildingType.COOP, 3, 8));
    }

    @Test
    void HU18_theCatalogueTellsWhatIsBuiltAndWhatIsLocked() {
        TestApp app = TestApp.startedGame();
        GameFixtures.coop(app.state());

        var catalog = app.controller.buildings().value().catalog();

        assertEquals(4, catalog.size());
        assertTrue(catalog.get(0).unlocked());
        assertEquals(1, catalog.get(0).built());
        assertEquals(2, catalog.get(0).max());
        assertFalse(catalog.get(1).unlocked());
        assertEquals("Manos a la tierra", catalog.get(1).requirement());
        assertEquals(1, app.controller.buildings().value().buildings().size());
    }

    @Test
    void HU18_withoutAGameEveryOrderAnswersSinPartida() {
        TestApp app = TestApp.create();

        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.build("COOP", 2, 7).code());
        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.buildings().code());
        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.buyAnimal("HEN").code());
        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.feed(0).code());
        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.collect(0).code());
        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.cure(0, 0).code());
    }

    @Test
    void HU18_anUnknownBuildingIsAnInternalErrorAndNeverThrows() {
        TestApp app = TestApp.startedGame();

        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.build("CASTLE", 2, 7).code());
        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.feed(7).code());
        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.collect(-1).code());
        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.cure(0, 0).code());
    }
}
