package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.ArrayList;
import java.util.List;

/**
 * The lines of the summary of the day, in Spanish and without Swing (HU-17).
 *
 * <p>Stories: HU-17.
 */
public final class DaySummaryText {

    private DaySummaryText() {
        // static helpers only
    }

    /** The lines the summary dialog shows, top to bottom. */
    public static List<String> lines(DaySummaryView summary) {
        List<String> lines = new ArrayList<>();
        lines.add(Texts.date(summary.year(), summary.season(), summary.day()));
        lines.add("Ingresos: " + Texts.money(summary.income()));
        lines.add("Gastos: " + Texts.money(summary.expenses()));
        lines.add("Saldo anterior: " + Texts.money(summary.balanceBefore()));
        lines.add("Saldo nuevo: " + Texts.money(summary.balanceAfter()));
        lines.addAll(summary.highlights());
        return lines;
    }
}
