package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.AnimalView;
import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.BuildingOfferView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.result.Messages;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.farm.Avatar;
import edu.ucc.farmsim.domain.farm.Plot;
import edu.ucc.farmsim.domain.game.DayReport;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.time.GameDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Turns the domain state into the immutable views the UI draws, so the UI never sees domain objects.
 *
 * <p>Stories: HU-01, HU-17, HU-18, HU-28, HU-34, HU-35, HU-45.
 */
@Component
public final class ViewMapper {

    /** The top bar of a game state. */
    public HudView hud(GameState state) {
        GameDate date = state.date();
        return new HudView(state.money(), date.year(), date.season().name(), date.day(),
                state.weather().today().name(), state.weather().forecast().name(),
                state.energy().current(), state.energy().max());
    }

    /** The plot at the given coordinates, which must be inside the map. */
    public PlotView plot(GameState state, int col, int row) {
        Plot plot = state.farm().plotAt(col, row);
        Crop crop = plot.crop();
        if (crop == null) {
            return new PlotView(col, row, plot.terrain().name(), null, null, false, 0, plot.isSheltered());
        }
        return new PlotView(col, row, plot.terrain().name(), crop.type().name(), crop.stage().name(),
                crop.isWatered(), crop.daysLeft(), plot.isSheltered());
    }

    /** The whole farm map, plot by plot in row-major order. */
    public MapView map(GameState state) {
        int cols = state.farm().cols();
        int rows = state.farm().rows();
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                plots.add(plot(state, col, row));
            }
        }
        return new MapView(cols, rows, List.copyOf(plots), buildingViews(state), avatar(state));
    }

    /** Where the avatar stands and looks. */
    public AvatarView avatar(GameState state) {
        Avatar avatar = state.avatar();
        return new AvatarView(avatar.col(), avatar.row(), avatar.facing().name());
    }

    /** One building with its animals. */
    public BuildingView building(Building building) {
        List<AnimalView> animals = new ArrayList<>();
        int index = 0;
        for (Animal animal : building.animals()) {
            animals.add(new AnimalView(index++, animal.type().name(), Names.animal(animal.type().name()),
                    animal.condition().name(), Names.condition(animal.condition().name()),
                    animal.daysWithoutFeed(), animal.isFedToday(), animal.pending()));
        }
        String product = building.product() == null ? null : Names.item(building.product().name());
        return new BuildingView(building.id(), building.type().name(), Names.building(building.type().name()),
                building.col(), building.row(), building.width(), building.height(), building.animalCapacity(),
                product, building.pendingProducts(), List.copyOf(animals));
    }

    /** Every building of the farm, in the order they were built. */
    public List<BuildingView> buildingViews(GameState state) {
        return state.farm().buildings().stream().map(this::building).toList();
    }

    /** The buildings and the catalogue of what can still be built. */
    public BuildingsView buildings(GameState state) {
        List<BuildingOfferView> catalog = new ArrayList<>();
        for (BuildingType type : BuildingType.values()) {
            Achievement required = type.requirement();
            boolean unlocked = required == null || state.progress().has(required);
            catalog.add(new BuildingOfferView(type.name(), Names.building(type.name()), type.width(), type.height(),
                    type.cost(), state.farm().countBuildings(type), type.maxCount(), unlocked,
                    required == null ? null : Names.achievement(required.name())));
        }
        return new BuildingsView(buildingViews(state), List.copyOf(catalog));
    }

    /** The summary of the day that just ended. */
    public DaySummaryView summary(GameState state, DayReport report) {
        List<String> highlights = new ArrayList<>();
        if (report.witheredCrops() > 0) {
            highlights.add(report.witheredCrops() == 1 ? "Se marchitó 1 cultivo."
                    : "Se marchitaron " + report.witheredCrops() + " cultivos.");
        }
        if (report.sickAnimals() > 0) {
            highlights.add(report.sickAnimals() == 1 ? "1 animal enfermó por falta de alimento."
                    : report.sickAnimals() + " animales enfermaron por falta de alimento.");
        }
        if (report.products() > 0) {
            highlights.add(report.products() == 1 ? "Los animales produjeron 1 unidad."
                    : "Los animales produjeron " + report.products() + " unidades.");
        }
        for (Achievement achievement : report.unlocked()) {
            highlights.add("Logro desbloqueado: «" + Names.achievement(achievement.name()) + "» (+"
                    + achievement.reward() + " m).");
        }
        if (highlights.isEmpty()) {
            highlights.add(Messages.NO_NEWS);
        }
        GameDate day = report.previousDate() != null ? report.previousDate() : state.date();
        return new DaySummaryView(day.year(), day.season().name(), day.day(), report.income(), report.expenses(),
                report.balanceBefore(), report.balanceAfter(), List.copyOf(highlights));
    }
}

