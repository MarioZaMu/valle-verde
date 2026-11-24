package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.AnimalOfferView;
import edu.ucc.farmsim.application.dto.BuyOfferView;
import edu.ucc.farmsim.application.dto.InventoryView;
import edu.ucc.farmsim.application.dto.MarketView;
import edu.ucc.farmsim.application.dto.SellOfferView;
import edu.ucc.farmsim.application.dto.SlotView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.Inventory;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.economy.Market;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.Season;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Los casos de uso de economía: comprar a precios fijos (RN-16), vender al precio dinámico (RN-17, RN-18), el total
 * previsto de una venta, y las vistas del mercado y el inventario. Todo rechazo deja la partida intacta.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-19, HU-26, HU-37, HU-38.
 */
@Service
public final class MarketService {

    private final GameSession session;
    private final EventBus events;
    private final AchievementService achievements;

    /**
     * Crea el servicio.
     *
     * @param session la partida en curso
     * @param events  donde se publican los eventos
     * @param achievements evalúa los logros después de cada venta
     */
    public MarketService(GameSession session, EventBus events, AchievementService achievements) {
        this.session = session;
        this.events = events;
        this.achievements = achievements;
    }

    /**
     * Compra unidades al precio fijo: la cantidad debe ser de 1 a 99, el jugador debe poder pagar el total y las
     * unidades deben caber en el inventario (RN-14, RN-15, RN-16).
     *
     * @return el total pagado en m
     */
    public Result<Integer> buy(String itemId, int quantity) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        ItemType item = ItemType.valueOf(itemId);
        if (!Market.isPurchasable(item)) {
            throw new IllegalArgumentException("Not purchasable: " + itemId);
        }
        if (quantity < GameRules.MIN_TRANSACTION || quantity > GameRules.MAX_TRANSACTION) {
            return Result.failWith(ErrorCode.CANTIDAD_INVALIDA, GameRules.MAX_TRANSACTION);
        }
        GameState state = session.current();
        int total = Market.buyPrice(item) * quantity;
        if (total > state.money()) {
            return Result.failWith(ErrorCode.SIN_FONDOS, total - state.money());
        }
        if (!state.inventory().add(item, quantity)) {
            return Result.failWith(ErrorCode.INVENTARIO_LLENO);
        }
        state.spend(total);
        events.publish(new GameEvent.MoneyChanged(state.money()));
        events.publish(new GameEvent.InventoryChanged());
        return Result.ok(total);
    }

    /**
     * Vende unidades una por una, cada una al precio de la demanda que encuentra, que luego baja (RN-17, RN-18).
     *
     * @return el total recibido en m
     */
    public Result<Integer> sell(String itemId, int quantity) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        ItemType item = sellable(itemId);
        int owned = state.inventory().count(item);
        if (quantity < GameRules.MIN_TRANSACTION || quantity > owned) {
            return Result.failWith(ErrorCode.CANTIDAD_INVALIDA, owned);
        }
        Season season = state.date().season();
        int total = 0;
        for (int i = 0; i < quantity; i++) {
            total += state.market().sellUnit(item, season);
        }
        state.inventory().remove(item, quantity);
        state.earn(total);
        state.progress().recordSale(quantity);
        events.publish(new GameEvent.MoneyChanged(state.money()));
        events.publish(new GameEvent.InventoryChanged());
        achievements.check(state);
        return Result.ok(total);
    }

    /** Lo que pagaría vender la cantidad ahora mismo, sin vender (el «total previsto» de HU-38). */
    public Result<Integer> quote(String itemId, int quantity) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        ItemType item = sellable(itemId);
        int owned = state.inventory().count(item);
        if (quantity < GameRules.MIN_TRANSACTION || quantity > owned) {
            return Result.failWith(ErrorCode.CANTIDAD_INVALIDA, owned);
        }
        return Result.ok(state.market().quote(item, state.date().season(), quantity));
    }

    /** El mercado tal como lo muestra el diálogo: precios, tendencias y el catálogo de compra. */
    public Result<MarketView> market() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        Season season = state.date().season();
        List<SellOfferView> sells = new ArrayList<>();
        for (ItemType item : Market.sellableItems()) {
            sells.add(new SellOfferView(item.name(), Names.item(item.name()), state.inventory().count(item),
                    state.market().priceOf(item, season), state.market().trendOf(item, season).name()));
        }
        List<BuyOfferView> buys = new ArrayList<>();
        for (ItemType item : Market.purchasableItems()) {
            buys.add(new BuyOfferView(item.name(), Names.item(item.name()), Market.buyPrice(item)));
        }
        List<AnimalOfferView> animals = new ArrayList<>();
        for (AnimalType type : AnimalType.values()) {
            int free = state.farm().buildings().stream().filter(b -> b.species() == type)
                    .mapToInt(b -> b.animalCapacity() - b.animals().size()).sum();
            animals.add(new AnimalOfferView(type.name(), Names.animal(type.name()), type.price(), free));
        }
        return Result.ok(new MarketView(sells, buys, animals));
    }

    /** El inventario como una grilla. */
    public Result<InventoryView> inventory() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        Inventory inventory = session.current().inventory();
        List<SlotView> slots = new ArrayList<>();
        for (Inventory.ItemStack stack : inventory.stacks()) {
            slots.add(new SlotView(stack.type().name(), Names.item(stack.type().name()), stack.amount()));
        }
        return Result.ok(new InventoryView(inventory.capacity(), slots));
    }

    private static ItemType sellable(String itemId) {
        ItemType item = ItemType.valueOf(itemId);
        if (!Market.isSellable(item)) {
            throw new IllegalArgumentException("Not sellable: " + itemId);
        }
        return item;
    }
}
