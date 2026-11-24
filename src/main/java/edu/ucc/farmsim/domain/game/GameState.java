package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.Inventory;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.economy.Market;
import edu.ucc.farmsim.domain.farm.Avatar;
import edu.ucc.farmsim.domain.farm.Direction;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.npc.ErrandBook;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.progress.AchievementTracker;
import edu.ucc.farmsim.domain.progress.ProgressFacts;
import edu.ucc.farmsim.domain.progress.Tutorial;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.StateRandom;
import edu.ucc.farmsim.domain.time.WeatherCycle;
import java.util.ArrayList;
import java.util.List;

/**
 * Pattern: Memento — Role: Originator (creates and restores snapshots of itself).
 *
 * <p>El estado de una partida: dinero, energía, inventario, mapa de la granja, clima y el generador aleatorio, más el
 * calendario compartido ({@link GameClock}). Es la raíz del agregado sobre la que trabaja toda operación. {@link
 * #createMemento()} y {@link #restore(GameMemento)} deshacen un final de día fallido (HU-10) y respaldarán el guardado
 * y la carga (HU-29, HU-30).
 *
 * <p>Stories: HU-01, HU-10, HU-14, HU-15, HU-17, HU-18, HU-26, HU-28, HU-45, HU-47, HU-49.
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
    private final Avatar avatar = Avatar.start();
    private final ErrandBook errands = new ErrandBook();
    private final Tutorial tutorial = new Tutorial();

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
     * Crea el estado inicial de una partida nueva (RN-30). El calendario se reinicia en Año 1, Primavera, día 1; el día
     * 1 es soleado y el pronóstico del día 2 se sortea con el generador de la semilla dada.
     *
     * @param seed semilla del generador aleatorio; la misma semilla siempre da la misma partida
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
     * Construye una partida desde una instantánea leída de un guardado. La instantánea ya debe ser válida (véase {@link
     * GameStateValidator}); el calendario se fija en su fecha.
     */
    public static GameState fromMemento(GameMemento memento) {
        GameState state = newGame(0L);
        state.restore(memento);
        return state;
    }

    /** Dinero que tiene el jugador. */
    public int money() {
        return money;
    }

    /** Agrega dinero (una venta, una recompensa). */
    public void earn(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        money += amount;
        ledger.addIncome(amount);
    }

    /**
     * Toma dinero si alcanza; el saldo nunca baja de cero (RN-14).
     *
     * @return {@code true} si se gastó; {@code false} (y nada cambia) si el saldo es menor
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

    /** Lo que se ganó y se gastó hoy. */
    public DayLedger ledger() {
        return ledger;
    }

    /** Los logros desbloqueados. */
    public AchievementTracker progress() {
        return progress;
    }

    /** Números que miran los logros, tomados de la partida tal como está ahora. */
    public ProgressFacts facts() {
        int animals = farm.buildings().stream().mapToInt(b -> b.animals().size()).sum();
        return new ProgressFacts(money, progress.harvests(), progress.unitsSold(), progress.buildingsBuilt(),
                animals, progress.animalsCured(), date().year());
    }

    /**
     * Desbloquea todos los logros cuya condición se cumple y paga su recompensa (RN-28). Una recompensa puede cumplir
     * otra condición (el saldo), así que se repite hasta que no se desbloquea nada nuevo.
     *
     * @return los logros desbloqueados ahora, en orden
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

    /** El mercado: demanda, precios y tendencias. */
    public Market market() {
        return market;
    }

    /** La energía diaria del jugador. */
    public Energy energy() {
        return energy;
    }

    /** El inventario. */
    public Inventory inventory() {
        return inventory;
    }

    /** El mapa de la granja. */
    public FarmMap farm() {
        return farm;
    }

    /** Clima de hoy y pronóstico de mañana. */
    public WeatherCycle weather() {
        return weather;
    }

    /** El personaje del jugador: dónde está y hacia dónde mira (RN-36). */
    public Avatar avatar() {
        return avatar;
    }

    /**
     * Un paso del avatar (RN-36): gira hacia la dirección y se mueve allí si se puede caminar sobre la parcela. No
     * cuesta energía ni mueve el calendario.
     *
     * @return {@code true} si se movió; {@code false} si el camino estaba bloqueado y solo giró
     */
    public boolean stepAvatar(Direction direction) {
        return avatar.step(direction, farm);
    }

    /** Los encargos de las personas del valle ya hechos (RN-38). */
    public ErrandBook errands() {
        return errands;
    }

    /** Dónde va el jugador en el tutorial de inicio (RN-39). */
    public Tutorial tutorial() {
        return tutorial;
    }

    /** El generador aleatorio de esta partida. */
    public StateRandom random() {
        return random;
    }

    /** Toma una instantánea inmutable de todo lo que cambia durante el juego. */
    public GameMemento createMemento() {
        return new GameMemento(date(), money, energy.current(), inventory.snapshot(),
                farm.snapshot(), weather.today(), weather.forecast(), random.state(), market.snapshot(),
                ledger.snapshot(), progress.snapshot(), avatar.snapshot(), errands.snapshot(), tutorial.snapshot());
    }

    /** Deja el estado exactamente como estaba cuando se tomó la instantánea. */
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
        if (memento.avatar() == null) {
            avatar.returnToStart(farm);
        } else {
            avatar.restoreFrom(memento.avatar());
        }
        errands.restoreFrom(memento.errandsDone());
        tutorial.restoreFrom(memento.tutorial());
    }

    /** Fecha actual del calendario. */
    public GameDate date() {
        return GameClock.getInstance().date();
    }
}
