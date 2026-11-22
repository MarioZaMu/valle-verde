package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.time.Season;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The market: what each product sells for, how its demand falls when sold and recovers each day (RN-17, RN-18),
 * its trend (RN-19) and the fixed catalogue of purchases (RN-16).
 *
 * <p>Every sellable product (the five crops, eggs and milk) has its own demand between 50 and 100 percent. The
 * price of a unit is worked out by the {@link PricingStrategy} of the product with the demand of that moment.
 *
 * <p>Stories: HU-14, HU-15, HU-16.
 */
public final class Market {

    private final PricingStrategy cropPricing = new CropPricing();
    private final PricingStrategy flatPricing = new FlatPricing();
    private final Map<ItemType, Integer> demand = new EnumMap<>(ItemType.class);
    private final Map<ItemType, Integer> previousPrice = new EnumMap<>(ItemType.class);

    private Market() {
        for (ItemType item : sellableItems()) {
            demand.put(item, GameRules.DEMAND_MAX);
            previousPrice.put(item, 0);
        }
    }

    /** A market of a new game: full demand everywhere and no price history. */
    public static Market standard() {
        return new Market();
    }

    /** Whether a product can be sold: crops, eggs and milk. */
    public static boolean isSellable(ItemType item) {
        return item.kind() == ItemType.Kind.CROP || item.kind() == ItemType.Kind.ANIMAL_PRODUCT;
    }

    /** Every sellable product, in a stable order. */
    public static List<ItemType> sellableItems() {
        List<ItemType> items = new ArrayList<>();
        for (ItemType item : ItemType.values()) {
            if (isSellable(item)) {
                items.add(item);
            }
        }
        return items;
    }

    /** Whether a product is in the fixed purchase catalogue: seeds, hay and medicine (RN-16). */
    public static boolean isPurchasable(ItemType item) {
        return item.kind() == ItemType.Kind.SEED || item.kind() == ItemType.Kind.SUPPLY;
    }

    /** Every purchasable product, in a stable order. */
    public static List<ItemType> purchasableItems() {
        List<ItemType> items = new ArrayList<>();
        for (ItemType item : ItemType.values()) {
            if (isPurchasable(item)) {
                items.add(item);
            }
        }
        return items;
    }

    /** The fixed price in m of one unit of a purchasable product. */
    public static int buyPrice(ItemType item) {
        return switch (item) {
            case HAY -> GameRules.PRICE_HAY;
            case MEDICINE -> GameRules.PRICE_MEDICINE;
            default -> {
                if (item.kind() != ItemType.Kind.SEED) {
                    throw new IllegalArgumentException("Not purchasable: " + item);
                }
                yield item.cropType().seedPrice();
            }
        };
    }

    /** The demand of a sellable product, in percent. */
    public int demandOf(ItemType item) {
        requireSellable(item);
        return demand.get(item);
    }

    /** The price of the next unit of a product at the current demand. */
    public int priceOf(ItemType item, Season season) {
        return strategyFor(item).unitPrice(item, season, demandOf(item));
    }

    /**
     * Sells one unit: returns its price at the current demand and then lowers the demand by 2 (never below 50).
     */
    public int sellUnit(ItemType item, Season season) {
        int price = priceOf(item, season);
        demand.put(item, Math.max(GameRules.DEMAND_MIN, demandOf(item) - GameRules.DEMAND_DROP_PER_UNIT));
        return price;
    }

    /** What selling the given quantity would pay, each unit at the demand it would find. Changes nothing. */
    public int quote(ItemType item, Season season, int quantity) {
        int current = demandOf(item);
        int total = 0;
        for (int i = 0; i < quantity; i++) {
            total += strategyFor(item).unitPrice(item, season, current);
            current = Math.max(GameRules.DEMAND_MIN, current - GameRules.DEMAND_DROP_PER_UNIT);
        }
        return total;
    }

    /**
     * Step 4 of RN-03: remembers the current price of every product as yesterday's (RN-19) and then recovers
     * the demand by 5 (never above 100, RN-18).
     */
    public void endOfDay(Season season) {
        for (ItemType item : sellableItems()) {
            previousPrice.put(item, priceOf(item, season));
            demand.put(item, Math.min(GameRules.DEMAND_MAX, demandOf(item) + GameRules.DEMAND_RECOVERY_PER_DAY));
        }
    }

    /** Whether the price went up, down or stayed the same since the last end of day (RN-19). */
    public Trend trendOf(ItemType item, Season season) {
        int before = previousPrice.get(item);
        if (before == 0) {
            return Trend.SAME;
        }
        int now = priceOf(item, season);
        if (now > before) {
            return Trend.UP;
        }
        return now < before ? Trend.DOWN : Trend.SAME;
    }

    /** An immutable copy of the market. */
    public MarketSnapshot snapshot() {
        return new MarketSnapshot(demand, previousPrice);
    }

    /** Puts the market back as it was when the snapshot was taken. */
    public void restoreFrom(MarketSnapshot snapshot) {
        demand.putAll(snapshot.demand());
        previousPrice.putAll(snapshot.previousPrice());
    }

    private PricingStrategy strategyFor(ItemType item) {
        requireSellable(item);
        return item.kind() == ItemType.Kind.CROP ? cropPricing : flatPricing;
    }

    private static void requireSellable(ItemType item) {
        if (!isSellable(item)) {
            throw new IllegalArgumentException("Not sellable: " + item);
        }
    }
}
