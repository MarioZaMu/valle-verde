package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.ArrayList;
import java.util.List;

/**
 * Las líneas del resumen del día, en español y sin Swing (HU-17).
 *
 * <p>Stories: HU-17.
 */
public final class DaySummaryText {

    private DaySummaryText() {
                // solo métodos de utilidad estáticos
    }

    /** Las líneas que muestra el diálogo del resumen, de arriba abajo. */
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
