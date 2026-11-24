package edu.ucc.farmsim.support;

import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.ui.screens.GameDialogs;
import java.util.ArrayList;
import java.util.List;

/** Un {@link GameDialogs} que no abre nada y recuerda lo que pidió la pantalla. */
public final class RecordingDialogs implements GameDialogs {

    /**
     * Lo que se abrió, en orden: {@code market}, {@code inventory}, {@code construction:col,row}, {@code building:id}.
     */
    public final List<String> opened = new ArrayList<>();
    /** Los diálogos abiertos. */
    public final List<DialogueView> dialogues = new ArrayList<>();
    /** Los resúmenes mostrados. */
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
    public void showAchievements() {
        opened.add("achievements");
    }

    @Override
    public void showSlots(boolean saving) {
        opened.add(saving ? "slots:save" : "slots:load");
    }

    @Override
    public void showSummary(DaySummaryView summary) {
        summaries.add(summary);
    }

    @Override
    public void showDialogue(DialogueView dialogue) {
        opened.add("dialogue:" + dialogue.npcId());
        dialogues.add(dialogue);
    }
}
