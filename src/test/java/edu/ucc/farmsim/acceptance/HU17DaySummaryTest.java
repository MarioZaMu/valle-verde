package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-17: the summary of the day that has just ended (RN-03, RN-14). */
class HU17DaySummaryTest {

    private static DaySummaryView summaryOf(TestApp app) {
        return app.events.stream().filter(GameEvent.DaySummaryReady.class::isInstance)
                .map(e -> ((GameEvent.DaySummaryReady) e).summary()).reduce((first, second) -> second).orElseThrow();
    }

    @Test
    void HU17_ca1_aSaleAndAPurchaseAppearAsIncomeAndExpenses() {
        // Given a day with a sale of 79 m and a purchase of 150 m
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);
        app.controller.buy("CORN_SEED", 5);
        app.controller.sell("CARROT", 3);

        // When the day ends
        app.controller.endDay();

        // Then the summary shows income 79, expenses 150 and the balances
        DaySummaryView summary = summaryOf(app);
        assertEquals(79, summary.income());
        assertEquals(150, summary.expenses());
        assertEquals(500, summary.balanceBefore());
        assertEquals(429, summary.balanceAfter());
        assertEquals(1, summary.day());
        assertEquals("SPRING", summary.season());
    }

    @Test
    void HU17_ca2_aWitheredCropAppearsInTheHighlights() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW, CropType.CARROT, 1, 2, false, false);

        app.controller.endDay();

        assertEquals(List.of("Se marchitó 1 cultivo."), summaryOf(app).highlights());
    }

    @Test
    void HU17_ca2_neglectedAnimalsAndTheirProductsAppearToo() {
        TestApp app = TestApp.startedGame();
        var coop = GameFixtures.coop(app.state());
        GameFixtures.house(coop, edu.ucc.farmsim.domain.animal.AnimalType.HEN,
                edu.ucc.farmsim.domain.animal.AnimalCondition.HUNGRY, 2, false, 0);
        GameFixtures.house(coop, edu.ucc.farmsim.domain.animal.AnimalType.HEN,
                edu.ucc.farmsim.domain.animal.AnimalCondition.HEALTHY, 0, true, 0);

        app.controller.endDay();

        assertEquals(List.of("1 animal enfermó por falta de alimento.", "Los animales produjeron 1 unidad."),
                summaryOf(app).highlights());
    }

    @Test
    void HU17_ca3_aDayWithoutMovementsSaysSinNovedades() {
        TestApp app = TestApp.startedGame();

        app.controller.endDay();

        DaySummaryView summary = summaryOf(app);
        assertEquals(0, summary.income());
        assertEquals(0, summary.expenses());
        assertEquals(List.of("Sin novedades"), summary.highlights());
        assertEquals(500, summary.balanceAfter());
    }

    @Test
    void HU17_ca4_theNextSummaryStartsFromZero() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);
        app.controller.sell("CARROT", 3);
        app.controller.endDay();
        app.events.clear();

        app.controller.endDay();

        DaySummaryView second = summaryOf(app);
        assertEquals(0, second.income());
        assertEquals(0, second.expenses());
        assertEquals(2, second.day());
        assertTrue(app.state().ledger().income() == 0 && app.state().ledger().expenses() == 0);
    }
}
