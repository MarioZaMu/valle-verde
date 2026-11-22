package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Messages;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.game.Energy;
import org.junit.jupiter.api.Test;

/**
 * HU-08: daily energy (RN-02). The end-to-end restoration at the end of the day is verified in HU-10 · CA-2;
 * the sowing flow that spends energy arrives with HU-01.
 */
class HU08EnergyManagementTest {

    @Test
    void HU08_ca1_everyActionConsumesItsCostUntilEnergyIsZero() {
        // Given 100 energy
        Energy energy = new Energy();
        assertEquals(100, energy.current());

        // When 50 sowings (2 each) are paid
        for (int i = 0; i < 50; i++) {
            assertTrue(energy.trySpend(GameRules.ENERGY_COST_SOW));
        }

        // Then energy reaches 0
        assertEquals(0, energy.current());
    }

    @Test
    void HU08_ca2_rejectsAnActionWhenEnergyIsInsufficient() {
        // Given 1 energy
        Energy energy = Energy.withCurrent(1);

        // When an action that costs 2 is attempted
        boolean paid = energy.trySpend(GameRules.ENERGY_COST_SOW);

        // Then it is rejected, energy stays at 1, and the message says what was needed
        assertFalse(paid);
        assertEquals(1, energy.current());
        assertEquals("Energía insuficiente: necesitas 2 y tienes 1.",
                Messages.of(ErrorCode.SIN_ENERGIA, GameRules.ENERGY_COST_SOW, energy.current()));
    }

    @Test
    void HU08_ca3_restoreBringsEnergyBackToTheMaximumAndLeftoversDoNotAccumulate() {
        // Given 37 energy left
        Energy energy = Energy.withCurrent(37);

        // When the day ends (the pipeline step calls restore)
        energy.restore();

        // Then energy is 100, not 137
        assertEquals(GameRules.MAX_ENERGY, energy.current());
    }

    @Test
    void HU08_ca4_zeroCostActionsAreAlwaysAcceptedAndEnergyStaysWithinZeroAndMaximum() {
        // Given 0 energy, a free action (buy, sell, save) is accepted and energy stays at 0
        Energy empty = Energy.withCurrent(0);
        assertTrue(empty.trySpend(0));
        assertEquals(0, empty.current());
        assertFalse(empty.trySpend(1));
        assertEquals(0, empty.current());

        // And energy never goes above the maximum
        Energy full = new Energy();
        full.restore();
        assertEquals(100, full.current());
        assertFalse(full.trySpend(101));
        assertEquals(100, full.current());
        assertThrows(IllegalArgumentException.class, () -> Energy.withCurrent(101));
        assertThrows(IllegalArgumentException.class, () -> Energy.withCurrent(-1));
    }

    @Test
    void HU08_actionCostsMatchBusinessRuleRn02() {
        assertEquals(2, GameRules.ENERGY_COST_SOW);
        assertEquals(1, GameRules.ENERGY_COST_WATER);
        assertEquals(2, GameRules.ENERGY_COST_WATER_IN_DROUGHT);
        assertEquals(2, GameRules.ENERGY_COST_HARVEST);
        assertEquals(1, GameRules.ENERGY_COST_CLEAN);
        assertEquals(1, GameRules.ENERGY_COST_FEED_PER_ANIMAL);
        assertEquals(1, GameRules.ENERGY_COST_COLLECT);
        assertEquals(1, GameRules.ENERGY_COST_CURE);
    }
}
