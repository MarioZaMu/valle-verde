package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-14: comprar semillas, heno y medicina a precios fijos (RN-14, RN-15, RN-16). */
class HU14BuyTest {

    @Test
    void HU14_ca1_buyingFiveCornSeedsCosts150() {
        // Given 500 m
        TestApp app = TestApp.startedGame();

                // Cuando el jugador compra 5 semillas de maíz (30 m cada una)
        Result<Integer> result = app.controller.buy("CORN_SEED", 5);

                // Entonces el saldo es 350 m y hay 5 semillas más
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(150, result.value());
        assertEquals(350, app.state().money());
        assertEquals(5, app.state().inventory().count(ItemType.CORN_SEED));
    }

    @Test
    void HU14_ca2_notEnoughMoneyIsRejectedWithTheMissingAmountAndNothingChanges() {
        // Given 100 m
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 100);

                // Cuando el jugador intenta comprar 3 medicinas (120 m)
        Result<Integer> result = app.controller.buy("MEDICINE", 3);

                // Entonces se rechaza: faltan 20 m
        assertFalse(result.isOk());
        assertEquals(ErrorCode.SIN_FONDOS, result.code());
        assertEquals("Fondos insuficientes: faltan 20 m.", result.message());
        assertEquals(100, app.state().money());
        assertEquals(0, app.state().inventory().count(ItemType.MEDICINE));
        assertTrue(app.events.isEmpty());
    }

    @Test
    void HU14_ca3_withoutRoomInTheInventoryTheMoneyIsNotTaken() {
                // Dado un inventario sin lugar para 10 de heno
        TestApp app = TestApp.startedGame();
        GameFixtures.fillInventoryExcept(app.state(), ItemType.CARROT, 5);

                // Cuando el jugador intenta comprarlos
        Result<Integer> result = app.controller.buy("MEDICINE", 10);

                // Entonces se rechaza y el saldo es el mismo
        assertEquals(ErrorCode.INVENTARIO_LLENO, result.code());
        assertEquals(500, app.state().money());
    }

    @Test
    void HU14_ca4_theQuantityMustBeBetween1And99() {
        TestApp app = TestApp.startedGame();

        Result<Integer> zero = app.controller.buy("HAY", 0);
        Result<Integer> hundred = app.controller.buy("HAY", 100);

        assertEquals(ErrorCode.CANTIDAD_INVALIDA, zero.code());
        assertEquals("La cantidad debe estar entre 1 y 99.", zero.message());
        assertEquals(ErrorCode.CANTIDAD_INVALIDA, hundred.code());
        assertEquals(500, app.state().money());
    }

    @Test
    void HU14_ca4_pumpkinSeedsCanBeBoughtInSpringEvenThoughTheyCannotBeSownThen() {
        TestApp app = TestApp.startedGame();

        Result<Integer> result = app.controller.buy("PUMPKIN_SEED", 1);

        assertTrue(result.isOk());
        assertEquals(40, result.value());
        assertEquals(1, app.state().inventory().count(ItemType.PUMPKIN_SEED));
    }

    @Test
    void HU14_everyCatalogPriceMatchesRn16() {
        assertEquals(10, edu.ucc.farmsim.domain.economy.Market.buyPrice(ItemType.CARROT_SEED));
        assertEquals(20, edu.ucc.farmsim.domain.economy.Market.buyPrice(ItemType.POTATO_SEED));
        assertEquals(25, edu.ucc.farmsim.domain.economy.Market.buyPrice(ItemType.TOMATO_SEED));
        assertEquals(30, edu.ucc.farmsim.domain.economy.Market.buyPrice(ItemType.CORN_SEED));
        assertEquals(40, edu.ucc.farmsim.domain.economy.Market.buyPrice(ItemType.PUMPKIN_SEED));
        assertEquals(5, edu.ucc.farmsim.domain.economy.Market.buyPrice(ItemType.HAY));
        assertEquals(40, edu.ucc.farmsim.domain.economy.Market.buyPrice(ItemType.MEDICINE));
    }

    @Test
    void HU14_buyingDoesNotChangeTheDemandOfAnyProduct() {
        TestApp app = TestApp.startedGame();

        app.controller.buy("CARROT_SEED", 5);

        assertEquals(100, app.state().market().demandOf(ItemType.CARROT));
    }
}
