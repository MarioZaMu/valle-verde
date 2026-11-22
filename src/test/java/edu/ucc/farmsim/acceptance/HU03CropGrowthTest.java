package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropStage;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.game.DayPhase;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.DayReport;
import edu.ucc.farmsim.domain.game.DayStep;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-03: watered crops grow every day and move through Seed, Sprout, Growing and Mature (RN-03, RN-08). */
class HU03CropGrowthTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    private static Crop cropOf(TestApp app) {
        return app.state().farm().plotAt(COL, ROW).crop();
    }

    private static void waterAndEndDay(TestApp app) {
        app.controller.water(COL, ROW);
        assertTrue(app.controller.endDay().isOk());
    }

    @Test
    void HU03_ca1_aWateredCropGrowsOneDayAndLeavesTheSeedStage() {
        // Given a carrot (T = 4) just sown and watered
        TestApp app = TestApp.startedGame();
        assertTrue(app.controller.plant(COL, ROW, "CARROT_SEED").isOk());
        assertTrue(app.controller.water(COL, ROW).isOk());

        // When the day ends
        app.controller.endDay();

        // Then it has 1 day of growth, it is a Sprout and it has not gone without water
        Crop crop = cropOf(app);
        assertEquals(1, crop.daysGrown());
        assertEquals(CropStage.SPROUT, crop.stage());
        assertEquals(0, crop.daysWithoutWater());
    }

    @Test
    void HU03_ca2_stageThresholdsFollowRn08ForCarrotAndTomato() {
        // Carrot T = 4, halfway = 2: watered every day it is Growing at d = 2 and Mature at d = 4
        TestApp carrot = TestApp.startedGame();
        carrot.controller.plant(COL, ROW, "CARROT_SEED");
        waterAndEndDay(carrot);
        assertEquals(CropStage.SPROUT, cropOf(carrot).stage());
        waterAndEndDay(carrot);
        assertEquals(2, cropOf(carrot).daysGrown());
        assertEquals(CropStage.GROWING, cropOf(carrot).stage());
        waterAndEndDay(carrot);
        assertEquals(CropStage.GROWING, cropOf(carrot).stage());
        waterAndEndDay(carrot);
        assertEquals(4, cropOf(carrot).daysGrown());
        assertEquals(CropStage.MATURE, cropOf(carrot).stage());

        // Tomato T = 7, halfway = 4: Sprout at d = 3, Growing at d = 4
        TestApp tomato = TestApp.startedGame();
        GameFixtures.plant(tomato.state(), COL, ROW, CropType.TOMATO, 0);
        for (int day = 1; day <= 3; day++) {
            waterAndEndDay(tomato);
        }
        assertEquals(CropStage.SPROUT, cropOf(tomato).stage());
        waterAndEndDay(tomato);
        assertEquals(4, cropOf(tomato).daysGrown());
        assertEquals(CropStage.GROWING, cropOf(tomato).stage());
    }

    @Test
    void HU03_ca3_anUnwateredCropDoesNotGrowAndCountsADayWithoutWater() {
        // Given a Sprout (d = 1) that was not watered
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 1);

        // When the day ends
        app.controller.endDay();

        // Then growth does not change and the dry-day counter goes from 0 to 1
        Crop crop = cropOf(app);
        assertEquals(1, crop.daysGrown());
        assertEquals(CropStage.SPROUT, crop.stage());
        assertEquals(1, crop.daysWithoutWater());
    }

    @Test
    void HU03_ca4_aMatureCropNoLongerGrowsOrNeedsWater() {
        // Given a mature carrot (d = 4)
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 4);

        // When 5 days pass without watering it
        for (int day = 1; day <= 5; day++) {
            app.controller.endDay();
        }

        // Then it is still mature with d = 4 and it has not withered
        Crop crop = cropOf(app);
        assertEquals(4, crop.daysGrown());
        assertEquals(CropStage.MATURE, crop.stage());
        assertTrue(crop.isAlive());
    }

    @Test
    void HU10_ca2_endingTheDayClearsTheWateredMarkOfEveryCrop() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 0);
        GameFixtures.plant(app.state(), COL + 1, ROW, CropType.POTATO, 0);
        app.controller.water(COL, ROW);
        app.controller.water(COL + 1, ROW);

        app.controller.endDay();

        assertFalse(cropOf(app).isWatered());
        assertFalse(app.state().farm().plotAt(COL + 1, ROW).crop().isWatered());
    }

    @Test
    void HU10_ca4_aFailureAfterGrowthRestoresTheProgressOfTheCrops() {
        // Given a watered crop and a step that fails after the growth phase
        DayStep failing = new DayStep() {
            @Override
            public DayPhase phase() {
                return DayPhase.ENERGY;
            }

            @Override
            public void apply(GameState state, DayReport report) {
                throw new IllegalStateException("boom");
            }
        };
        TestApp app = TestApp.create(DayPipeline.standard().add(failing));
        app.controller.newGame(TestApp.SEED);
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 1);
        app.controller.water(COL, ROW);
        GameMemento before = app.state().createMemento();

        // When the day fails
        assertFalse(app.controller.endDay().isOk());

        // Then the crop is exactly as before: not grown, still watered
        assertEquals(before, app.state().createMemento());
        assertEquals(1, cropOf(app).daysGrown());
        assertTrue(cropOf(app).isWatered());
    }

    @Test
    void HU03_theMementoKeepsTheProgressOfCropsAcrossDays() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.POTATO, 2, 1, true, false);
        GameMemento saved = app.state().createMemento();

        app.controller.endDay();
        app.controller.endDay();
        app.state().restore(saved);

        Crop crop = cropOf(app);
        assertEquals(2, crop.daysGrown());
        assertEquals(1, crop.daysWithoutWater());
        assertTrue(crop.isWatered());
    }
}
