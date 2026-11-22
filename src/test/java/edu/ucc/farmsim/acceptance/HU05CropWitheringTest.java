package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropStage;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.Weather;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-05: crops wither from lack of water and out of season (RN-01, RN-03, RN-09). */
class HU05CropWitheringTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    /** Ends the day with a Cloudy day, so the random weather (rain would water the crops) does not interfere. */
    private static void endCloudyDay(TestApp app) {
        app.state().weather().restore(Weather.CLOUDY, Weather.CLOUDY);
        app.controller.endDay();
    }

    private static Crop cropOf(TestApp app) {
        return app.state().farm().plotAt(COL, ROW).crop();
    }

    @Test
    void HU05_ca1_aCropWithersAfterThreeDaysWithoutWater() {
        // Given a crop in the Growing stage
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2);
        assertEquals(CropStage.GROWING, cropOf(app).stage());

        // When 2 days pass without watering it, it is still alive with 2 dry days
        endCloudyDay(app);
        endCloudyDay(app);
        assertTrue(cropOf(app).isAlive());
        assertEquals(2, cropOf(app).daysWithoutWater());

        // And when the third day passes, it withers
        endCloudyDay(app);
        assertEquals(CropStage.WITHERED, cropOf(app).stage());
    }

    @Test
    void HU05_ca2_inADroughtACropWithersAfterJustTwoDryDays() {
        // Given a crop with 1 day without water and a Drought day ending
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 1, false, false);
        app.state().weather().restore(Weather.DROUGHT, Weather.SUNNY);

        // When the day ends
        app.controller.endDay();

        // Then it has 2 dry days and it has withered (tolerance is 2 in a drought)
        assertEquals(2, cropOf(app).daysWithoutWater());
        assertEquals(CropStage.WITHERED, cropOf(app).stage());
    }

    @Test
    void HU05_ca3_cropsOutOfTheNewSeasonWitherEvenIfMature() {
        // Given a mature carrot (valid only in Spring) alive on day 28 of Spring
        TestApp app = TestApp.startedGame();
        GameFixtures.setDate(1, Season.SPRING, 28);
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 4);

        // When the day ends and Summer begins, it withers
        app.controller.endDay();
        assertEquals(Season.SUMMER, app.state().date().season());
        assertEquals(CropStage.WITHERED, cropOf(app).stage());
    }

    @Test
    void HU05_ca3_aCornCropSurvivesSummerToAutumnButNotAutumnToWinter() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setDate(1, Season.SUMMER, 28);
        GameFixtures.plant(app.state(), COL, ROW, CropType.CORN, 8);

        app.controller.endDay();
        assertEquals(Season.AUTUMN, app.state().date().season());
        assertTrue(cropOf(app).isAlive(), "corn is valid in Summer and Autumn");

        GameFixtures.setDate(1, Season.AUTUMN, 28);
        app.controller.endDay();
        assertEquals(Season.WINTER, app.state().date().season());
        assertEquals(CropStage.WITHERED, cropOf(app).stage());
    }

    @Test
    void HU05_ca3_nothingWithersByTheSeasonInTheMiddleOfAValidSeason() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setDate(1, Season.SPRING, 27);
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 4);

        app.controller.endDay();

        assertEquals(28, app.state().date().day());
        assertTrue(cropOf(app).isAlive());
    }

    @Test
    void HU05_ca4_wateringResetsTheDryDayCounter() {
        // Given a crop with 2 dry days
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 2, false, false);

        // When it is watered and the day ends
        assertTrue(app.controller.water(COL, ROW).isOk());
        app.controller.endDay();

        // Then the counter is 0 and the crop did not wither
        assertEquals(0, cropOf(app).daysWithoutWater());
        assertTrue(cropOf(app).isAlive());
        assertEquals(3, cropOf(app).daysGrown());
    }

    @Test
    void HU05_ca5_aWitheredCropKeepsOccupyingThePlotAndNeitherGrowsNorCanBeSownOver() {
        // Given a withered crop
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 3, false, true);

        // When the player tries to sow on its plot
        var result = app.controller.plant(COL, ROW, "CARROT_SEED");

        // Then the plot is occupied
        assertEquals(ErrorCode.PARCELA_OCUPADA, result.code());
        assertEquals(10, app.state().inventory().count(ItemType.CARROT_SEED));

        // And a day later it has not grown
        app.controller.endDay();
        assertEquals(2, cropOf(app).daysGrown());
        assertEquals(CropStage.WITHERED, cropOf(app).stage());
    }

    @Test
    void HU05_aSeedWithoutWaterWithersLikeAnyLivingYoungCrop() {
        TestApp app = TestApp.startedGame();
        assertTrue(app.controller.plant(COL, ROW, "CARROT_SEED").isOk());

        endCloudyDay(app);
        endCloudyDay(app);
        endCloudyDay(app);

        assertEquals(CropStage.WITHERED, cropOf(app).stage());
    }

    @Test
    void HU05_theMementoRestoresACropThatWitheredDuringAFailedDay() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setDate(1, Season.SPRING, 28);
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 4);
        var before = app.state().createMemento();

        app.controller.endDay(); // withers because Summer starts
        assertEquals(CropStage.WITHERED, cropOf(app).stage());
        app.state().restore(before);

        assertEquals(CropStage.MATURE, cropOf(app).stage());
    }
}
