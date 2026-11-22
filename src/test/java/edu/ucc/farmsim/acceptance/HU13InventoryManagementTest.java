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

/** HU-13: manage the inventory (24 slots, one item type per slot, stacks of up to 99). */
class HU13InventoryManagementTest {

    @Test
    void HU13_ca1_stacksUpTo99PerSlot() {
        // Given an empty inventory of 24 slots
        Inventory inventory = new Inventory(GameRules.INVENTORY_BASE_SLOTS);

        // When 120 carrots are inserted
        boolean inserted = inventory.add(ItemType.CARROT, 120);

        // Then they occupy 2 slots: one of 99 and one of 21
        assertTrue(inserted);
        assertEquals(120, inventory.count(ItemType.CARROT));
        assertEquals(List.of(99, 21), inventory.stacks().stream().map(s -> s.amount()).toList());
    }

    @Test
    void HU13_ca2_insertionIsAtomicWhenItDoesNotFit() {
        // Given a carrot stack of 98 and the other 23 slots full of other items
        Inventory inventory = new Inventory(GameRules.INVENTORY_BASE_SLOTS);
        assertTrue(inventory.add(ItemType.MILK, GameRules.MAX_STACK * 10));
        assertTrue(inventory.add(ItemType.HAY, GameRules.MAX_STACK * 13));
        assertTrue(inventory.add(ItemType.CARROT, 98));
        assertEquals(GameRules.INVENTORY_BASE_SLOTS, inventory.usedSlots());

        // When 3 more carrots are inserted (only 1 fits)
        boolean inserted = inventory.add(ItemType.CARROT, 3);

        // Then it is rejected and the stack stays at 98: not even one carrot is inserted
        assertFalse(inserted);
        assertEquals(98, inventory.count(ItemType.CARROT));
    }

    @Test
    void HU13_ca3_removeTakesOutTheAmountAndRejectsMoreThanExists() {
        // Given 120 carrots
        Inventory inventory = new Inventory(GameRules.INVENTORY_BASE_SLOTS);
        inventory.add(ItemType.CARROT, 120);

        // When 30 are removed, then 90 remain
        assertTrue(inventory.remove(ItemType.CARROT, 30));
        assertEquals(90, inventory.count(ItemType.CARROT));

        // And removing 130 is rejected and leaves the 90 untouched
        assertFalse(inventory.remove(ItemType.CARROT, 130));
        assertEquals(90, inventory.count(ItemType.CARROT));
    }

    @Test
    void HU13_ca4_hasTwentyFourSlotsOneTypePerSlotAndFourteenItemTypes() {
        // Given a new standard inventory
        Inventory inventory = Inventory.standard();

        // Then it has 24 slots and a maximum stack of 99
        assertEquals(24, inventory.capacity());
        assertEquals(99, GameRules.MAX_STACK);

        // And there are 14 item types: 5 seeds, 5 crops, egg, milk, hay and medicine
        assertEquals(14, ItemType.values().length);
        assertEquals(5, Arrays.stream(ItemType.values()).filter(t -> t.kind() == ItemType.Kind.SEED).count());
        assertEquals(5, Arrays.stream(ItemType.values()).filter(t -> t.kind() == ItemType.Kind.CROP).count());

        // And each slot admits a single type: two types never share a slot
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
