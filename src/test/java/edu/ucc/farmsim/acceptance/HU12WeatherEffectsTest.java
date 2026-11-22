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

/** HU-12: rain waters, drought punishes and frost withers the crops in the open (RN-03, RN-09, RN-12). */
class HU12WeatherEffectsTest {

    private static final int ROW = GameFixtures.SOIL_ROW;
    private static final int FIRST_COL = GameFixtures.SOIL_COL;

    private static Crop cropAt(TestApp app, int offset) {
        return app.state().farm().plotAt(FIRST_COL + offset, ROW).crop();
    }

    @Test
    void HU12_ca1_rainWatersEveryCropInTheOpenSoTheyGrowOneDay() {
        // Given 5 unwatered crops that are two days from withering, and a Rainy day ending
        TestApp app = TestApp.startedGame();
        for (int i = 0; i < 5; i++) {
            GameFixtures.plant(app.state(), FIRST_COL + i, ROW, CropType.CARROT, 1, 2, false, false);
        }
        app.state().weather().restore(Weather.RAINY, Weather.SUNNY);

        // When the day ends
        app.controller.endDay();

        // Then all 5 were watered by the rain: they grew one day, have no dry days and are alive
        for (int i = 0; i < 5; i++) {
            Crop crop = cropAt(app, i);
            assertEquals(2, crop.daysGrown(), "crop " + i);
            assertEquals(0, crop.daysWithoutWater(), "crop " + i);
            assertTrue(crop.isAlive(), "crop " + i);
        }
    }

    @Test
    void HU12_ca2_frostWithersEveryCropInTheOpenEvenAMatureOne() {
        // Given 3 crops in the open, one of them mature and not harvested, and a Frost day ending
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), FIRST_COL, ROW, CropType.CARROT, 1);
        GameFixtures.plant(app.state(), FIRST_COL + 1, ROW, CropType.POTATO, 3);
        GameFixtures.plant(app.state(), FIRST_COL + 2, ROW, CropType.CARROT, 4);
        assertEquals(CropStage.MATURE, cropAt(app, 2).stage());
        app.state().weather().restore(Weather.FROST, Weather.SUNNY);

        // When the day ends
        app.controller.endDay();

        // Then the 3 crops are withered
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

        // When the player waters one crop it costs 2 energy
        assertTrue(app.controller.water(FIRST_COL + 1, ROW).isOk());
        assertEquals(98, app.state().energy().current());

        // And when the day ends, the crop with 1 dry day (now 2) withers and the watered one survives
        app.controller.endDay();
        assertEquals(CropStage.WITHERED, cropAt(app, 0).stage());
        assertTrue(cropAt(app, 1).isAlive());
    }

    @Test
    void HU12_ca4_sunnyCloudyAndSnowyDaysOnlyAddADryDay() {
        for (Weather weather : new Weather[] {Weather.SUNNY, Weather.CLOUDY, Weather.SNOWY}) {
            // Given an unwatered crop and a day of this weather ending
            TestApp app = TestApp.startedGame();
            GameFixtures.plant(app.state(), FIRST_COL, ROW, CropType.CARROT, 1);
            app.state().weather().restore(weather, Weather.SUNNY);

            // When the day ends
            app.controller.endDay();

            // Then it only gained a dry day: not watered by the weather, not withered by it
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
