package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-15: selling at a price that depends on the season and on how much was sold (RN-14, RN-17, RN-18). */
class HU15SellDynamicPriceTest {

    private static int priceOf(TestApp app, ItemType item) {
        return app.state().market().priceOf(item, app.state().date().season());
    }

    @Test
    void HU15_ca1_threeCarrotsInSpringPay79AndTheDemandFallsTo94() {
        // Given Spring, demand 100 and 3 carrots
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);

        // When the player sells them
        Result<Integer> result = app.controller.sell("CARROT", 3);

        // Then the player receives 27 + 26 + 26 = 79 m and the demand is 94
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(79, result.value());
        assertEquals(579, app.state().money());
        assertEquals(94, app.state().market().demandOf(ItemType.CARROT));
        assertEquals(0, app.state().inventory().count(ItemType.CARROT));
    }

    @Test
    void HU15_ca1_aSaleAnnouncesTheNewMoneyAndTheInventoryChange() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);

        app.controller.sell("CARROT", 3);

        assertEquals(java.util.List.of(new GameEvent.MoneyChanged(579), new GameEvent.InventoryChanged()), app.events);
    }

    @Test
    void HU15_ca2_afterTenCarrotsTheNextOnePaysOnly22() {
        // Given 10 carrots already sold today (demand 80)
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 11);
        app.controller.sell("CARROT", 10);
        assertEquals(80, app.state().market().demandOf(ItemType.CARROT));

        // Then the price of the next one is 22 m and not 27 m
        assertEquals(22, priceOf(app, ItemType.CARROT));
    }

    @Test
    void HU15_ca3_theDemandNeverFallsBelow50() {
        // Given demand 50 for carrots in Spring
        TestApp app = TestApp.startedGame();
        GameFixtures.setDemand(app.state(), ItemType.CARROT, 50);
        app.state().inventory().add(ItemType.CARROT, 1);

        // When one more is sold, it pays 14 m (13,5 rounds half up) and the demand stays at 50
        Result<Integer> result = app.controller.sell("CARROT", 1);

        assertEquals(14, result.value());
        assertEquals(50, app.state().market().demandOf(ItemType.CARROT));
    }

    @Test
    void HU15_ca4_endingTheDayRecoversFivePointsUpToACeilingOf100() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setDemand(app.state(), ItemType.CARROT, 80);
        GameFixtures.setDemand(app.state(), ItemType.POTATO, 98);

        app.controller.endDay();

        assertEquals(85, app.state().market().demandOf(ItemType.CARROT));
        assertEquals(100, app.state().market().demandOf(ItemType.POTATO));
    }

    @Test
    void HU15_ca5_theSeasonFactorIs120OutOfSeasonAnd90InSeason() {
        // Given demand 100 for pumpkins
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.PUMPKIN, 2);

        // In Spring (out of season, factor 120) one pumpkin pays 132 m
        assertEquals(132, app.controller.sell("PUMPKIN", 1).value());

        // In Autumn (in season, factor 90) with full demand it pays 99 m
        GameFixtures.setDemand(app.state(), ItemType.PUMPKIN, 100);
        GameFixtures.setDate(1, Season.AUTUMN, 1);
        assertEquals(99, app.controller.sell("PUMPKIN", 1).value());
    }

    @Test
    void HU15_ca6_sellingMoreThanOwnedIsRejectedAndNothingChanges() {
        // Given 2 carrots
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 2);

        // When the player tries to sell 5
        Result<Integer> result = app.controller.sell("CARROT", 5);

        // Then it is rejected and inventory, money and demand are unchanged
        assertFalse(result.isOk());
        assertEquals(ErrorCode.CANTIDAD_INVALIDA, result.code());
        assertEquals("La cantidad debe estar entre 1 y 2.", result.message());
        assertEquals(2, app.state().inventory().count(ItemType.CARROT));
        assertEquals(500, app.state().money());
        assertEquals(100, app.state().market().demandOf(ItemType.CARROT));
        assertTrue(app.events.isEmpty());
    }

    @Test
    void HU15_sellingZeroIsRejectedToo() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 2);

        assertEquals(ErrorCode.CANTIDAD_INVALIDA, app.controller.sell("CARROT", 0).code());
    }

    @Test
    void HU15_eggsAndMilkIgnoreTheSeason() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.EGG, 1);
        app.state().inventory().add(ItemType.MILK, 1);

        assertEquals(20, app.controller.sell("EGG", 1).value());
        assertEquals(60, app.controller.sell("MILK", 1).value());
    }

    @Test
    void HU15_theQuotePredictsTheTotalWithoutSelling() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);

        Result<Integer> quote = app.controller.quote("CARROT", 3);

        assertEquals(79, quote.value());
        assertEquals(100, app.state().market().demandOf(ItemType.CARROT));
        assertEquals(3, app.state().inventory().count(ItemType.CARROT));
        assertEquals(ErrorCode.CANTIDAD_INVALIDA, app.controller.quote("CARROT", 4).code());
    }

    @Test
    void HU15_withoutAGameEveryMarketOrderAnswersSinPartida() {
        TestApp app = TestApp.create();

        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.sell("CARROT", 1).code());
        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.buy("HAY", 1).code());
        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.quote("CARROT", 1).code());
        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.market().code());
        assertEquals(ErrorCode.SIN_PARTIDA, app.controller.inventory().code());
    }

    @Test
    void HU15_aProductThatCannotBeSoldIsAnInternalErrorNotACrash() {
        TestApp app = TestApp.startedGame();

        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.sell("CARROT_SEED", 1).code());
        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.buy("CARROT", 1).code());
    }
}
