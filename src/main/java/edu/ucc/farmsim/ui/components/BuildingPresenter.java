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
 * The logic of the building panel, without any Swing (HU-20, HU-22, HU-23): the animals to list, which buttons
 * are enabled and the messages after feeding, collecting or curing. The dialog only draws it.
 *
 * <p>Curing is done here and not with a tool of the bar (RN-27).
 *
 * <p>Stories: HU-20, HU-22, HU-23.
 */
public final class BuildingPresenter {

    private static final String SICK = "SICK";

    private final GameCommands game;
    private final int buildingId;
    private BuildingView building;

    /**
     * Creates the presenter and loads the building.
     *
     * @param game       the game the building belongs to
     * @param buildingId id of the building
     */
    public BuildingPresenter(GameCommands game, int buildingId) {
        this.game = game;
        this.buildingId = buildingId;
        refresh();
    }

    /** Loads the building again. */
    public void refresh() {
        Result<BuildingsView> all = game.buildings();
        if (all.isOk()) {
            building = all.value().buildings().stream().filter(b -> b.id() == buildingId).findFirst()
                    .orElse(building);
        }
    }

    /** The building, or {@code null} if it could not be loaded. */
    public BuildingView building() {
        return building;
    }

    /** Title of the panel, for example {@code Gallinero 1 — 2/4 animales}. */
    public String title() {
        if (building == null) {
            return Texts.BUILDING_TITLE;
        }
        return building.name() + " " + (building.id() + 1) + " — " + building.animals().size() + "/"
                + building.capacity() + " animales";
    }

    /** The animals of the building. */
    public List<AnimalView> animals() {
        return building == null ? List.of() : building.animals();
    }

    /** The row of an animal as the table shows it. */
    public Object[] row(AnimalView animal) {
        return new Object[] {animal.name(), animal.conditionName(), animal.daysWithoutFeed(),
                animal.fedToday() ? Texts.YES : Texts.NO, animal.pending()};
    }

    /** True if some animal can eat and has not eaten today. */
    public boolean canFeed() {
        return animals().stream().anyMatch(a -> !SICK.equals(a.condition()) && !a.fedToday());
    }

    /** True if the building has products waiting. */
    public boolean canCollect() {
        return building != null && building.pendingProducts() > 0;
    }

    /** True if the animal at the position is sick. */
    public boolean canCure(int animalIndex) {
        return animalIndex >= 0 && animalIndex < animals().size() && SICK.equals(animals().get(animalIndex).condition());
    }

    /** Feeds the building and answers the message to show. */
    public String feed() {
        Result<BuildingActionView> result = game.feed(buildingId);
        refresh();
        if (!result.isOk()) {
            return result.message();
        }
        BuildingActionView view = result.value();
        return view.notice() != null ? view.notice() : "Alimentaste a " + view.amount() + " animal(es).";
    }

    /** Collects the products and answers the message to show. */
    public String collect() {
        Result<BuildingActionView> result = game.collect(buildingId);
        refresh();
        if (!result.isOk()) {
            return result.message();
        }
        BuildingActionView view = result.value();
        return view.notice() != null ? view.notice() : "Recogiste " + view.amount() + " × " + product(view) + ".";
    }

    /** Cures the animal at the position and answers the message to show. */
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
