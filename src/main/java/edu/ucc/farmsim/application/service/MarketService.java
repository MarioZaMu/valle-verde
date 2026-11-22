package edu.ucc.farmsim.application.service;

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
 * The economy use cases: buying at fixed prices (RN-16), selling at the dynamic price (RN-17, RN-18), the
 * previsional total of a sale, the market and inventory views. Every rejection leaves the game untouched.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-37, HU-38.
 */
@Service
public final class MarketService {

    private final GameSession session;
    private final EventBus events;

    /**
     * Creates the service.
     *
     * @param session the game in progress
     * @param events  where events are published
     */
    public MarketService(GameSession session, EventBus events) {
        this.session = session;
        this.events = events;
    }

    /**
     * Buys units at the fixed price: the quantity must be 1 to 99, the player must afford the total and the
     * units must fit in the inventory (RN-14, RN-15, RN-16).
     *
     * @return the total paid in m
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
     * Sells units one by one, each at the price of the demand it finds, which then drops (RN-17, RN-18).
     *
     * @return the total received in m
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
        events.publish(new GameEvent.MoneyChanged(state.money()));
        events.publish(new GameEvent.InventoryChanged());
        return Result.ok(total);
    }

    /** What selling the quantity would pay right now, without selling (the "total previsto" of HU-38). */
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

    /** The market as the dialog shows it: prices, trends and the purchase catalogue. */
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
        return Result.ok(new MarketView(sells, buys));
    }

    /** The inventory as a grid. */
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
