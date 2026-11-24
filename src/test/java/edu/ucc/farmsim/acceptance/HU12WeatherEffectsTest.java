package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropStage;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.time.Weather;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/**
 * HU-12: la lluvia riega, la sequía castiga y la helada marchita los cultivos a la intemperie (RN-03, RN-09, RN-12).
 */
class HU12WeatherEffectsTest {

    private static final int ROW = GameFixtures.SOIL_ROW;
    private static final int FIRST_COL = GameFixtures.SOIL_COL;

    private static Crop cropAt(TestApp app, int offset) {
        return app.state().farm().plotAt(FIRST_COL + offset, ROW).crop();
    }

    @Test
    void HU12_ca1_rainWatersEveryCropInTheOpenSoTheyGrowOneDay() {
                // Dados 5 cultivos sin regar a dos días de marchitarse, y que termina un día Rainy
        TestApp app = TestApp.startedGame();
        for (int i = 0; i < 5; i++) {
            GameFixtures.plant(app.state(), FIRST_COL + i, ROW, CropType.CARROT, 1, 2, false, false);
        }
        app.state().weather().restore(Weather.RAINY, Weather.SUNNY);

                // Cuando termina el día
        app.controller.endDay();

                // Entonces la lluvia regó los 5: crecieron un día, no tienen días secos y están vivos
        for (int i = 0; i < 5; i++) {
            Crop crop = cropAt(app, i);
            assertEquals(2, crop.daysGrown(), "crop " + i);
            assertEquals(0, crop.daysWithoutWater(), "crop " + i);
            assertTrue(crop.isAlive(), "crop " + i);
        }
    }

    @Test
    void HU12_ca2_frostWithersEveryCropInTheOpenEvenAMatureOne() {
                // Dados 3 cultivos al aire libre, uno de ellos maduro y sin cosechar, y que termina un día de Frost
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), FIRST_COL, ROW, CropType.CARROT, 1);
        GameFixtures.plant(app.state(), FIRST_COL + 1, ROW, CropType.POTATO, 3);
        GameFixtures.plant(app.state(), FIRST_COL + 2, ROW, CropType.CARROT, 4);
        assertEquals(CropStage.MATURE, cropAt(app, 2).stage());
        app.state().weather().restore(Weather.FROST, Weather.SUNNY);

                // Cuando termina el día
        app.controller.endDay();

                // Entonces los 3 cultivos están marchitos
        for (int i = 0; i < 3; i++) {
            assertEquals(CropStage.WITHERED, cropAt(app, i).stage(), "crop " + i);
        }
    }

    @Test
    void HU12_ca2_frostLeavesAnAlreadyWitheredCropAndEmptyPlotsAlone() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), FIRST_COL, ROW, CropType.CARROT, 0, 0, false, true);
        app.state().weather().restore(Weather.FROST, Weather.SUNNY);

        app.controller.endDay();

        assertEquals(CropStage.WITHERED, cropAt(app, 0).stage());
        assertEquals(null, cropAt(app, 1));
    }

    @Test
    void HU12_ca3_inADroughtWateringCostsTwoEnergyAndOneDryDayIsEnoughToWither() {
        // Given a Drought day
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), FIRST_COL, ROW, CropType.CARROT, 2, 1, false, false);
        GameFixtures.plant(app.state(), FIRST_COL + 1, ROW, CropType.CARROT, 2, 0, false, false);
        app.state().weather().restore(Weather.DROUGHT, Weather.SUNNY);

                // Cuando el jugador riega un cultivo cuesta 2 de energía
        assertTrue(app.controller.water(FIRST_COL + 1, ROW).isOk());
        assertEquals(98, app.state().energy().current());

                // Y cuando termina el día, el cultivo con 1 día seco (ahora 2) se marchita y el regado sobrevive
        app.controller.endDay();
        assertEquals(CropStage.WITHERED, cropAt(app, 0).stage());
        assertTrue(cropAt(app, 1).isAlive());
    }

    @Test
    void HU12_ca4_sunnyCloudyAndSnowyDaysOnlyAddADryDay() {
        for (Weather weather : new Weather[] {Weather.SUNNY, Weather.CLOUDY, Weather.SNOWY}) {
                        // Dado un cultivo sin regar y que termina un día de este clima
            TestApp app = TestApp.startedGame();
            GameFixtures.plant(app.state(), FIRST_COL, ROW, CropType.CARROT, 1);
            app.state().weather().restore(weather, Weather.SUNNY);

                        // Cuando termina el día
            app.controller.endDay();

                        // Entonces solo ganó un día seco: no lo regó el clima, no lo marchitó el clima
            Crop crop = cropAt(app, 0);
            assertEquals(1, crop.daysWithoutWater(), weather.name());
            assertEquals(1, crop.daysGrown(), weather.name());
            assertTrue(crop.isAlive(), weather.name());
            assertFalse(crop.isWatered(), weather.name());
        }
    }

    @Test
    void HU12_theWitheredByFrostAreCountedInTheDayReport() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), FIRST_COL, ROW, CropType.CARROT, 1);
        GameFixtures.plant(app.state(), FIRST_COL + 1, ROW, CropType.CARROT, 1);
        app.state().weather().restore(Weather.FROST, Weather.SUNNY);

        var report = edu.ucc.farmsim.domain.game.DayPipeline.standard().run(app.state());

        assertEquals(2, report.witheredCrops());
        assertTrue(report.phasesRun().contains(edu.ucc.farmsim.domain.game.DayPhase.WEATHER_EFFECTS));
    }
}
