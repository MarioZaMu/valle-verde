package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucc.farmsim.application.dto.MarketView;
import edu.ucc.farmsim.application.dto.SellOfferView;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.economy.Trend;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-16: the price trend of each product compared with yesterday (RN-19). */
class HU16PriceTrendTest {

    private static Trend trend(TestApp app, ItemType item) {
        return app.state().market().trendOf(item, app.state().date().season());
    }

    @Test
    void HU16_ca1_aNewGameShowsEveryProductAsTheSame() {
        TestApp app = TestApp.startedGame();

        for (ItemType item : edu.ucc.farmsim.domain.economy.Market.sellableItems()) {
            assertEquals(Trend.SAME, trend(app, item), item.name());
        }
    }

    @Test
    void HU16_ca2_priceGoesUpAfterTheDemandRecovers() {
        // Given carrots in Spring with demand 80 (price 22 m)
        TestApp app = TestApp.startedGame();
        GameFixtures.setDemand(app.state(), ItemType.CARROT, 80);
        assertEquals(22, app.state().market().priceOf(ItemType.CARROT, app.state().date().season()));

        // When the day ends (demand 85)
        app.controller.endDay();

        // Then the price is 23 m and the trend is UP
        assertEquals(23, app.state().market().priceOf(ItemType.CARROT, app.state().date().season()));
        assertEquals(Trend.UP, trend(app, ItemType.CARROT));
    }

    @Test
    void HU16_ca3_sellingDuringTheDayMakesThePriceGoDown() {
        // Given yesterday's price 23 m with demand 85
        TestApp app = TestApp.startedGame();
        GameFixtures.setDemand(app.state(), ItemType.CARROT, 80);
        app.controller.endDay();
        app.state().inventory().add(ItemType.CARROT, 10);

        // When 10 units are sold (demand 65, price 18 m)
        app.controller.sell("CARROT", 10);

        // Then the trend is DOWN
        assertEquals(18, app.state().market().priceOf(ItemType.CARROT, app.state().date().season()));
        assertEquals(Trend.DOWN, trend(app, ItemType.CARROT));
    }

    @Test
    void HU16_ca4_withFullDemandBeforeAndAfterTheTrendIsTheSame() {
        TestApp app = TestApp.startedGame();

        app.controller.endDay();

        assertEquals(Trend.SAME, trend(app, ItemType.CARROT));
    }

    @Test
    void HU16_theMarketViewCarriesTheTrendAsAName() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setDemand(app.state(), ItemType.CARROT, 80);
        app.controller.endDay();

        MarketView view = app.controller.market().value();
        SellOfferView carrot = view.sells().stream().filter(o -> o.itemId().equals("CARROT")).findFirst().orElseThrow();

        assertEquals("UP", carrot.trend());
        assertEquals(23, carrot.unitPrice());
        assertEquals("Zanahoria", carrot.name());
        assertEquals(7, view.sells().size());
        assertEquals(7, view.buys().size());
    }

    @Test
    void HU16_theHistorySurvivesAMementoRestore() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setDemand(app.state(), ItemType.CARROT, 80);
        app.controller.endDay();
        var memento = app.state().createMemento();
        app.state().inventory().add(ItemType.CARROT, 10);
        app.controller.sell("CARROT", 10);

        app.state().restore(memento);

        assertEquals(Trend.UP, trend(app, ItemType.CARROT));
        assertEquals(85, app.state().market().demandOf(ItemType.CARROT));
    }

    @Test
    void HU28_ca1_aNewGameStartsEveryProductWithFullDemand() {
        TestApp app = TestApp.startedGame();

        for (ItemType item : edu.ucc.farmsim.domain.economy.Market.sellableItems()) {
            assertEquals(100, app.state().market().demandOf(item), item.name());
        }
    }
}
