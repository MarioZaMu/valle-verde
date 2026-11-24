package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.time.Season;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * El mercado: por cuánto se vende cada producto, cómo baja su demanda al venderse y se recupera cada día (RN-17,
 * RN-18), su tendencia (RN-19) y el catálogo fijo de compras (RN-16).
 *
 * <p>Cada producto vendible (los cinco cultivos, huevos y leche) tiene su propia demanda entre 50 y 100 por ciento. El
 * precio de una unidad lo calcula la {@link PricingStrategy} del producto con la demanda de ese momento.
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

    /** Un mercado de partida nueva: demanda completa en todo y sin historial de precios. */
    public static Market standard() {
        return new Market();
    }

    /** Si un producto se puede vender: cultivos, huevos y leche. */
    public static boolean isSellable(ItemType item) {
        return item.kind() == ItemType.Kind.CROP || item.kind() == ItemType.Kind.ANIMAL_PRODUCT;
    }

    /** Todos los productos vendibles, en un orden estable. */
    public static List<ItemType> sellableItems() {
        List<ItemType> items = new ArrayList<>();
        for (ItemType item : ItemType.values()) {
            if (isSellable(item)) {
                items.add(item);
            }
        }
        return items;
    }

    /** Si un producto está en el catálogo fijo de compras: semillas, heno y medicina (RN-16). */
    public static boolean isPurchasable(ItemType item) {
        return item.kind() == ItemType.Kind.SEED || item.kind() == ItemType.Kind.SUPPLY;
    }

    /** Todos los productos comprables, en un orden estable. */
    public static List<ItemType> purchasableItems() {
        List<ItemType> items = new ArrayList<>();
        for (ItemType item : ItemType.values()) {
            if (isPurchasable(item)) {
                items.add(item);
            }
        }
        return items;
    }

    /** El precio fijo en m de una unidad de un producto comprable. */
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

    /** La demanda de un producto vendible, en porcentaje. */
    public int demandOf(ItemType item) {
        requireSellable(item);
        return demand.get(item);
    }

    /** El precio de la siguiente unidad de un producto con la demanda actual. */
    public int priceOf(ItemType item, Season season) {
        return strategyFor(item).unitPrice(item, season, demandOf(item));
    }

    /**
     * Vende una unidad: devuelve su precio con la demanda actual y luego baja la demanda en 2 (nunca por debajo de 50).
     */
    public int sellUnit(ItemType item, Season season) {
        int price = priceOf(item, season);
        demand.put(item, Math.max(GameRules.DEMAND_MIN, demandOf(item) - GameRules.DEMAND_DROP_PER_UNIT));
        return price;
    }

    /** Lo que pagaría vender la cantidad dada, cada unidad con la demanda que encontraría. No cambia nada. */
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
     * Paso 4 de RN-03: recuerda el precio actual de cada producto como el de ayer (RN-19) y luego recupera la demanda
     * en 5 (nunca por encima de 100, RN-18).
     */
    public void endOfDay(Season season) {
        for (ItemType item : sellableItems()) {
            previousPrice.put(item, priceOf(item, season));
            demand.put(item, Math.min(GameRules.DEMAND_MAX, demandOf(item) + GameRules.DEMAND_RECOVERY_PER_DAY));
        }
    }

    /** Si el precio subió, bajó o quedó igual desde el último fin de día (RN-19). */
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

    /** Una copia inmutable del mercado. */
    public MarketSnapshot snapshot() {
        return new MarketSnapshot(demand, previousPrice);
    }

    /** Deja el mercado como estaba cuando se tomó la instantánea. */
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
