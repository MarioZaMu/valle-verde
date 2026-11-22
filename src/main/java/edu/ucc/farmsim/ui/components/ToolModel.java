package edu.ucc.farmsim.ui.components;

import java.util.ArrayList;
import java.util.List;

/**
 * Which tool is selected and which seed the Sow tool uses. No Swing: the tool bar, the keys and the map all
 * share this one model, so they can never disagree. Listeners are told after every change.
 *
 * <p>Stories: HU-36.
 */
public final class ToolModel {

    /** Seed used by the Sow tool until the player picks another. */
    public static final String DEFAULT_SEED = "CARROT_SEED";

    private static final String SEED_SUFFIX = "_SEED";

    private Tool current;
    private String selectedSeed = DEFAULT_SEED;
    private final List<Runnable> listeners = new ArrayList<>();

    /** The selected tool, or {@code null} when none is selected. */
    public Tool current() {
        return current;
    }

    /** Selects a tool. */
    public void select(Tool tool) {
        this.current = tool;
        fireChanged();
    }

    /** Deselects the tool (Escape). */
    public void deselect() {
        this.current = null;
        fireChanged();
    }

    /**
     * Handles a typed character: the digits 1 to 5 select a tool.
     *
     * @return {@code true} if the character was a tool key
     */
    public boolean handleKey(char key) {
        for (Tool tool : Tool.values()) {
            if (tool.key() == key) {
                select(tool);
                return true;
            }
        }
        return false;
    }

    /** Id of the seed the Sow tool uses, for example {@code CARROT_SEED}. */
    public String selectedSeed() {
        return selectedSeed;
    }

    /**
     * Chooses the seed the Sow tool uses.
     *
     * @throws IllegalArgumentException if the id is not a seed id
     */
    public void selectSeed(String seedId) {
        if (seedId == null || !seedId.endsWith(SEED_SUFFIX)) {
            throw new IllegalArgumentException("Not a seed id: " + seedId);
        }
        this.selectedSeed = seedId;
        fireChanged();
    }

    /** Registers something to run after every change of the selection. */
    /** Forgets the chosen seed, for example when the last one has been sown (HU-37 · CA-3). */
    public void clearSeed() {
        this.selectedSeed = null;
        fireChanged();
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fireChanged() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}
