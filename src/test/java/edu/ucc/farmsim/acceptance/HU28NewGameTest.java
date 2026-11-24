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
 * HU-28: iniciar una partida nueva con el estado inicial de RN-30.
 *
 * <p>La demanda del mercado (100 para todos los productos) y los logros bloqueados con contadores en 0 se comprueban
 * cuando existan esas clases (HU-15 en la fase 5.3 y HU-26 en la fase 5.5). La confirmación antes de descartar una
 * partida en curso se completa del lado de la UI con {@code HU28_ca3_cancellingTheConfirmation...} en las pruebas de la
 * pantalla de inicio (HU-33).
 */
class HU28NewGameTest {

    @Test
    void HU28_ca1_newGameStartsWithTheInitialValuesOfRn30() {
                // Dado que el jugador elige «Nueva partida»
        TestApp app = TestApp.create();

                // Cuando se crea la partida
        Result<HudView> result = app.controller.newGame(TestApp.SEED);

                // Entonces el estado es Año 1, Primavera, día 1, Sunny, 500 m y 100 de energía
        assertTrue(result.isOk());
        GameState state = app.session.current();
        assertEquals(new GameDate(1, Season.SPRING, 1), state.date());
        assertEquals(Weather.SUNNY, state.weather().today());
        assertEquals(500, state.money());
        assertEquals(100, state.energy().current());

                // Y el inventario tiene 10 semillas de zanahoria y 5 de papa, nada más
        assertEquals(10, state.inventory().count(ItemType.CARROT_SEED));
        assertEquals(5, state.inventory().count(ItemType.POTATO_SEED));
        assertEquals(2, state.inventory().usedSlots());

                // Y no hay edificios, ni animales, los logros están bloqueados y todos los contadores en 0
        assertTrue(state.farm().buildings().isEmpty());
        assertEquals(new edu.ucc.farmsim.domain.progress.ProgressSnapshot(java.util.Set.of(), 0, 0, 0, 0),
                state.progress().snapshot());
        assertTrue(app.controller.achievements().value().stream()
                .noneMatch(edu.ucc.farmsim.application.dto.AchievementView::unlocked));

                // Y la vista que dibuja la pantalla muestra los mismos valores
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
    void HU28_ca2_initialValleyHas1152PlotsWith40Soil976Grass6Water30Obstacles34PathAnd66House() {
                // Cuando se crea una partida nueva
        FarmMap map = GameState.newGame(TestApp.SEED).farm();

                // Entonces el valle es de 48 x 24 = 1152 parcelas con la distribución de RN-04
        assertEquals(48, map.cols());
        assertEquals(24, map.rows());
        assertEquals(40, map.count(Terrain.SOIL));
        assertEquals(976, map.count(Terrain.GRASS));
        assertEquals(6, map.count(Terrain.WATER));
        assertEquals(30, map.count(Terrain.OBSTACLE));
        assertEquals(34, map.count(Terrain.PATH));
        assertEquals(66, map.count(Terrain.HOUSE));
        int total = 0;
        for (Terrain terrain : Terrain.values()) {
            total += map.count(terrain);
        }
        assertEquals(1152, total);

                // Y las 40 parcelas de tierra forman un solo bloque de 8 x 5
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

                // Dada una partida en curso en la que el jugador gastó energía
        GameState before = app.session.current();
        before.energy().trySpend(10);

                // Cuando se le pide confirmar y cancela, la UI simplemente no vuelve a llamar a newGame Entonces la partida
        // actual queda exactamente como estaba
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
                // Dada la misma semilla del generador
        GameState first = GameState.newGame(77L);
        Weather firstForecast = first.weather().forecast();
        GameState second = GameState.newGame(77L);

                // Entonces ambas partidas tienen el mismo pronóstico para el día 2 y el mismo estado aleatorio
        assertEquals(firstForecast, second.weather().forecast());
        assertEquals(first.random().state(), second.random().state());
    }
}
