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

/** HU-15: vender a un precio que depende de la estación y de cuánto se vendió (RN-14, RN-17, RN-18). */
class HU15SellDynamicPriceTest {

    private static int priceOf(TestApp app, ItemType item) {
        return app.state().market().priceOf(item, app.state().date().season());
    }

    @Test
    void HU15_ca1_threeCarrotsInSpringPay79AndTheDemandFallsTo94() {
                // Dada Primavera, demanda 100 y 3 zanahorias
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);

                // Cuando el jugador las vende
        Result<Integer> result = app.controller.sell("CARROT", 3);

                // Entonces el jugador recibe 27 + 26 + 26 = 79 m y la demanda es 94
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

                // Entonces el precio de la siguiente es 22 m y no 27 m
        assertEquals(22, priceOf(app, ItemType.CARROT));
    }

    @Test
    void HU15_ca3_theDemandNeverFallsBelow50() {
                // Dada demanda 50 de zanahorias en Primavera
        TestApp app = TestApp.startedGame();
        GameFixtures.setDemand(app.state(), ItemType.CARROT, 50);
        app.state().inventory().add(ItemType.CARROT, 1);

                // Cuando se vende una más, paga 14 m (13,5 redondea hacia arriba) y la demanda se queda en 50
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
                // Dada demanda 100 de calabazas
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.PUMPKIN, 2);

                // En Primavera (fuera de temporada, factor 120) una calabaza paga 132 m
        assertEquals(132, app.controller.sell("PUMPKIN", 1).value());

                // En Otoño (en temporada, factor 90) con demanda completa paga 99 m
        GameFixtures.setDemand(app.state(), ItemType.PUMPKIN, 100);
        GameFixtures.setDate(1, Season.AUTUMN, 1);
        assertEquals(99, app.controller.sell("PUMPKIN", 1).value());
    }

    @Test
    void HU15_ca6_sellingMoreThanOwnedIsRejectedAndNothingChanges() {
        // Given 2 carrots
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 2);

                // Cuando el jugador intenta vender 5
        Result<Integer> result = app.controller.sell("CARROT", 5);

                // Entonces se rechaza y el inventario, el dinero y la demanda no cambian
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
