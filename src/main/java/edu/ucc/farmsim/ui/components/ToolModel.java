package edu.ucc.farmsim.ui.components;

import java.util.ArrayList;
import java.util.List;

/**
 * Qué herramienta está seleccionada y qué semilla usa la herramienta Sembrar. Sin Swing: la barra de herramientas, las
 * teclas y el mapa comparten este único modelo, así que nunca pueden discrepar. Se avisa a los oyentes después de cada
 * cambio.
 *
 * <p>Stories: HU-36.
 */
public final class ToolModel {

    /** Semilla que usa la herramienta Sembrar hasta que el jugador elige otra. */
    public static final String DEFAULT_SEED = "CARROT_SEED";

    private static final String SEED_SUFFIX = "_SEED";

    private Tool current;
    private String selectedSeed = DEFAULT_SEED;
    private final List<Runnable> listeners = new ArrayList<>();

    /** La herramienta seleccionada, o {@code null} cuando no hay ninguna. */
    public Tool current() {
        return current;
    }

    /** Selecciona una herramienta. */
    public void select(Tool tool) {
        this.current = tool;
        fireChanged();
    }

    /** Deselecciona la herramienta (Escape). */
    public void deselect() {
        this.current = null;
        fireChanged();
    }

    /**
     * Maneja un carácter escrito: los dígitos de 1 a 5 seleccionan una herramienta.
     *
     * @return {@code true} si el carácter era una tecla de herramienta
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

    /** Id de la semilla que usa la herramienta Sembrar, por ejemplo {@code CARROT_SEED}. */
    public String selectedSeed() {
        return selectedSeed;
    }

    /**
     * Elige la semilla que usa la herramienta Sembrar.
     *
     * @throws IllegalArgumentException si el id no es un id de semilla
     */
    public void selectSeed(String seedId) {
        if (seedId == null || !seedId.endsWith(SEED_SUFFIX)) {
            throw new IllegalArgumentException("Not a seed id: " + seedId);
        }
        this.selectedSeed = seedId;
        fireChanged();
    }

    /** Registra algo que se ejecuta después de cada cambio de la selección. */
    /** Olvida la semilla elegida, por ejemplo cuando se sembró la última (HU-37 · CA-3). */
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
