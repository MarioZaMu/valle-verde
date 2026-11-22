package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * The logic of the save slots, without any Swing (HU-29, HU-30, HU-32): the text of each slot, saving with the
 * confirmation that overwriting asks for, and loading. The dialogs only draw it.
 *
 * <p>Stories: HU-29, HU-30, HU-32.
 */
public final class SlotsPresenter {

    private final GameCommands game;
    private List<SaveSlotView> slots = List.of();
    private String error = "";

    /**
     * Creates the presenter and reads the slots.
     *
     * @param game the game the slots belong to
     */
    public SlotsPresenter(GameCommands game) {
        this.game = game;
        refresh();
    }

    /** Reads the slots again (so a new save shows at once). */
    public void refresh() {
        Result<List<SaveSlotView>> result = game.saveSlots();
        error = result.isOk() ? "" : result.message();
        if (result.isOk()) {
            slots = result.value();
        }
    }

    /** The reason the slots could not be read, or an empty text. */
    public String error() {
        return error;
    }

    /** The slots as the last refresh found them. */
    public List<SaveSlotView> slots() {
        return slots;
    }

    /** The line of each slot, for example {@code Ranura 1: Año 1 · Verano · día 5 · 1.250 m · 2026-11-22 10:30}. */
    public List<String> lines() {
        return slots.stream().map(SlotsPresenter::line).toList();
    }

    /** The line of one slot. */
    public static String line(SaveSlotView slot) {
        String prefix = "Ranura " + slot.slot() + ": ";
        return switch (slot.status()) {
            case "EMPTY" -> prefix + Texts.SLOT_EMPTY;
            case "DAMAGED" -> prefix + Texts.SLOT_DAMAGED;
            default -> prefix + Texts.date(slot.year(), slot.season(), slot.day()).replace("Día", "día") + " · "
                    + Texts.money(slot.money()) + " · " + slot.savedAt();
        };
    }

    /** True if the slot holds a game that can be loaded. */
    public boolean canLoad(int slot) {
        return slots.stream().anyMatch(s -> s.slot() == slot && "OK".equals(s.status()));
    }

    /** True if saving in the slot would replace something (the player is asked first, HU-29 · CA-3). */
    public boolean needsConfirmation(int slot) {
        return slots.stream().anyMatch(s -> s.slot() == slot && !"EMPTY".equals(s.status()));
    }

    /**
     * Saves in the slot, asking first if it is occupied.
     *
     * @param slot      slot number
     * @param confirmed asked when the slot is occupied; saving goes on only if it answers {@code true}
     * @return the message to show, or an empty text if the player cancelled
     */
    public String save(int slot, BooleanSupplier confirmed) {
        if (needsConfirmation(slot) && !confirmed.getAsBoolean()) {
            return "";
        }
        Result<SaveSlotView> result = game.save(slot);
        refresh();
        return result.isOk() ? Texts.SAVED_IN_SLOT + slot + "." : result.message();
    }

    /** Loads the slot; the game in progress changes only if this succeeds. */
    public Result<HudView> load(int slot) {
        Result<HudView> result = game.load(slot);
        refresh();
        return result;
    }
}
