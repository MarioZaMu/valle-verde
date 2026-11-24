package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.AchievementView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-26: logros con recompensa, evaluados después de cada acción exitosa y al final del día (RN-28). */
class HU26AchievementsTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    private static void matureCrop(TestApp app, int col) {
        GameFixtures.plant(app.state(), col, ROW, CropType.CARROT, 4);
    }

    @Test
    void HU26_ca1_theFirstHarvestUnlocksLg01PaysFiftyAndPublishesTheEvent() {
                // Dadas ninguna cosecha y un cultivo maduro
        TestApp app = TestApp.startedGame();
        matureCrop(app, COL);

                // Cuando el jugador lo cosecha
        app.controller.harvest(COL, ROW);

                // Entonces LG-01 se desbloquea, se pagan 50 m y se publicó el evento
        assertTrue(app.state().progress().has(Achievement.FIRST_HARVEST));
        assertEquals(550, app.state().money());
        assertTrue(app.events.contains(new GameEvent.AchievementUnlocked("LG-01", "Primera cosecha", 50)));
        assertEquals(1, app.state().progress().harvests());
    }

    @Test
    void HU26_ca2_anAchievementIsPaidOnlyOnce() {
        TestApp app = TestApp.startedGame();
        matureCrop(app, COL);
        matureCrop(app, COL + 1);
        app.controller.harvest(COL, ROW);
        app.events.clear();

        app.controller.harvest(COL + 1, ROW);

        assertEquals(550, app.state().money());
        assertTrue(app.events.stream().noneMatch(GameEvent.AchievementUnlocked.class::isInstance));
    }

    @Test
    void HU26_ca3_clearingWitheredPlotsDoesNotCountAndThe25thHarvestPays150() {
        TestApp app = TestApp.startedGame();
        var tracker = app.state().progress();
        tracker.unlock(Achievement.FIRST_HARVEST);
        for (int i = 0; i < 24; i++) {
            tracker.recordHarvest();
        }
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 0, false, true);
        app.controller.harvest(COL, ROW); // limpia el cultivo marchito
        assertEquals(24, tracker.harvests());
        assertTrue(!tracker.has(Achievement.GREEN_THUMB));

        matureCrop(app, COL + 1);
        app.controller.harvest(COL + 1, ROW);

        assertTrue(tracker.has(Achievement.GREEN_THUMB));
        assertEquals(500 + 150, app.state().money());
    }

    @Test
    void HU26_ca4_aBalanceOf3000UnlocksLg05AndReachingYearTwoUnlocksLg09() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.PUMPKIN, 40);
        GameFixtures.setMoney(app.state(), 2950);

        app.controller.sell("PUMPKIN", 10);

        assertTrue(app.state().progress().has(Achievement.CAPITALIST));
        assertTrue(app.events.contains(new GameEvent.AchievementUnlocked("LG-05", "Capitalista", 200)));

        GameFixtures.setDate(1, Season.WINTER, 28);
        long before = app.state().money();
        app.controller.endDay();

        assertTrue(app.state().progress().has(Achievement.ONE_YEAR_IN_THE_VALLEY));
        assertEquals(before + 500, app.state().money());
    }

    @Test
    void HU26_ca5_theFirstBuildingFourAnimalsAndTheFirstCureUnlockLg06Lg07AndLg08() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 2000);

        app.controller.build("COOP", 2, 7);
        assertTrue(app.state().progress().has(Achievement.BUILDER));
        long afterBuild = app.state().money();
        for (int i = 0; i < 4; i++) {
            app.controller.buyAnimal("HEN");
        }
        assertTrue(app.state().progress().has(Achievement.BARNYARD_FARMER));
        assertEquals(afterBuild - 4 * 150 + 150, app.state().money());

        Building coop = app.state().farm().buildings().get(0);
        coop.animals().get(0).cure(); // healthy: nothing happens
        GameFixtures.house(app.state().farm().place(edu.ucc.farmsim.domain.building.BuildingType.COOP, 5, 7),
                AnimalType.HEN, AnimalCondition.SICK, 3, false, 0);
        app.state().inventory().add(ItemType.MEDICINE, 1);
        long beforeCure = app.state().money();
        app.controller.cure(1, 0);

        assertTrue(app.state().progress().has(Achievement.VETERINARIAN));
        assertEquals(beforeCure + 80, app.state().money());
    }

    @Test
    void HU26_theSaleOf50UnitsUnlocksTheTrader() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 50);

        app.controller.sell("CARROT", 50);

        assertTrue(app.state().progress().has(Achievement.TRADER));
        assertEquals(50, app.state().progress().unitsSold());
    }

    @Test
    void HU26_theDialogListsEveryAchievementWithItsProgress() {
        TestApp app = TestApp.startedGame();
        app.state().progress().recordHarvest();
        app.state().evaluateAchievements();

        List<AchievementView> list = app.controller.achievements().value();

        assertEquals(9, list.size());
        AchievementView first = list.get(0);
        assertEquals("LG-01", first.code());
        assertTrue(first.unlocked());
        AchievementView second = list.get(1);
        assertEquals("Manos a la tierra", second.name());
        assertEquals(1, second.progress());
        assertEquals(25, second.target());
        assertEquals(50, list.get(3).target());
    }

    @Test
    void HU26_aRewardThatReachesTheBalanceOfLg05ChainsTheNextAchievement() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 2900);
        for (int i = 0; i < 99; i++) {
            app.state().progress().recordHarvest();
        }
        app.state().progress().recordHarvest();

        var unlocked = app.state().evaluateAchievements();

        // LG-01 50 + LG-02 150 + LG-03 400 = 600 -> 3500 >= 3000 -> LG-05
        assertTrue(unlocked.containsAll(List.of(Achievement.FIRST_HARVEST, Achievement.GREEN_THUMB,
                Achievement.BIG_HARVESTER, Achievement.CAPITALIST)));
        assertEquals(2900 + 50 + 150 + 400 + 200, app.state().money());
    }
}
