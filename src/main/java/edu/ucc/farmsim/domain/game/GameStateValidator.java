package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.animal.AnimalSnapshot;
import edu.ucc.farmsim.domain.building.BuildingSnapshot;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.crop.CropSnapshot;
import edu.ucc.farmsim.domain.economy.Inventory;
import edu.ucc.farmsim.domain.economy.Market;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.progress.ProgressFacts;
import edu.ucc.farmsim.domain.progress.ProgressSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks the invariants of RN-33 (step 5) on a game that was read from a save, without any I/O: money, energy,
 * inventory, crops, buildings, animals, market, achievements and the avatar must make sense together. A game that fails
 * is never loaded.
 *
 * <p>Stories: HU-31, HU-45, HU-47.
 */
public final class GameStateValidator {

    private GameStateValidator() {
        // static helpers only
    }

    /**
     * Every invariant the game breaks.
     *
     * @return one Spanish-free technical line per violation; empty if the game is valid
     */
    public static List<String> violations(GameMemento memento) {
        List<String> found = new ArrayList<>();
        if (memento == null) {
            found.add("no game");
            return found;
        }
        if (memento.money() < 0) {
            found.add("money is negative: " + memento.money());
        }
        if (memento.energy() < 0 || memento.energy() > GameRules.MAX_ENERGY) {
            found.add("energy out of range: " + memento.energy());
        }
        if (memento.date() == null || memento.today() == null || memento.forecast() == null) {
            found.add("date or weather missing");
        }
        if (memento.inventory() == null || memento.farm() == null || memento.market() == null
                || memento.ledger() == null || memento.progress() == null) {
            found.add("a part of the game is missing");
            return found;
        }
        checkLedger(memento, found);
        checkBuildingsAndInventory(memento, found);
        checkCrops(memento, found);
        checkMarket(memento, found);
        checkProgress(memento, found);
        return found;
    }

    private static void checkLedger(GameMemento memento, List<String> found) {
        if (memento.ledger().income() < 0 || memento.ledger().expenses() < 0) {
            found.add("the daily ledger is negative");
        }
    }

    private static void checkBuildingsAndInventory(GameMemento memento, List<String> found) {
        FarmMap map = FarmMap.initial();
        int warehouses = 0;
        int index = 0;
        for (BuildingSnapshot building : memento.farm().buildings()) {
            if (building.id() != index++) {
                found.add("building ids are not consecutive");
            }
            if (!map.canPlace(building.type(), building.col(), building.row())) {
                found.add("building outside the map, not on free grass or overlapping: " + building);
                continue;
            }
            map.place(building.type(), building.col(), building.row());
            warehouses += building.type() == BuildingType.WAREHOUSE ? 1 : 0;
            checkAnimals(building, found);
        }
        checkAvatar(memento, map, found);
        checkErrands(memento, found);
        var inventory = memento.inventory();
        int expected = GameRules.INVENTORY_BASE_SLOTS + warehouses * GameRules.WAREHOUSE_EXTRA_SLOTS;
        if (inventory.capacity() != expected || inventory.slots().size() != inventory.capacity()) {
            found.add("inventory capacity " + inventory.capacity() + " does not match the warehouses (" + expected + ")");
            return;
        }
        for (Inventory.ItemStack stack : inventory.slots()) {
            if (stack.amount() < 0 || stack.amount() > GameRules.MAX_STACK
                    || (stack.amount() > 0 && stack.type() == null)) {
                found.add("invalid stack: " + stack);
            }
        }
    }

    /** A save of version 1 has no avatar; otherwise it must stand on a plot it can walk on (RN-36). */
    private static void checkAvatar(GameMemento memento, FarmMap map, List<String> found) {
        var avatar = memento.avatar();
        if (avatar == null) {
            return;
        }
        if (avatar.facing() == null || !map.isWalkable(avatar.col(), avatar.row())) {
            found.add("the avatar is not on a plot it can walk on: " + avatar);
        }
    }

    /** A save written before the errands has none; otherwise no person appears twice or empty (RN-38). */
    private static void checkErrands(GameMemento memento, List<String> found) {
        var done = memento.errandsDone();
        if (done != null && (done.stream().anyMatch(java.util.Objects::isNull)
                || done.stream().distinct().count() != done.size())) {
            found.add("the errands done are not a list of different people: " + done);
        }
    }

    private static void checkAnimals(BuildingSnapshot building, List<String> found) {
        int capacity = building.type().housedAnimal() == null ? 0 : GameRules.ANIMALS_PER_BUILDING;
        if (building.animals().size() > capacity) {
            found.add("more animals than the capacity of " + building.type());
        }
        for (AnimalSnapshot animal : building.animals()) {
            if (animal.type() != building.type().housedAnimal() || animal.condition() == null
                    || animal.daysWithoutFeed() < 0 || animal.pending() < 0
                    || animal.pending() > GameRules.MAX_PENDING_PER_ANIMAL) {
                found.add("invalid animal in " + building.type() + ": " + animal);
            }
        }
    }

    private static void checkCrops(GameMemento memento, List<String> found) {
        FarmMap initial = FarmMap.initial();
        for (BuildingSnapshot building : memento.farm().buildings()) {
            if (initial.canPlace(building.type(), building.col(), building.row())) {
                initial.place(building.type(), building.col(), building.row());
            }
        }
        List<CropSnapshot> crops = memento.farm().crops();
        if (crops.size() != initial.cols() * initial.rows()) {
            found.add("the map has " + crops.size() + " plots");
            return;
        }
        for (int i = 0; i < crops.size(); i++) {
            CropSnapshot crop = crops.get(i);
            if (crop == null) {
                continue;
            }
            if (!initial.plotAt(i % initial.cols(), i / initial.cols()).canBeSown()) {
                found.add("a crop on a plot that is neither soil nor a greenhouse plot: " + i);
            }
            if (crop.type() == null || crop.daysGrown() < 0 || crop.daysWithoutWater() < 0) {
                found.add("invalid crop at plot " + i);
            }
        }
    }

    private static void checkMarket(GameMemento memento, List<String> found) {
        for (ItemType item : Market.sellableItems()) {
            Integer demand = memento.market().demand().get(item);
            if (demand == null || demand < GameRules.DEMAND_MIN || demand > GameRules.DEMAND_MAX) {
                found.add("demand out of range for " + item);
            }
        }
    }

    private static void checkProgress(GameMemento memento, List<String> found) {
        ProgressSnapshot progress = memento.progress();
        if (progress.harvests() < 0 || progress.unitsSold() < 0 || progress.buildingsBuilt() < 0
                || progress.animalsCured() < 0) {
            found.add("a counter is negative");
            return;
        }
        ProgressFacts counters = new ProgressFacts(0, progress.harvests(), progress.unitsSold(),
                progress.buildingsBuilt(), 0, progress.animalsCured(), 0);
        for (Achievement achievement : progress.unlocked()) {
            boolean counted = switch (achievement) {
                case FIRST_HARVEST, GREEN_THUMB, BIG_HARVESTER, TRADER, BUILDER, VETERINARIAN ->
                        achievement.isMetBy(counters);
                case CAPITALIST, BARNYARD_FARMER, ONE_YEAR_IN_THE_VALLEY -> true;
            };
            if (!counted) {
                found.add("achievement " + achievement.code() + " is unlocked but its counter does not allow it");
            }
        }
    }
}
