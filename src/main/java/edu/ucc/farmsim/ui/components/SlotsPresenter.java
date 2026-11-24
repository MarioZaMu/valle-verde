package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * La lógica de las ranuras de guardado, sin nada de Swing (HU-29, HU-30, HU-32): el texto de cada ranura, guardar con
 * la confirmación que pide sobrescribir, y cargar. Los diálogos solo la dibujan.
 *
 * <p>Stories: HU-29, HU-30, HU-32.
 */
public final class SlotsPresenter {

    private final GameCommands game;
    private List<SaveSlotView> slots = List.of();
    private String error = "";

    /**
     * Crea el presentador y lee las ranuras.
     *
     * @param game la partida a la que pertenecen las ranuras
     */
    public SlotsPresenter(GameCommands game) {
        this.game = game;
        refresh();
    }

    /** Lee las ranuras otra vez (para que un guardado nuevo se vea de inmediato). */
    public void refresh() {
        Result<List<SaveSlotView>> result = game.saveSlots();
        error = result.isOk() ? "" : result.message();
        if (result.isOk()) {
            slots = result.value();
        }
    }

    /** La razón por la que no se pudieron leer las ranuras, o un texto vacío. */
    public String error() {
        return error;
    }

    /** Las ranuras tal como las encontró la última actualización. */
    public List<SaveSlotView> slots() {
        return slots;
    }

    /** La línea de cada ranura, por ejemplo {@code Ranura 1: Año 1 · Verano · día 5 · 1.250 m · 2026-11-22 10:30}. */
    public List<String> lines() {
        return slots.stream().map(SlotsPresenter::line).toList();
    }

    /** La línea de una ranura. */
    public static String line(SaveSlotView slot) {
        String prefix = "Ranura " + slot.slot() + ": ";
        return switch (slot.status()) {
            case "EMPTY" -> prefix + Texts.SLOT_EMPTY;
            case "DAMAGED" -> prefix + Texts.SLOT_DAMAGED;
            default -> prefix + Texts.date(slot.year(), slot.season(), slot.day()).replace("Día", "día") + " · "
                    + Texts.money(slot.money()) + " · " + slot.savedAt();
        };
    }

    /** Verdadero si la ranura contiene una partida que se puede cargar. */
    public boolean canLoad(int slot) {
        return slots.stream().anyMatch(s -> s.slot() == slot && "OK".equals(s.status()));
    }

    /** Verdadero si guardar en la ranura reemplazaría algo (se le pregunta primero al jugador, HU-29 · CA-3). */
    public boolean needsConfirmation(int slot) {
        return slots.stream().anyMatch(s -> s.slot() == slot && !"EMPTY".equals(s.status()));
    }

    /**
     * Guarda en la ranura, preguntando primero si está ocupada.
     *
     * @param slot      número de ranura
     * @param confirmed se le pregunta cuando la ranura está ocupada; el guardado sigue solo si responde {@code true}
     * @return el mensaje que se muestra, o un texto vacío si el jugador canceló
     */
    public String save(int slot, BooleanSupplier confirmed) {
        if (needsConfirmation(slot) && !confirmed.getAsBoolean()) {
            return "";
        }
        Result<SaveSlotView> result = game.save(slot);
        refresh();
        return result.isOk() ? Texts.SAVED_IN_SLOT + slot + "." : result.message();
    }

    /** Carga la ranura; la partida en curso cambia solo si esto tiene éxito. */
    public Result<HudView> load(int slot) {
        Result<HudView> result = game.load(slot);
        refresh();
        return result;
    }
}
