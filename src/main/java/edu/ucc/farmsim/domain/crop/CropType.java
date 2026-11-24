package edu.ucc.farmsim.domain.crop;

import edu.ucc.farmsim.domain.time.Season;
import java.util.EnumSet;
import java.util.Set;

/**
 * Catálogo de cultivos (RN-05): días para madurar y las estaciones en que se puede sembrar cada uno. Este enum es una
 * tabla de configuración; los precios de semillas y cosechas los agregan las historias de economía.
 *
 * <p>Stories: HU-01.
 */
public enum CropType {
    CARROT(4, 10, 30, Season.SPRING),
    POTATO(6, 20, 55, Season.SPRING),
    TOMATO(7, 25, 65, Season.SUMMER),
    CORN(8, 30, 75, Season.SUMMER, Season.AUTUMN),
    PUMPKIN(10, 40, 110, Season.AUTUMN);

    private final int daysToMature;
    private final int seedPrice;
    private final int sellBasePrice;
    private final Set<Season> validSeasons;

    CropType(int daysToMature, int seedPrice, int sellBasePrice, Season first, Season... more) {
        this.daysToMature = daysToMature;
        this.seedPrice = seedPrice;
        this.sellBasePrice = sellBasePrice;
        this.validSeasons = EnumSet.of(first, more);
    }

    /** Días que hay que regar un cultivo para que madure (T en RN-08). */
    public int daysToMature() {
        return daysToMature;
    }

    /** Estaciones en que se puede sembrar este cultivo y sigue vivo. */
    /** Precio en m de una semilla en el mercado (RN-05, RN-16). */
    public int seedPrice() {
        return seedPrice;
    }

    /** Precio base de venta en m de una unidad cosechada (RN-05, RN-17). */
    public int sellBasePrice() {
        return sellBasePrice;
    }

    public Set<Season> validSeasons() {
        return EnumSet.copyOf(validSeasons);
    }

    /** Día de crecimiento en que el cultivo deja la etapa Brote: {@code ceil(T / 2)} (RN-08). */
    public int halfwayDays() {
        return (daysToMature + 1) / 2;
    }
}
