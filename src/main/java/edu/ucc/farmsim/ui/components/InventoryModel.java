package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.InventoryView;
import edu.ucc.farmsim.application.dto.SlotView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.application.result.Result;
import java.util.ArrayList;
import java.util.List;

/**
 * What the inventory grid shows (HU-37), without any Swing: one cell per slot, which seed is chosen, and the
 * click that chooses a seed for the Sow tool. It listens to the game events and reloads the inventory when it
 * changes; if the chosen seed runs out, the choice is cleared (HU-37 · CA-3).
 *
 * <p>Pattern: Observer — Role: ConcreteObserver (keeps the inventory grid up to date from game events)
 *
 * <p>Stories: HU-37.
 */
public final class InventoryModel implements GameListener {

    /**
     * One cell of the grid.
     *
     * @param itemId item id, or {@code null} for an empty slot
     * @param name   Spanish name, or an empty text
     * @param amount units in the slot (0 when empty)
     */
    public record Cell(String itemId, String name, int amount) {

        /** True for a slot without items. */
        public boolean isEmpty() {
            return itemId == null;
        }
    }

    private static final String SEED_SUFFIX = "_SEED";

    private final GameCommands game;
    private final ToolModel tools;
    private final List<Runnable> listeners = new ArrayList<>();
    private InventoryView view = new InventoryView(0, List.of());

    /**
     * Creates the model.
     *
     * @param game  where the inventory comes from
     * @param tools the shared tool selection, whose chosen seed this model changes
     */
    public InventoryModel(GameCommands game, ToolModel tools) {
        this.game = game;
        this.tools = tools;
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof GameEvent.GameStarted || event instanceof GameEvent.InventoryChanged) {
            refresh();
        }
    }

    /** Loads the inventory again and fixes the seed choice if that seed is gone. */
    public void refresh() {
        Result<InventoryView> result = game.inventory();
        if (result.isOk()) {
            view = result.value();
            String chosen = tools.selectedSeed();
            if (chosen != null && view.slots().stream().noneMatch(slot -> slot.itemId().equals(chosen))) {
                tools.clearSeed();
            }
            fireChanged();
        }
    }

    /** Number of slots of the grid (24, or 36 with a warehouse). */
    public int slotCount() {
        return view.capacity();
    }

    /** The cell at a slot index, from 0. */
    public Cell cell(int index) {
        if (index < 0 || index >= view.capacity()) {
            throw new IllegalArgumentException("no such slot: " + index);
        }
        if (index < view.slots().size()) {
            SlotView slot = view.slots().get(index);
            return new Cell(slot.itemId(), slot.name(), slot.amount());
        }
        return new Cell(null, "", 0);
    }

    /** Whether the cell holds the seed the Sow tool will use. */
    public boolean isSelected(int index) {
        Cell cell = cell(index);
        return !cell.isEmpty() && cell.itemId().equals(tools.selectedSeed());
    }

    /** A click on a cell: if it holds a seed, the Sow tool uses it from now on. Other cells do nothing. */
    public void select(int index) {
        Cell cell = cell(index);
        if (!cell.isEmpty() && cell.itemId().endsWith(SEED_SUFFIX)) {
            tools.selectSeed(cell.itemId());
            fireChanged();
        }
    }

    /** Registers something to run when the grid must be redrawn. */
    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fireChanged() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}
