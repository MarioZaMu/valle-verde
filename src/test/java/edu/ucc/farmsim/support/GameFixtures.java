package edu.ucc.farmsim.support;

import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropSnapshot;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.Inventory;
import edu.ucc.farmsim.domain.economy.InventorySnapshot;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.economy.MarketSnapshot;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Season;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Helpers that put a game in a known situation without going through the whole day-by-day flow. */
public final class GameFixtures {

    /** A soil plot inside the starting soil block. */
    public static final int SOIL_COL = 5;
    public static final int SOIL_ROW = 3;

    private GameFixtures() {
        // static helpers only
    }

    /** Puts a crop with a given progress on a plot, bypassing the sowing rules. */
    public static Crop plant(GameState state, int col, int row, CropType type, int daysGrown,
                             int daysWithoutWater, boolean watered, boolean withered) {
        Crop crop = Crop.fromSnapshot(new CropSnapshot(type, daysGrown, daysWithoutWater, watered, withered));
        state.farm().plotAt(col, row).plant(crop);
        return crop;
    }

    /** Puts a crop that is alive, unwatered and has grown for the given number of days. */
    public static Crop plant(GameState state, int col, int row, CropType type, int daysGrown) {
        return plant(state, col, row, type, daysGrown, 0, false, false);
    }

    /** Sets the demand of a product (percent) without selling anything. */
    public static void setDemand(GameState state, ItemType item, int demand) {
        MarketSnapshot now = state.market().snapshot();
        Map<ItemType, Integer> changed = new EnumMap<>(ItemType.class);
        changed.putAll(now.demand());
        changed.put(item, demand);
        state.market().restoreFrom(new MarketSnapshot(changed, now.previousPrice()));
    }

    /** Sets the player's money. */
    public static void setMoney(GameState state, int money) {
        if (money > state.money()) {
            state.earn(money - state.money());
        } else {
            state.spend(state.money() - money);
        }
    }

    /** Moves the calendar to a date. */
    public static void setDate(int year, Season season, int day) {
        GameClock.getInstance().restore(new GameDate(year, season, day));
    }

    /**
     * Replaces the inventory with one whose first slot holds the given amount of an item and whose remaining
     * slots are all full stacks of hay, so nothing else fits.
     */
    public static void fillInventoryExcept(GameState state, ItemType item, int amount) {
        int capacity = state.inventory().capacity();
        List<Inventory.ItemStack> slots = new ArrayList<>();
        slots.add(new Inventory.ItemStack(item, amount));
        for (int i = 1; i < capacity; i++) {
            slots.add(new Inventory.ItemStack(ItemType.HAY, 99));
        }
        state.inventory().restoreFrom(new InventorySnapshot(capacity, slots));
    }
}

