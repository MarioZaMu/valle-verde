package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.assets.AssetManager;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * El inventario como una grilla de casillas (HU-37): el nombre y la cantidad de cada casilla ocupada (la cantidad de
 * una pila llena es 99) y la semilla elegida marcada. Un clic en una semilla la elige para la herramienta Sembrar. Cada
 * casilla ocupada muestra el sprite de su objeto sobre el texto; sin sprites muestra solo texto.
 *
 * <p>Stories: HU-37, HU-48.
 */
public final class InventoryGrid extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int COLUMNS = 6;

    private final transient InventoryModel model;
    private final transient AssetManager assets;

    /**
     * Construye la grilla y la mantiene al día con el modelo.
     *
     * @param model lo que contienen las casillas
     */
    public InventoryGrid(InventoryModel model) {
        this(model, AssetManager.shared());
    }

    /**
     * Construye la grilla con íconos de objetos de los recursos dados (solo texto si faltan).
     *
     * @param model  lo que contienen las casillas
     * @param assets de donde vienen los íconos de objetos
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
