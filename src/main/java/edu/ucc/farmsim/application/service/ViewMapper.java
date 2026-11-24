package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.AnimalView;
import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.BuildingOfferView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.HouseView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.result.Messages;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.farm.Avatar;
import edu.ucc.farmsim.domain.farm.House;
import edu.ucc.farmsim.domain.farm.Plot;
import edu.ucc.farmsim.domain.game.DayReport;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.time.GameDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Convierte el estado del dominio en las vistas inmutables que dibuja la UI, de modo que la UI nunca ve objetos del
 * dominio.
 *
 * <p>Stories: HU-01, HU-17, HU-18, HU-28, HU-34, HU-35, HU-45, HU-46, HU-50.
 */
@Component
public final class ViewMapper {

    /** Las casas nunca se mueven, así que sus vistas se hacen una sola vez. */
    private static final List<HouseView> HOUSES = java.util.Arrays.stream(House.values())
            .map(h -> new HouseView(h.name(), h.col(), h.row(), h.width(), h.height())).toList();

    /** La barra superior de un estado de partida. */
    public HudView hud(GameState state) {
        GameDate date = state.date();
        return new HudView(state.money(), date.year(), date.season().name(), date.day(),
                state.weather().today().name(), state.weather().forecast().name(),
                state.energy().current(), state.energy().max());
    }

    /** La parcela en las coordenadas dadas, que deben estar dentro del mapa. */
    public PlotView plot(GameState state, int col, int row) {
        Plot plot = state.farm().plotAt(col, row);
        Crop crop = plot.crop();
        if (crop == null) {
            return new PlotView(col, row, plot.terrain().name(), null, null, false, 0, plot.isSheltered());
        }
        return new PlotView(col, row, plot.terrain().name(), crop.type().name(), crop.stage().name(),
                crop.isWatered(), crop.daysLeft(), plot.isSheltered());
    }

    /** Todo el mapa de la granja, parcela por parcela en orden de fila. */
    public MapView map(GameState state) {
        int cols = state.farm().cols();
        int rows = state.farm().rows();
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                plots.add(plot(state, col, row));
            }
        }
        return new MapView(cols, rows, List.copyOf(plots), buildingViews(state), avatar(state), npcs(state), HOUSES);
    }

    /** Las personas del valle, en un orden fijo, con el estado de sus encargos. */
    public List<NpcView> npcs(GameState state) {
        List<NpcView> all = new ArrayList<>();
        for (Npc npc : Npc.values()) {
            all.add(new NpcView(npc.name(), Names.npc(npc.name()), Names.npcRole(npc.name()), npc.col(), npc.row(),
                    npc.errandItem().name(), npc.errandAmount(), npc.errandReward(), state.errands().isDone(npc)));
        }
        return List.copyOf(all);
    }

    /** Dónde está el avatar y hacia dónde mira. */
    public AvatarView avatar(GameState state) {
        Avatar avatar = state.avatar();
        return new AvatarView(avatar.col(), avatar.row(), avatar.facing().name());
    }

    /** Un edificio con sus animales. */
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

    /** Todos los edificios de la granja, en el orden en que se construyeron. */
    public List<BuildingView> buildingViews(GameState state) {
        return state.farm().buildings().stream().map(this::building).toList();
    }

    /** Los edificios y el catálogo de lo que aún se puede construir. */
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

    /** El resumen del día que acaba de terminar. */
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

