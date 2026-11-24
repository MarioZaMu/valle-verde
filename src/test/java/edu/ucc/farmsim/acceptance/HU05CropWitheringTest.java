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

/** HU-05: los cultivos se marchitan por falta de agua y fuera de estación (RN-01, RN-03, RN-09). */
class HU05CropWitheringTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    /**
     * Termina el día con un día Cloudy, de modo que el clima aleatorio (la lluvia regaría los cultivos) no interfiera.
     */
    private static void endCloudyDay(TestApp app) {
        app.state().weather().restore(Weather.CLOUDY, Weather.CLOUDY);
        app.controller.endDay();
    }

    private static Crop cropOf(TestApp app) {
        return app.state().farm().plotAt(COL, ROW).crop();
    }

    @Test
    void HU05_ca1_aCropWithersAfterThreeDaysWithoutWater() {
                // Dado un cultivo en la etapa Growing
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2);
        assertEquals(CropStage.GROWING, cropOf(app).stage());

                // Cuando pasan 2 días sin regarlo, sigue vivo con 2 días secos
        endCloudyDay(app);
        endCloudyDay(app);
        assertTrue(cropOf(app).isAlive());
        assertEquals(2, cropOf(app).daysWithoutWater());

                // Y cuando pasa el tercer día, se marchita
        endCloudyDay(app);
        assertEquals(CropStage.WITHERED, cropOf(app).stage());
    }

    @Test
    void HU05_ca2_inADroughtACropWithersAfterJustTwoDryDays() {
                // Dado un cultivo con 1 día sin agua y que termina un día de Drought
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 1, false, false);
        app.state().weather().restore(Weather.DROUGHT, Weather.SUNNY);

                // Cuando termina el día
        app.controller.endDay();

                // Entonces tiene 2 días secos y se ha marchitado (la tolerancia es 2 en una sequía)
        assertEquals(2, cropOf(app).daysWithoutWater());
        assertEquals(CropStage.WITHERED, cropOf(app).stage());
    }

    @Test
    void HU05_ca3_cropsOutOfTheNewSeasonWitherEvenIfMature() {
                // Dada una zanahoria madura (válida solo en Primavera) viva el día 28 de Primavera
        TestApp app = TestApp.startedGame();
        GameFixtures.setDate(1, Season.SPRING, 28);
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 4);

                // Cuando termina el día y empieza el Verano, se marchita
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
                // Dado un cultivo con 2 días secos
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 2, false, false);

                // Cuando se riega y termina el día
        assertTrue(app.controller.water(COL, ROW).isOk());
        app.controller.endDay();

                // Entonces el contador es 0 y el cultivo no se marchitó
        assertEquals(0, cropOf(app).daysWithoutWater());
        assertTrue(cropOf(app).isAlive());
        assertEquals(3, cropOf(app).daysGrown());
    }

    @Test
    void HU05_ca5_aWitheredCropKeepsOccupyingThePlotAndNeitherGrowsNorCanBeSownOver() {
        // Given a withered crop
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 3, false, true);

                // Cuando el jugador intenta sembrar en su parcela
        var result = app.controller.plant(COL, ROW, "CARROT_SEED");

                // Entonces la parcela está ocupada
        assertEquals(ErrorCode.PARCELA_OCUPADA, result.code());
        assertEquals(10, app.state().inventory().count(ItemType.CARROT_SEED));

                // Y un día después no ha crecido
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
