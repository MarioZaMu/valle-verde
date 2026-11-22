package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.Weather;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-25: the greenhouse protects crops from the season, the frost and the drought, but not from thirst (RN-22). */
class HU25GreenhouseTest {

    private static final int GH_COL = GameFixtures.GRASS_COL;
    private static final int GH_ROW = GameFixtures.GRASS_ROW;
    private static final int OPEN_COL = GameFixtures.SOIL_COL;
    private static final int OPEN_ROW = GameFixtures.SOIL_ROW;

    private static TestApp appWithGreenhouse() {
        TestApp app = TestApp.startedGame();
        GameFixtures.unlock(app.state(), Achievement.BUILDER);
        GameFixtures.unlock(app.state(), Achievement.CAPITALIST);
        GameFixtures.setMoney(app.state(), 2000);
        assertTrue(app.controller.build("GREENHOUSE", GH_COL, GH_ROW).isOk());
        return app;
    }

    private static Crop inside(TestApp app, int dx, int dy) {
        return app.state().farm().plotAt(GH_COL + dx, GH_ROW + dy).crop();
    }

    @Test
    void HU25_ca1_buildingItCosts1500AndMakesNineProtectedPlots() {
        TestApp app = TestApp.startedGame();
        GameFixtures.unlock(app.state(), Achievement.BUILDER);
        GameFixtures.unlock(app.state(), Achievement.CAPITALIST);
        GameFixtures.setMoney(app.state(), 2000);

        var built = app.controller.build("GREENHOUSE", GH_COL, GH_ROW);

        assertTrue(built.isOk());
        assertEquals(500, app.state().money());
        int sheltered = 0;
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = 0; dx < 3; dx++) {
                assertTrue(app.state().farm().plotAt(GH_COL + dx, GH_ROW + dy).isSheltered());
                assertTrue(app.controller.inspect(GH_COL + dx, GH_ROW + dy).value().sheltered());
                sheltered++;
            }
        }
        assertEquals(9, sheltered);
        assertFalse(app.state().farm().plotAt(OPEN_COL, OPEN_ROW).isSheltered());
        assertFalse(app.controller.inspect(OPEN_COL, OPEN_ROW).value().sheltered());
    }

    @Test
    void HU25_ca2_aSeedOutOfSeasonCanBeSownInsideButNotOutside() {
        TestApp app = appWithGreenhouse();
        app.state().inventory().add(ItemType.PUMPKIN_SEED, 4);
        assertEquals(Season.SPRING, app.state().date().season());

        Result<PlotView> inside = app.controller.plant(GH_COL + 1, GH_ROW + 1, "PUMPKIN_SEED");
        Result<PlotView> outside = app.controller.plant(OPEN_COL, OPEN_ROW, "PUMPKIN_SEED");

        assertTrue(inside.isOk());
        assertEquals("PUMPKIN", inside.value().crop());
        assertEquals(ErrorCode.FUERA_DE_ESTACION, outside.code());
    }

    @Test
    void HU25_ca2_aGreenhousePlotStillRejectsAnOccupiedPlotAndGrassOutsideIt() {
        TestApp app = appWithGreenhouse();
        app.state().inventory().add(ItemType.CARROT_SEED, 4);

        assertTrue(app.controller.plant(GH_COL, GH_ROW, "CARROT_SEED").isOk());
        assertEquals(ErrorCode.PARCELA_OCUPADA, app.controller.plant(GH_COL, GH_ROW, "CARROT_SEED").code());
        assertEquals(ErrorCode.PARCELA_NO_CULTIVABLE, app.controller.plant(GH_COL + 3, GH_ROW, "CARROT_SEED").code());
    }

    @Test
    void HU25_ca3_frostWithersTheCropsInTheOpenButNotTheOnesInside() {
        TestApp app = appWithGreenhouse();
        GameFixtures.plant(app.state(), GH_COL, GH_ROW, CropType.CARROT, 2, 0, true, false);
        GameFixtures.plant(app.state(), OPEN_COL, OPEN_ROW, CropType.CARROT, 2, 0, true, false);
        app.state().weather().restore(Weather.FROST, Weather.SUNNY);

        app.controller.endDay();

        assertTrue(inside(app, 0, 0).isAlive());
        assertFalse(app.state().farm().plotAt(OPEN_COL, OPEN_ROW).crop().isAlive());
    }

    @Test
    void HU25_ca3_droughtLowersToleranceOutsideButTheGreenhouseKeepsThreeDays() {
        TestApp app = appWithGreenhouse();
        GameFixtures.plant(app.state(), GH_COL, GH_ROW, CropType.CARROT, 1, 1, false, false);
        GameFixtures.plant(app.state(), OPEN_COL, OPEN_ROW, CropType.CARROT, 1, 1, false, false);
        app.state().weather().restore(Weather.DROUGHT, Weather.SUNNY);

        app.controller.endDay();

        assertTrue(inside(app, 0, 0).isAlive());
        assertEquals(2, inside(app, 0, 0).daysWithoutWater());
        assertFalse(app.state().farm().plotAt(OPEN_COL, OPEN_ROW).crop().isAlive());
    }

    @Test
    void HU25_ca3_insideTheGreenhouseAThirdDryDayStillWithers() {
        TestApp app = appWithGreenhouse();
        GameFixtures.plant(app.state(), GH_COL, GH_ROW, CropType.CARROT, 1, 2, false, false);

        app.controller.endDay();

        assertFalse(inside(app, 0, 0).isAlive());
    }

    @Test
    void HU25_ca3_aChangeOfSeasonDoesNotWitherTheCropsInside() {
        TestApp app = appWithGreenhouse();
        GameFixtures.setDate(1, Season.SPRING, 28);
        GameFixtures.plant(app.state(), GH_COL, GH_ROW, CropType.CARROT, 1, 0, true, false);
        GameFixtures.plant(app.state(), OPEN_COL, OPEN_ROW, CropType.CARROT, 1, 0, true, false);

        app.controller.endDay();

        assertEquals(Season.SUMMER, app.state().date().season());
        assertTrue(inside(app, 0, 0).isAlive());
        assertFalse(app.state().farm().plotAt(OPEN_COL, OPEN_ROW).crop().isAlive());
    }

    @Test
    void HU25_ca4_rainWatersTheCropsOutsideButNotTheOnesInside() {
        TestApp app = appWithGreenhouse();
        GameFixtures.plant(app.state(), GH_COL, GH_ROW, CropType.CARROT, 1, 1, false, false);
        GameFixtures.plant(app.state(), OPEN_COL, OPEN_ROW, CropType.CARROT, 1, 1, false, false);
        app.state().weather().restore(Weather.RAINY, Weather.SUNNY);

        app.controller.endDay();

        assertEquals(1, inside(app, 0, 0).daysGrown());
        assertEquals(2, inside(app, 0, 0).daysWithoutWater());
        assertEquals(2, app.state().farm().plotAt(OPEN_COL, OPEN_ROW).crop().daysGrown());
    }

    @Test
    void HU25_ca4_wateringInsideCostsOneEnergyEvenInDrought() {
        TestApp app = appWithGreenhouse();
        GameFixtures.plant(app.state(), GH_COL, GH_ROW, CropType.CARROT, 1);
        GameFixtures.plant(app.state(), OPEN_COL, OPEN_ROW, CropType.CARROT, 1);
        app.state().weather().restore(Weather.DROUGHT, Weather.SUNNY);
        int before = app.state().energy().current();

        assertTrue(app.controller.water(GH_COL, GH_ROW).isOk());
        assertEquals(before - 1, app.state().energy().current());
        assertTrue(app.controller.water(OPEN_COL, OPEN_ROW).isOk());
        assertEquals(before - 3, app.state().energy().current());
    }

    @Test
    void HU25_ca5_withoutTheAchievementItIsRejected() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 3000);

        assertEquals(ErrorCode.EDIFICIO_BLOQUEADO, app.controller.build("GREENHOUSE", GH_COL, GH_ROW).code());
        assertEquals(0, app.state().farm().countBuildings(BuildingType.GREENHOUSE));
    }

    @Test
    void HU25_ca3_theProtectedCropsSurviveSavingAndLoading() {
        TestApp app = appWithGreenhouse();
        app.state().inventory().add(ItemType.PUMPKIN_SEED, 2);
        app.controller.plant(GH_COL, GH_ROW, "PUMPKIN_SEED");

        var memento = app.state().createMemento();
        app.controller.newGame(1L);
        app.state().restore(memento);

        assertTrue(app.state().farm().plotAt(GH_COL, GH_ROW).isSheltered());
        assertEquals(CropType.PUMPKIN, inside(app, 0, 0).type());
        assertTrue(edu.ucc.farmsim.domain.game.GameStateValidator.violations(memento).isEmpty());
    }
}
