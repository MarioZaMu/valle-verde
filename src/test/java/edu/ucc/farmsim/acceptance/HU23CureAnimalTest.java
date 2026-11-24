package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** HU-23: curar a un animal enfermo con medicina (RN-02, RN-16, RN-27). */
class HU23CureAnimalTest {

    private static Animal sickHen(TestApp app, int medicines) {
        Building coop = GameFixtures.coop(app.state());
        Animal hen = GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.SICK, 3, false, 0);
        if (medicines > 0) {
            app.state().inventory().add(ItemType.MEDICINE, medicines);
        }
        return hen;
    }

    @Test
    void HU23_ca1_aMedicineMakesTheHenHealthyAtOnceForOneEnergy() {
        TestApp app = TestApp.startedGame();
        Animal hen = sickHen(app, 1);

        Result<BuildingActionView> result = app.controller.cure(0, 0);

        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(1, result.value().amount());
        assertEquals(AnimalCondition.HEALTHY, hen.condition());
        assertEquals(0, hen.daysWithoutFeed());
        assertEquals(0, app.state().inventory().count(ItemType.MEDICINE));
        assertEquals(99, app.state().energy().current());
    }

    @Test
    void HU23_ca2_withoutMedicineTheHenStaysSick() {
        TestApp app = TestApp.startedGame();
        Animal hen = sickHen(app, 0);

        Result<BuildingActionView> result = app.controller.cure(0, 0);

        assertEquals(ErrorCode.SIN_MEDICINA, result.code());
        assertEquals("No tienes Medicina.", result.message());
        assertEquals(AnimalCondition.SICK, hen.condition());
        assertEquals(100, app.state().energy().current());
    }

    @Test
    void HU23_ca3_withoutEnergyTheMedicineIsKept() {
        TestApp app = TestApp.startedGame();
        Animal hen = sickHen(app, 1);
        app.state().energy().trySpend(100);

        Result<BuildingActionView> result = app.controller.cure(0, 0);

        assertEquals(ErrorCode.SIN_ENERGIA, result.code());
        assertEquals(1, app.state().inventory().count(ItemType.MEDICINE));
        assertEquals(AnimalCondition.SICK, hen.condition());
    }

    @Test
    void HU23_ca4_aCuredHenThatIsFedTheSameDayLaysAnEgg() {
        TestApp app = TestApp.startedGame();
        sickHen(app, 1);
        app.state().inventory().add(ItemType.HAY, 1);
        app.controller.cure(0, 0);
        app.controller.feed(0);

        app.controller.endDay();

        assertEquals(1, app.state().farm().buildings().get(0).pendingProducts());
    }

    @Test
    void HU23_curingAHealthyAnimalOnlyTellsThePlayer() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.hen(coop);
        app.state().inventory().add(ItemType.MEDICINE, 1);

        Result<BuildingActionView> result = app.controller.cure(0, 0);

        assertTrue(result.isOk());
        assertEquals(0, result.value().amount());
        assertEquals("Este animal no está enfermo.", result.value().notice());
        assertEquals(1, app.state().inventory().count(ItemType.MEDICINE));
        assertEquals(100, app.state().energy().current());
    }

    @Test
    void HU23_anAnimalThatDoesNotExistIsAnInternalError() {
        TestApp app = TestApp.startedGame();
        GameFixtures.coop(app.state());

        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.cure(0, 3).code());
    }
}
