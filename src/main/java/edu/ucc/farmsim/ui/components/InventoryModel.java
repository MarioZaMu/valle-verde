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
 * Lo que muestra la grilla del inventario (HU-37), sin nada de Swing: una celda por casilla, cuál semilla está elegida
 * y el clic que elige una semilla para la herramienta Sembrar. Escucha los eventos del juego y recarga el inventario
 * cuando cambia; si la semilla elegida se agota, la elección se borra (HU-37 · CA-3).
 *
 * <p>Pattern: Observer — Role: ConcreteObserver (keeps the inventory grid up to date from game events)
 *
 * <p>Stories: HU-37.
 */
public final class InventoryModel implements GameListener {

    /**
     * Una celda de la grilla.
     *
     * @param itemId id del objeto, o {@code null} para una casilla vacía
     * @param name   nombre en español, o un texto vacío
     * @param amount unidades en la casilla (0 si está vacía)
     */
    public record Cell(String itemId, String name, int amount) {

        /** Verdadero para una casilla sin objetos. */
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
     * Crea el modelo.
     *
     * @param game  de dónde viene el inventario
     * @param tools la selección de herramienta compartida, cuya semilla elegida cambia este modelo
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

    /** Carga el inventario otra vez y corrige la elección de semilla si esa semilla ya no está. */
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

    /** Número de casillas de la grilla (24, o 36 con un almacén). */
    public int slotCount() {
        return view.capacity();
    }

    /** La celda de un índice de casilla, desde 0. */
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

    /** Si la celda contiene la semilla que usará la herramienta Sembrar. */
    public boolean isSelected(int index) {
        Cell cell = cell(index);
        return !cell.isEmpty() && cell.itemId().equals(tools.selectedSeed());
    }

    /**
     * Un clic en una celda: si contiene una semilla, la herramienta Sembrar la usa desde ahora. Las otras celdas no
     * hacen nada.
     */
    public void select(int index) {
        Cell cell = cell(index);
        if (!cell.isEmpty() && cell.itemId().endsWith(SEED_SUFFIX)) {
            tools.selectSeed(cell.itemId());
            fireChanged();
        }
    }

    /** Registra algo que se ejecuta cuando hay que redibujar la grilla. */
    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fireChanged() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}
