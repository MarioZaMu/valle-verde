package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.assets.AssetManager;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * The inventory as a grid of slots (HU-37): the name and amount of each occupied slot (the amount of a full
 * stack is 99) and the chosen seed marked. A click on a seed chooses it for the Sow tool. Each occupied slot shows
 * the sprite of its item above the text; without sprites it shows only text.
 *
 * <p>Stories: HU-37, HU-48.
 */
public final class InventoryGrid extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int COLUMNS = 6;

    private final transient InventoryModel model;
    private final transient AssetManager assets;

    /**
     * Builds the grid and keeps it in step with the model.
     *
     * @param model what the slots hold
     */
    public InventoryGrid(InventoryModel model) {
        this(model, AssetManager.shared());
    }

    /**
     * Builds the grid with item icons from the given assets (text only if they are missing).
     *
     * @param model  what the slots hold
     * @param assets where the item icons come from
     */
    public InventoryGrid(InventoryModel model, AssetManager assets) {
        super(new GridLayout(0, COLUMNS, 4, 4));
        this.model = model;
        this.assets = assets;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        model.addChangeListener(this::rebuild);
        rebuild();
    }

    private void rebuild() {
        removeAll();
        for (int i = 0; i < model.slotCount(); i++) {
            InventoryModel.Cell cell = model.cell(i);
            JToggleButton slot = new JToggleButton(cell.isEmpty() ? " "
                    : "<html><center>" + cell.name() + "<br><b>" + cell.amount() + "</b></center></html>");
            slot.setName("slot-" + i);
            if (!cell.isEmpty()) {
                assets.icon("items/" + cell.itemId() + ".png").ifPresent(slot::setIcon);
                slot.setVerticalTextPosition(SwingConstants.BOTTOM);
                slot.setHorizontalTextPosition(SwingConstants.CENTER);
            }
            slot.setFocusable(false);
            slot.setSelected(model.isSelected(i));
            slot.setEnabled(!cell.isEmpty());
            int index = i;
            slot.addActionListener(event -> model.select(index));
            add(slot);
        }
        revalidate();
        repaint();
    }
}
