package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.result.Names;

/**
 * The text of a plot's tooltip (HU-07), in Spanish. It has no Swing, so it is tested without a display.
 *
 * <p>Examples: {@code Tomate · Brote · Regado: sí · Faltan 5 días}, {@code Tierra de cultivo (libre)},
 * {@code Zanahoria · Marchito}.
 *
 * <p>Stories: HU-07.
 */
public final class PlotText {

    private static final String SEPARATOR = " · ";
    private static final String FREE = " (libre)";

    private PlotText() {
        // static helpers only
    }

    /** Describes a plot as the tooltip shows it. */
    public static String describe(PlotView plot) {
        if (plot.crop() == null) {
            String terrain = Names.terrain(plot.terrain());
            return "SOIL".equals(plot.terrain()) ? terrain + FREE : terrain;
        }
        String crop = Names.crop(plot.crop());
        String stage = Names.stage(plot.stage());
        if ("WITHERED".equals(plot.stage())) {
            return crop + SEPARATOR + stage;
        }
        if ("MATURE".equals(plot.stage())) {
            return crop + SEPARATOR + stage + SEPARATOR + "Listo para cosechar";
        }
        return crop + SEPARATOR + stage + SEPARATOR + "Regado: " + (plot.watered() ? "sí" : "no")
                + SEPARATOR + daysLeft(plot.daysToMature());
    }

    private static String daysLeft(int days) {
        return days == 1 ? "Falta 1 día" : "Faltan " + days + " días";
    }
}
