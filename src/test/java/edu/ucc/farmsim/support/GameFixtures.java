package edu.ucc.farmsim.support;

import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.animal.AnimalSnapshot;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropSnapshot;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.Inventory;
import edu.ucc.farmsim.domain.economy.InventorySnapshot;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.economy.MarketSnapshot;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.progress.ProgressSnapshot;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Season;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Helpers that put a game in a known situation without going through the whole day-by-day flow. */
public final class GameFixtures {

    /** A soil plot inside the starting soil block. */
    public static final int SOIL_COL = 5;
    public static final int SOIL_ROW = 3;

    /** Top-left plot of a free grass area where a 2 x 2 or 3 x 2 building fits. */
    public static final int GRASS_COL = 2;
    public static final int GRASS_ROW = 7;

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

    /** Puts a building on the map without paying for it or checking its unlock. */
    public static Building build(GameState state, BuildingType type, int col, int row) {
        return state.farm().place(type, col, row);
    }

    /** Puts a coop on the free grass at the bottom left. */
    public static Building coop(GameState state) {
        return build(state, BuildingType.COOP, GRASS_COL, GRASS_ROW);
    }

    /** Houses an animal in a building in the given situation, bypassing purchase and feeding. */
    public static Animal house(Building building, AnimalType type, AnimalCondition condition, int daysWithoutFeed,
                               boolean fedToday, int pending) {
        Animal animal = Animal.fromSnapshot(new AnimalSnapshot(type, condition, daysWithoutFeed, fedToday, pending));
        if (!building.addAnimal(animal)) {
            throw new IllegalStateException("The building has no room");
        }
        return animal;
    }

    /** Houses a healthy hen that has not eaten. */
    public static Animal hen(Building coop) {
        return house(coop, AnimalType.HEN, AnimalCondition.HEALTHY, 0, false, 0);
    }

    /** Grants an achievement without playing for it. */
    public static void unlock(GameState state, Achievement achievement) {
        GameMemento now = state.createMemento();
        ProgressSnapshot progress = now.progress();
        Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);
        unlocked.addAll(progress.unlocked());
        unlocked.add(achievement);
        if (harvestsFor(achievement) > 1) {
            unlocked.add(Achievement.FIRST_HARVEST);
        }
        if (achievement == Achievement.BIG_HARVESTER) {
            unlocked.add(Achievement.GREEN_THUMB);
        }
        // keep the counters coherent with what is unlocked, as a real game would have them
        ProgressSnapshot coherent = new ProgressSnapshot(unlocked,
                Math.max(progress.harvests(), harvestsFor(achievement)),
                Math.max(progress.unitsSold(), achievement == Achievement.TRADER ? 50 : 0),
                Math.max(progress.buildingsBuilt(), achievement == Achievement.BUILDER ? 1 : 0),
                Math.max(progress.animalsCured(), achievement == Achievement.VETERINARIAN ? 1 : 0));
        state.restore(new GameMemento(now.date(), now.money(), now.energy(), now.inventory(), now.farm(),
                now.today(), now.forecast(), now.randomState(), now.market(), now.ledger(), coherent));
    }

    private static int harvestsFor(Achievement achievement) {
        return switch (achievement) {
            case FIRST_HARVEST -> 1;
            case GREEN_THUMB -> 25;
            case BIG_HARVESTER -> 100;
            default -> 0;
        };
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
        GameMemento now = state.createMemento();
        state.restore(new GameMemento(now.date(), money, now.energy(), now.inventory(), now.farm(), now.today(),
                now.forecast(), now.randomState(), now.market(), now.ledger(), now.progress()));
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

