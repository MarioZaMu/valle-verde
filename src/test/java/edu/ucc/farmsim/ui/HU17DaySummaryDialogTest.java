package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.RecordingDialogs;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.DaySummaryDialog;
import edu.ucc.farmsim.ui.components.DaySummaryText;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.screens.GameScreenPresenter;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

/** The summary shown when the day ends (HU-17). */
class HU17DaySummaryDialogTest {

    private static final DaySummaryView SUMMARY = new DaySummaryView(1, "SPRING", 1, 79, 150, 500, 429,
            List.of("Se marchitó 1 cultivo."));

    @Test
    void HU17_ca1_theLinesShowIncomeExpensesAndBothBalances() {
        assertEquals(List.of("Año 1 · Primavera · Día 1", "Ingresos: 79 m", "Gastos: 150 m", "Saldo anterior: 500 m",
                "Saldo nuevo: 429 m", "Se marchitó 1 cultivo."), DaySummaryText.lines(SUMMARY));
    }

    @Test
    void HU17_ca3_aQuietDayShowsZeroesAndSinNovedades() {
        TestApp app = TestApp.startedGame();
        RecordingDialogs dialogs = new RecordingDialogs();
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, new ToolModel(), m -> { }, dialogs);
        app.bus.subscribe(event -> {
            if (event instanceof edu.ucc.farmsim.application.event.GameEvent.DaySummaryReady ready) {
                presenter.onSummary(ready.summary());
            }
        });

        presenter.onEndDay();

        assertEquals(1, dialogs.summaries.size());
        List<String> lines = DaySummaryText.lines(dialogs.summaries.get(0));
        assertTrue(lines.contains("Ingresos: 0 m"));
        assertTrue(lines.contains("Gastos: 0 m"));
        assertEquals("Sin novedades", lines.get(lines.size() - 1));
    }

    @Test
    void HU17_ca1_aSaleAndAPurchaseReachTheDialogThroughTheEventBus() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);
        app.controller.buy("CORN_SEED", 5);
        app.controller.sell("CARROT", 3);
        RecordingDialogs dialogs = new RecordingDialogs();
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, new ToolModel(), m -> { }, dialogs);
        app.bus.subscribe(event -> {
            if (event instanceof edu.ucc.farmsim.application.event.GameEvent.DaySummaryReady ready) {
                presenter.onSummary(ready.summary());
            }
        });

        presenter.onEndDay();

        List<String> lines = DaySummaryText.lines(dialogs.summaries.get(0));
        assertTrue(lines.contains("Ingresos: 79 m"));
        assertTrue(lines.contains("Gastos: 150 m"));
        assertTrue(lines.contains("Saldo anterior: 500 m"));
        assertTrue(lines.contains("Saldo nuevo: 429 m"));
    }

    @Test
    void HU17_theDialogDoesNotBlockTheGameAndCloses() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        DaySummaryDialog dialog = new DaySummaryDialog(null, SUMMARY);

        assertFalse(dialog.isModal());
        List<String> shown = new ArrayList<>();
        labels(dialog, shown);
        assertEquals(DaySummaryText.lines(SUMMARY), shown);
        dialog.dispose();
    }

    private static void labels(Container container, List<String> texts) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label) {
                texts.add(label.getText());
            }
            if (child instanceof Container inner) {
                labels(inner, texts);
            }
        }
    }
}
