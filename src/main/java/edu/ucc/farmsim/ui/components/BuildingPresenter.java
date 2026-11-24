package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.AnimalView;
import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.List;

/**
 * La lógica del panel del edificio, sin nada de Swing (HU-20, HU-22, HU-23): los animales que se listan, qué botones
 * están habilitados y los mensajes después de alimentar, recoger o curar. El diálogo solo la dibuja.
 *
 * <p>Curar se hace aquí y no con una herramienta de la barra (RN-27).
 *
 * <p>Stories: HU-20, HU-22, HU-23.
 */
public final class BuildingPresenter {

    private static final String SICK = "SICK";

    private final GameCommands game;
    private final int buildingId;
    private BuildingView building;

    /**
     * Crea el presentador y carga el edificio.
     *
     * @param game       la partida a la que pertenece el edificio
     * @param buildingId id del edificio
     */
    public BuildingPresenter(GameCommands game, int buildingId) {
        this.game = game;
        this.buildingId = buildingId;
        refresh();
    }

    /** Carga el edificio otra vez. */
    public void refresh() {
        Result<BuildingsView> all = game.buildings();
        if (all.isOk()) {
            building = all.value().buildings().stream().filter(b -> b.id() == buildingId).findFirst()
                    .orElse(building);
        }
    }

    /** El edificio, o {@code null} si no se pudo cargar. */
    public BuildingView building() {
        return building;
    }

    /** Título del panel, por ejemplo {@code Gallinero 1 · 2/4 animales}. */
    public String title() {
        if (building == null) {
            return Texts.BUILDING_TITLE;
        }
        return building.name() + " " + (building.id() + 1) + " — " + building.animals().size() + "/"
                + building.capacity() + " animales";
    }

    /** Los animales del edificio. */
    public List<AnimalView> animals() {
        return building == null ? List.of() : building.animals();
    }

    /** La fila de un animal tal como la muestra la tabla. */
    public Object[] row(AnimalView animal) {
        return new Object[] {animal.name(), animal.conditionName(), animal.daysWithoutFeed(),
                animal.fedToday() ? Texts.YES : Texts.NO, animal.pending()};
    }

    /** Verdadero si algún animal puede comer y no ha comido hoy. */
    public boolean canFeed() {
        return animals().stream().anyMatch(a -> !SICK.equals(a.condition()) && !a.fedToday());
    }

    /** Verdadero si el edificio tiene productos esperando. */
    public boolean canCollect() {
        return building != null && building.pendingProducts() > 0;
    }

    /** Verdadero si el animal de la posición está enfermo. */
    public boolean canCure(int animalIndex) {
        return animalIndex >= 0 && animalIndex < animals().size() && SICK.equals(animals().get(animalIndex).condition());
    }

    /** Alimenta el edificio y responde el mensaje que se muestra. */
    public String feed() {
        Result<BuildingActionView> result = game.feed(buildingId);
        refresh();
        if (!result.isOk()) {
            return result.message();
        }
        BuildingActionView view = result.value();
        return view.notice() != null ? view.notice() : "Alimentaste a " + view.amount() + " animal(es).";
    }

    /** Recoge los productos y responde el mensaje que se muestra. */
    public String collect() {
        Result<BuildingActionView> result = game.collect(buildingId);
        refresh();
        if (!result.isOk()) {
            return result.message();
        }
        BuildingActionView view = result.value();
        return view.notice() != null ? view.notice() : "Recogiste " + view.amount() + " × " + product(view) + ".";
    }

    /** Cura al animal de la posición y responde el mensaje que se muestra. */
    public String cure(int animalIndex) {
        Result<BuildingActionView> result = game.cure(buildingId, animalIndex);
        refresh();
        if (!result.isOk()) {
            return result.message();
        }
        BuildingActionView view = result.value();
        return view.notice() != null ? view.notice() : "El animal se curó.";
    }

    private static String product(BuildingActionView view) {
        return view.building().productName() == null ? "productos" : view.building().productName();
    }
}
