package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.BuildingOfferView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.TestApp;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-27: buildings are unlocked by achievements (RN-28, RN-29). */
class HU27UnlockBuildingsTest {

    private static List<Boolean> unlocked(TestApp app) {
        return app.controller.buildings().value().catalog().stream().map(BuildingOfferView::unlocked).toList();
    }

    @Test
    void HU27_ca1_aNewGameOnlyHasTheCoopAvailable() {
        TestApp app = TestApp.startedGame();

        assertEquals(List.of(true, false, false, false), unlocked(app));
    }

    @Test
    void HU27_ca2_eachAchievementUnlocksItsBuilding() {
        TestApp app = TestApp.startedGame();

        GameFixtures.unlock(app.state(), Achievement.FIRST_HARVEST);
        assertEquals(List.of(true, false, true, false), unlocked(app));
        GameFixtures.unlock(app.state(), Achievement.GREEN_THUMB);
        assertEquals(List.of(true, true, true, false), unlocked(app));
        GameFixtures.unlock(app.state(), Achievement.CAPITALIST);
        assertEquals(List.of(true, true, true, true), unlocked(app));
    }

    @Test
    void HU27_ca2_harvestingForRealUnlocksTheWarehouse() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW, CropType.CARROT, 4);

        app.controller.harvest(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW);

        assertTrue(app.controller.buildings().value().catalog().get(2).unlocked());
        assertTrue(app.controller.build("WAREHOUSE", 2, 7).isOk());
    }

    @Test
    void HU27_ca3_aLockedBuildingNamesTheAchievementItNeeds() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 3000);

        var greenhouse = app.controller.build("GREENHOUSE", 2, 7);

        assertEquals(ErrorCode.EDIFICIO_BLOQUEADO, greenhouse.code());
        assertEquals("Edificio bloqueado: requiere el logro «Capitalista».", greenhouse.message());
    }

    @Test
    void HU27_ca4_anUnlockedBuildingStaysUnlockedAfterSavingAndLoading() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        GameFixtures.unlock(app.state(), Achievement.GREEN_THUMB);
        app.controller.save(1);
        app.controller.newGame(5L);
        assertFalse(unlocked(app).get(1));

        assertTrue(app.controller.load(1).isOk());

        assertEquals(List.of(true, true, true, false), unlocked(app));
    }
}
