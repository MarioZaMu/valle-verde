package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.economy.InventorySnapshot;
import edu.ucc.farmsim.domain.economy.MarketSnapshot;
import edu.ucc.farmsim.domain.farm.AvatarSnapshot;
import edu.ucc.farmsim.domain.farm.FarmMapSnapshot;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.domain.progress.ProgressSnapshot;
import edu.ucc.farmsim.domain.progress.TutorialStep;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Weather;
import java.util.List;

/**
 * Pattern: Memento — Role: Memento (immutable snapshot of the game state).
 *
 * <p>Solo expone datos simples, así que el originador es el único lugar que sabe reconstruir un estado a partir de él.
 * Guarda todo lo que cambia durante el juego: cultivos, edificios y animales, el mercado, el libro diario, los logros,
 * el avatar, los encargos hechos y el paso del tutorial de inicio.
 *
 * <p>Stories: HU-01, HU-10, HU-15, HU-16, HU-17, HU-18, HU-26, HU-45, HU-47, HU-49, HU-50.
 *
 * @param date        fecha del calendario
 * @param money       monedas
 * @param energy      energía disponible
 * @param inventory   contenido del inventario
 * @param farm        cultivos del mapa
 * @param today       clima de hoy
 * @param forecast    pronóstico de mañana
 * @param randomState estado interno del generador aleatorio
 * @param market       demanda y precios anteriores
 * @param ledger       lo que se ganó y se gastó hoy
 * @param progress     los logros desbloqueados y sus contadores
 * @param avatar       dónde está el avatar y hacia dónde mira; {@code null} en una partida de versión 1, que no tenía
 *                     avatar
 * @param errandsDone  las personas cuyo encargo está hecho; {@code null} en una partida escrita antes de los encargos
 * @param tutorial     el paso del tutorial de inicio; {@code null} en una partida escrita antes de él (se carga
 *                     terminado)
 */
public record GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory,
                          FarmMapSnapshot farm, Weather today, Weather forecast, long randomState,
                          MarketSnapshot market, LedgerSnapshot ledger, ProgressSnapshot progress,
                          AvatarSnapshot avatar, List<Npc> errandsDone, TutorialStep tutorial) {

    /** Una instantánea con el avatar y los encargos pero sin tutorial. */
    public GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory, FarmMapSnapshot farm,
                       Weather today, Weather forecast, long randomState, MarketSnapshot market,
                       LedgerSnapshot ledger, ProgressSnapshot progress, AvatarSnapshot avatar, List<Npc> errandsDone) {
        this(date, money, energy, inventory, farm, today, forecast, randomState, market, ledger, progress, avatar,
                errandsDone, null);
    }

    /** Una instantánea con el avatar pero sin encargos hechos. */
    public GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory, FarmMapSnapshot farm,
                       Weather today, Weather forecast, long randomState, MarketSnapshot market,
                       LedgerSnapshot ledger, ProgressSnapshot progress, AvatarSnapshot avatar) {
        this(date, money, energy, inventory, farm, today, forecast, randomState, market, ledger, progress, avatar,
                (List<Npc>) null);
    }

    /** Una instantánea sin avatar, como la guarda una partida de versión 1 (el avatar vuelve a su inicio). */
    public GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory, FarmMapSnapshot farm,
                       Weather today, Weather forecast, long randomState, MarketSnapshot market,
                       LedgerSnapshot ledger, ProgressSnapshot progress) {
        this(date, money, energy, inventory, farm, today, forecast, randomState, market, ledger, progress, null);
    }

    /** La misma instantánea con otro mapa (una partida del antiguo mapa de 16 x 10 ampliada al valle, RN-40). */
    public GameMemento withFarm(FarmMapSnapshot other) {
        return new GameMemento(date, money, energy, inventory, other, today, forecast, randomState, market, ledger,
                progress, avatar, errandsDone, tutorial);
    }
}
