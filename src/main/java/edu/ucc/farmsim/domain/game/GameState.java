package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.Inventory;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.economy.Market;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.progress.AchievementTracker;
import edu.ucc.farmsim.domain.progress.ProgressFacts;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.StateRandom;
import edu.ucc.farmsim.domain.time.WeatherCycle;
import java.util.ArrayList;
import java.util.List;

/**
 * Pattern: Memento — Role: Originator (creates and restores snapshots of itself).
 *
 * <p>The state of one game: money, energy, inventory, farm map, weather and the random generator, plus the
 * shared calendar ({@link GameClock}). It is the aggregate root that every operation works on.
 * {@link #createMemento()} and {@link #restore(GameMemento)} undo a failed end of the day (HU-10) and
 * will back saving and loading (HU-29, HU-30).
 *
 * <p>Stories: HU-01, HU-10, HU-14, HU-15, HU-17, HU-18, HU-26, HU-28.
 */
public final class GameState {

    private int money;
    private final Energy energy;
    private final Inventory inventory;
    private final FarmMap farm;
    private final WeatherCycle weather;
    private final StateRandom random;
    private final Market market;
    private final DayLedger ledger = new DayLedger();
    private final AchievementTracker progress = new AchievementTracker();

    private GameState(int money, Energy energy, Inventory inventory, FarmMap farm,
                      WeatherCycle weather, StateRandom random, Market market) {
        this.money = money;
        this.energy = energy;
        this.inventory = inventory;
        this.farm = farm;
        this.weather = weather;
        this.random = random;
        this.market = market;
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
                WeatherCycle.startNewGame(clock, random), random, Market.standard());
    }

    /**
     * Builds a game from a snapshot read from a save. The snapshot must already be valid (see
     * {@link GameStateValidator}); the calendar is set to its date.
     */
    public static GameState fromMemento(GameMemento memento) {
        GameState state = newGame(0L);
        state.restore(memento);
        return state;
    }

    /** Money the player has. */
    public int money() {
        return money;
    }

    /** Adds money (a sale, a reward). */
    public void earn(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        money += amount;
        ledger.addIncome(amount);
    }

    /**
     * Takes money if there is enough; the balance never goes below zero (RN-14).
     *
     * @return {@code true} if it was spent; {@code false} (and nothing changed) if the balance is smaller
     */
    public boolean spend(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        if (amount > money) {
            return false;
        }
        money -= amount;
        ledger.addExpense(amount);
        return true;
    }

    /** What was earned and spent today. */
    public DayLedger ledger() {
        return ledger;
    }

    /** The achievements unlocked. */
    public AchievementTracker progress() {
        return progress;
    }

    /** Numbers the achievements look at, taken from the game as it is now. */
    public ProgressFacts facts() {
        int animals = farm.buildings().stream().mapToInt(b -> b.animals().size()).sum();
        return new ProgressFacts(money, progress.harvests(), progress.unitsSold(), progress.buildingsBuilt(),
                animals, progress.animalsCured(), date().year());
    }

    /**
     * Unlocks every achievement whose condition is met and pays its reward (RN-28). A reward can itself meet
     * another condition (the balance), so it repeats until nothing new unlocks.
     *
     * @return the achievements unlocked now, in order
     */
    public List<Achievement> evaluateAchievements() {
        List<Achievement> all = new ArrayList<>();
        List<Achievement> fresh = progress.evaluate(facts());
        while (!fresh.isEmpty()) {
            for (Achievement achievement : fresh) {
                earn(achievement.reward());
            }
            all.addAll(fresh);
            fresh = progress.evaluate(facts());
        }
        return all;
    }

    /** The market: demand, prices and trends. */
    public Market market() {
        return market;
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

    /** Takes an immutable snapshot of everything that changes during play. */
    public GameMemento createMemento() {
        return new GameMemento(date(), money, energy.current(), inventory.snapshot(),
                farm.snapshot(), weather.today(), weather.forecast(), random.state(), market.snapshot(),
                ledger.snapshot(), progress.snapshot());
    }

    /** Puts the state back exactly as it was when the snapshot was taken. */
    public void restore(GameMemento memento) {
        GameClock.getInstance().restore(memento.date());
        this.money = memento.money();
        energy.set(memento.energy());
        inventory.restoreFrom(memento.inventory());
        farm.restoreFrom(memento.farm());
        weather.restore(memento.today(), memento.forecast());
        random.restore(memento.randomState());
        market.restoreFrom(memento.market());
        ledger.restoreFrom(memento.ledger());
        progress.restoreFrom(memento.progress());
    }

    /** Current calendar date. */
    public GameDate date() {
        return GameClock.getInstance().date();
    }
}
