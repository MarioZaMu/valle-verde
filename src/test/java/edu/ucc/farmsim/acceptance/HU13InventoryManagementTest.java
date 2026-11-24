package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.Inventory;
import edu.ucc.farmsim.domain.economy.ItemType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-13: administrar el inventario (24 casillas, un tipo de objeto por casilla, pilas de hasta 99). */
class HU13InventoryManagementTest {

    @Test
    void HU13_ca1_stacksUpTo99PerSlot() {
                // Dado un inventario vacío de 24 casillas
        Inventory inventory = new Inventory(GameRules.INVENTORY_BASE_SLOTS);

                // Cuando se insertan 120 zanahorias
        boolean inserted = inventory.add(ItemType.CARROT, 120);

                // Entonces ocupan 2 casillas: una de 99 y una de 21
        assertTrue(inserted);
        assertEquals(120, inventory.count(ItemType.CARROT));
        assertEquals(List.of(99, 21), inventory.stacks().stream().map(s -> s.amount()).toList());
    }

    @Test
    void HU13_ca2_insertionIsAtomicWhenItDoesNotFit() {
                // Dada una pila de 98 zanahorias y las otras 23 casillas llenas de otros objetos
        Inventory inventory = new Inventory(GameRules.INVENTORY_BASE_SLOTS);
        assertTrue(inventory.add(ItemType.MILK, GameRules.MAX_STACK * 10));
        assertTrue(inventory.add(ItemType.HAY, GameRules.MAX_STACK * 13));
        assertTrue(inventory.add(ItemType.CARROT, 98));
        assertEquals(GameRules.INVENTORY_BASE_SLOTS, inventory.usedSlots());

                // Cuando se insertan 3 zanahorias más (solo cabe 1)
        boolean inserted = inventory.add(ItemType.CARROT, 3);

                // Entonces se rechaza y la pila queda en 98: ni siquiera se inserta una zanahoria
        assertFalse(inserted);
        assertEquals(98, inventory.count(ItemType.CARROT));
    }

    @Test
    void HU13_ca3_removeTakesOutTheAmountAndRejectsMoreThanExists() {
        // Given 120 carrots
        Inventory inventory = new Inventory(GameRules.INVENTORY_BASE_SLOTS);
        inventory.add(ItemType.CARROT, 120);

                // Cuando se quitan 30, entonces quedan 90
        assertTrue(inventory.remove(ItemType.CARROT, 30));
        assertEquals(90, inventory.count(ItemType.CARROT));

                // Y quitar 130 se rechaza y deja las 90 intactas
        assertFalse(inventory.remove(ItemType.CARROT, 130));
        assertEquals(90, inventory.count(ItemType.CARROT));
    }

    @Test
    void HU13_ca4_hasTwentyFourSlotsOneTypePerSlotAndFourteenItemTypes() {
        // Given a new standard inventory
        Inventory inventory = Inventory.standard();

                // Entonces tiene 24 casillas y una pila máxima de 99
        assertEquals(24, inventory.capacity());
        assertEquals(99, GameRules.MAX_STACK);

                // Y hay 14 tipos de objeto: 5 semillas, 5 cultivos, huevo, leche, heno y medicina
        assertEquals(14, ItemType.values().length);
        assertEquals(5, Arrays.stream(ItemType.values()).filter(t -> t.kind() == ItemType.Kind.SEED).count());
        assertEquals(5, Arrays.stream(ItemType.values()).filter(t -> t.kind() == ItemType.Kind.CROP).count());

                // Y cada casilla admite un solo tipo: dos tipos nunca comparten casilla
        inventory.add(ItemType.CARROT, 1);
        inventory.add(ItemType.EGG, 1);
        assertEquals(2, inventory.usedSlots());
    }

    @Test
    void HU13_nonPositiveAmountsAreProgrammingErrors() {
        Inventory inventory = Inventory.standard();
        try {
            inventory.add(ItemType.CARROT, 0);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("A non-positive amount must be rejected with IllegalArgumentException");
    }
}
