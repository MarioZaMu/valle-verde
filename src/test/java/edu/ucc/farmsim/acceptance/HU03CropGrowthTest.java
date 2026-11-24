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

/** HU-03: los cultivos regados crecen cada día y pasan por Seed, Sprout, Growing y Mature (RN-03, RN-08). */
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
                // Dada una zanahoria (T = 4) recién sembrada y regada
        TestApp app = TestApp.startedGame();
        assertTrue(app.controller.plant(COL, ROW, "CARROT_SEED").isOk());
        assertTrue(app.controller.water(COL, ROW).isOk());

                // Cuando termina el día
        app.controller.endDay();

                // Entonces tiene 1 día de crecimiento, es un Sprout y no ha pasado días sin agua
        Crop crop = cropOf(app);
        assertEquals(1, crop.daysGrown());
        assertEquals(CropStage.SPROUT, crop.stage());
        assertEquals(0, crop.daysWithoutWater());
    }

    @Test
    void HU03_ca2_stageThresholdsFollowRn08ForCarrotAndTomato() {
                // Zanahoria T = 4, mitad = 2: regada todos los días es Growing en d = 2 y Mature en d = 4
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

                // Tomate T = 7, mitad = 4: Sprout en d = 3, Growing en d = 4
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
                // Dado un Sprout (d = 1) que no fue regado
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 1);

                // Cuando termina el día
        app.controller.endDay();

                // Entonces el crecimiento no cambia y el contador de días secos pasa de 0 a 1
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

                // Cuando pasan 5 días sin regarlo
        for (int day = 1; day <= 5; day++) {
            app.controller.endDay();
        }

                // Entonces sigue maduro con d = 4 y no se ha marchitado
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
                // Dado un cultivo regado y un paso que falla después de la fase de crecimiento
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

                // Cuando el día falla
        assertFalse(app.controller.endDay().isOk());

                // Entonces el cultivo queda exactamente como antes: sin crecer, todavía regado
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
