package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.Inventory;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.StateRandom;
import edu.ucc.farmsim.domain.time.WeatherCycle;

/**
 * The state of one game: money, energy, inventory, farm map, weather and the random generator, plus the
 * shared calendar ({@link GameClock}). It is the aggregate root that every operation works on.
 *
 * <p>Stories: HU-28.
 */
public final class GameState {

    private int money;
    private final Energy energy;
    private final Inventory inventory;
    private final FarmMap farm;
    private final WeatherCycle weather;
    private final StateRandom random;

    private GameState(int money, Energy energy, Inventory inventory, FarmMap farm,
                      WeatherCycle weather, StateRandom random) {
        this.money = money;
        this.energy = energy;
        this.inventory = inventory;
        this.farm = farm;
        this.weather = weather;
        this.random = random;
    }

    /**
     * Creates the initial state of a new game (RN-30). The calendar is reset to Year 1, Spring, day 1,
     * day 1 is Sunny and the forecast of day 2 is drawn with the generator of the given seed.
     *
     * @param seed seed of the random generator; the same seed always gives the same game
     */
    public static GameState newGame(long seed) {
        GameClock clock = GameClock.getInstance();
        clock.restore(GameDate.start());
        StateRandom random = new StateRandom(seed);
        Inventory inventory = Inventory.standard();
        inventory.add(ItemType.CARROT_SEED, GameRules.START_CARROT_SEEDS);
        inventory.add(ItemType.POTATO_SEED, GameRules.START_POTATO_SEEDS);
        return new GameState(GameRules.START_MONEY, new Energy(), inventory, FarmMap.initial(),
                WeatherCycle.startNewGame(clock, random), random);
    }

    /** Money the player has. */
    public int money() {
        return money;
    }

    /** The player's daily energy. */
    public Energy energy() {
        return energy;
    }

    /** The inventory. */
    public Inventory inventory() {
        return inventory;
    }

    /** The farm map. */
    public FarmMap farm() {
        return farm;
    }

    /** Weather of today and forecast of tomorrow. */
    public WeatherCycle weather() {
        return weather;
    }

    /** The random generator of this game. */
    public StateRandom random() {
        return random;
    }

    /** Current calendar date. */
    public GameDate date() {
        return GameClock.getInstance().date();
    }
}
