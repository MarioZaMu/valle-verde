package edu.ucc.farmsim.support;

import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.ui.screens.GameDialogs;
import java.util.ArrayList;
import java.util.List;

/** A {@link GameDialogs} that opens nothing and remembers what the screen asked for. */
public final class RecordingDialogs implements GameDialogs {

    /** What was opened, in order: {@code market}, {@code inventory}, {@code construction:col,row}, {@code building:id}. */
    public final List<String> opened = new ArrayList<>();
    /** The summaries shown. */
    public final List<DaySummaryView> summaries = new ArrayList<>();

    @Override
    public void showMarket() {
        opened.add("market");
    }

    @Override
    public void showInventory() {
        opened.add("inventory");
    }

    @Override
    public void showConstruction(int col, int row) {
        opened.add("construction:" + col + "," + row);
    }

    @Override
    public void showBuilding(int buildingId) {
        opened.add("building:" + buildingId);
    }

    @Override
    public void showSummary(DaySummaryView summary) {
        summaries.add(summary);
    }
}
