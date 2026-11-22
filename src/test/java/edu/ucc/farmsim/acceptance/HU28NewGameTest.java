package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.farm.Terrain;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.Weather;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/**
 * HU-28: start a new game with the initial state of RN-30.
 *
 * <p>The market demand (100 for every product) and the locked achievements with counters at 0 are asserted
 * when those classes exist (HU-15 in phase 5.3 and HU-26 in phase 5.5). The confirmation before discarding
 * a game in progress is completed on the UI side by {@code HU28_ca3_cancellingTheConfirmation...} in the
 * start-screen tests (HU-33).
 */
class HU28NewGameTest {

    @Test
    void HU28_ca1_newGameStartsWithTheInitialValuesOfRn30() {
        // Given the player chooses "New game"
        TestApp app = TestApp.create();

        // When the game is created
        Result<HudView> result = app.controller.newGame(TestApp.SEED);

        // Then the state is Year 1, Spring, day 1, Sunny, 500 m and 100 energy
        assertTrue(result.isOk());
        GameState state = app.session.current();
        assertEquals(new GameDate(1, Season.SPRING, 1), state.date());
        assertEquals(Weather.SUNNY, state.weather().today());
        assertEquals(500, state.money());
        assertEquals(100, state.energy().current());

        // And the inventory holds 10 carrot seeds and 5 potato seeds, nothing else
        assertEquals(10, state.inventory().count(ItemType.CARROT_SEED));
        assertEquals(5, state.inventory().count(ItemType.POTATO_SEED));
        assertEquals(2, state.inventory().usedSlots());

        // And no buildings, no animals, locked achievements and every counter at 0
        assertTrue(state.farm().buildings().isEmpty());
        assertEquals(new edu.ucc.farmsim.domain.progress.ProgressSnapshot(java.util.Set.of(), 0, 0, 0, 0),
                state.progress().snapshot());
        assertTrue(app.controller.achievements().value().stream()
                .noneMatch(edu.ucc.farmsim.application.dto.AchievementView::unlocked));

        // And the view the screen draws shows the same values
        HudView hud = result.value();
        assertEquals(500, hud.money());
        assertEquals(1, hud.year());
        assertEquals("SPRING", hud.season());
        assertEquals(1, hud.day());
        assertEquals("SUNNY", hud.weather());
        assertEquals(100, hud.energy());
        assertEquals(100, hud.maxEnergy());
    }

    @Test
    void HU28_ca2_initialMapHas160PlotsWith40Soil102Grass6WaterAnd12Obstacles() {
        // When a new game is created
        FarmMap map = GameState.newGame(TestApp.SEED).farm();

        // Then the map is 16 x 10 = 160 plots with the RN-04 distribution
        assertEquals(16, map.cols());
        assertEquals(10, map.rows());
        assertEquals(40, map.count(Terrain.SOIL));
        assertEquals(102, map.count(Terrain.GRASS));
        assertEquals(6, map.count(Terrain.WATER));
        assertEquals(12, map.count(Terrain.OBSTACLE));
        assertEquals(160, map.count(Terrain.SOIL) + map.count(Terrain.GRASS)
                + map.count(Terrain.WATER) + map.count(Terrain.OBSTACLE));

        // And the 40 soil plots form one 8 x 5 block
        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.cols(); col++) {
                boolean insideBlock = col >= GameRules.SOIL_COL && col < GameRules.SOIL_COL + GameRules.SOIL_WIDTH
                        && row >= GameRules.SOIL_ROW && row < GameRules.SOIL_ROW + GameRules.SOIL_HEIGHT;
                assertEquals(insideBlock, map.plotAt(col, row).terrain() == Terrain.SOIL, "plot " + col + "," + row);
            }
        }
    }

    @Test
    void HU28_ca3_aGameInProgressIsReportedSoTheUiCanAskForConfirmationAndCancellingKeepsIt() {
        TestApp app = TestApp.create();
        assertFalse(app.controller.hasGameInProgress());
        app.controller.newGame(TestApp.SEED);
        assertTrue(app.controller.hasGameInProgress());

        // Given a game in progress where the player has spent energy
        GameState before = app.session.current();
        before.energy().trySpend(10);

        // When the player is asked to confirm and cancels, the UI simply does not call newGame again
        // Then the current game stays exactly as it was
        assertTrue(app.controller.hasGameInProgress());
        assertEquals(90, app.session.current().energy().current());
    }

    @Test
    void HU28_ca3_confirmingReplacesTheGameInProgressWithAFreshOne() {
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        app.session.current().energy().trySpend(10);

        app.controller.newGame(TestApp.SEED);

        assertEquals(100, app.session.current().energy().current());
    }

    @Test
    void HU28_ca4_sameSeedGivesTheSameInitialGameAndForecast() {
        // Given the same generator seed
        GameState first = GameState.newGame(77L);
        Weather firstForecast = first.weather().forecast();
        GameState second = GameState.newGame(77L);

        // Then both games have the same forecast for day 2 and the same random state
        assertEquals(firstForecast, second.weather().forecast());
        assertEquals(first.random().state(), second.random().state());
    }
}
