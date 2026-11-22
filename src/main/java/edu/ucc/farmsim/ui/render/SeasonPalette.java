package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.ui.theme.Palette;
import java.awt.Color;

/**
 * The look of each season on the map (HU-40 · CA-1): grass changes colour, and the weather adds a tint. Spring
 * keeps the base palette.
 *
 * <p>Stories: HU-40.
 */
final class SeasonPalette {

    private static final Color SUMMER_GRASS = new Color(0x86B33C);
    private static final Color AUTUMN_GRASS = new Color(0xB98A3E);
    private static final Color WINTER_GRASS = new Color(0xD8E6EE);

    private SeasonPalette() {
        // static helpers only
    }

    /** Grass colour of a season id; unknown ids use the spring colour. */
    static Color grass(String season) {
        if (season == null) {
            return Palette.GRASS;
        }
        return switch (season) {
            case "SUMMER" -> SUMMER_GRASS;
            case "AUTUMN" -> AUTUMN_GRASS;
            case "WINTER" -> WINTER_GRASS;
            default -> Palette.GRASS;
        };
    }

    /** Translucent tint laid over the whole map for a weather id, or {@code null} if it does not tint. */
    static Color tint(String weather) {
        if (weather == null) {
            return null;
        }
        return switch (weather) {
            case "RAINY" -> new Color(30, 60, 120, 45);
            case "CLOUDY" -> new Color(90, 90, 100, 35);
            case "DROUGHT" -> new Color(255, 170, 40, 50);
            case "FROST" -> new Color(150, 200, 255, 60);
            case "SNOWY" -> new Color(255, 255, 255, 30);
            default -> null;
        };
    }
}
