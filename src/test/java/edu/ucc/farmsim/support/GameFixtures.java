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

/** Ayudantes que dejan una partida en una situación conocida sin pasar por todo el flujo día a día. */
public final class GameFixtures {

    /** Una parcela de tierra dentro del bloque de tierra inicial. */
    public static final int SOIL_COL = 5;
    public static final int SOIL_ROW = 3;

    /** Parcela superior izquierda de un área de pasto libre donde cabe un edificio de 2 x 2 o 3 x 2. */
    public static final int GRASS_COL = 2;
    public static final int GRASS_ROW = 7;

    private GameFixtures() {
                // solo métodos de utilidad estáticos
    }

    /** Pone un cultivo con un progreso dado en una parcela, saltándose las reglas de siembra. */
    public static Crop plant(GameState state, int col, int row, CropType type, int daysGrown,
                             int daysWithoutWater, boolean watered, boolean withered) {
        Crop crop = Crop.fromSnapshot(new CropSnapshot(type, daysGrown, daysWithoutWater, watered, withered));
        state.farm().plotAt(col, row).plant(crop);
        return crop;
    }

    /** Pone un cultivo vivo, sin regar y que ha crecido el número de días dado. */
    public static Crop plant(GameState state, int col, int row, CropType type, int daysGrown) {
        return plant(state, col, row, type, daysGrown, 0, false, false);
    }

    /** Pone un edificio en el mapa sin pagarlo ni comprobar su desbloqueo. */
    public static Building build(GameState state, BuildingType type, int col, int row) {
        return state.farm().place(type, col, row);
    }

    /** Pone un gallinero sobre el pasto libre de abajo a la izquierda. */
    public static Building coop(GameState state) {
        return build(state, BuildingType.COOP, GRASS_COL, GRASS_ROW);
    }

    /** Aloja un animal en un edificio en la situación dada, saltándose la compra y la alimentación. */
    public static Animal house(Building building, AnimalType type, AnimalCondition condition, int daysWithoutFeed,
                               boolean fedToday, int pending) {
        Animal animal = Animal.fromSnapshot(new AnimalSnapshot(type, condition, daysWithoutFeed, fedToday, pending));
        if (!building.addAnimal(animal)) {
            throw new IllegalStateException("The building has no room");
        }
        return animal;
    }

    /** Aloja una gallina sana que no ha comido. */
    public static Animal hen(Building coop) {
        return house(coop, AnimalType.HEN, AnimalCondition.HEALTHY, 0, false, 0);
    }

    /** Concede un logro sin jugar para conseguirlo. */
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
                // mantiene coherentes los contadores con lo que está desbloqueado, como los tendría una partida real
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

    /** Fija la demanda de un producto (porcentaje) sin vender nada. */
    public static void setDemand(GameState state, ItemType item, int demand) {
        MarketSnapshot now = state.market().snapshot();
        Map<ItemType, Integer> changed = new EnumMap<>(ItemType.class);
        changed.putAll(now.demand());
        changed.put(item, demand);
        state.market().restoreFrom(new MarketSnapshot(changed, now.previousPrice()));
    }

    /** Fija el dinero del jugador. */
    public static void setMoney(GameState state, int money) {
        GameMemento now = state.createMemento();
        state.restore(new GameMemento(now.date(), money, now.energy(), now.inventory(), now.farm(), now.today(),
                now.forecast(), now.randomState(), now.market(), now.ledger(), now.progress()));
    }

    /** Mueve el calendario a una fecha. */
    public static void setDate(int year, Season season, int day) {
        GameClock.getInstance().restore(new GameDate(year, season, day));
    }

    /**
     * Reemplaza el inventario por uno cuya primera casilla tiene la cantidad dada de un objeto y cuyas demás casillas
     * son todas pilas llenas de heno, de modo que no cabe nada más.
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

