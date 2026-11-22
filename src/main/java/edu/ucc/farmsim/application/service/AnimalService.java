package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Messages;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.game.Energy;
import edu.ucc.farmsim.domain.game.GameState;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The animal use cases: buy and house (RN-23), feed a building (RN-24), collect its products (RN-26) and cure
 * a sick animal (RN-27). Every operation checks all its rules first and only then changes the game, so a
 * rejection never leaves anything half done.
 *
 * <p>Stories: HU-19, HU-20, HU-22, HU-23, HU-26.
 */
@Service
public final class AnimalService {

    private final GameSession session;
    private final ViewMapper mapper;
    private final EventBus events;
    private final AchievementService achievements;

    /**
     * Creates the service.
     *
     * @param session the game in progress
     * @param mapper  builds the views the UI draws
     * @param events  where events are published
     * @param achievements evaluates the achievements after buying and curing
     */
    public AnimalService(GameSession session, ViewMapper mapper, EventBus events, AchievementService achievements) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
        this.achievements = achievements;
    }

    /**
     * Buys an animal and houses it in the first building of its kind with a free place (RN-23).
     *
     * @param animalId animal id, for example {@code HEN}
     * @return the building where the animal now lives
     */
    public Result<BuildingView> buyAnimal(String animalId) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        AnimalType type = AnimalType.valueOf(animalId);
        GameState state = session.current();
        if (type.price() > state.money()) {
            return Result.failWith(ErrorCode.SIN_FONDOS, type.price() - state.money());
        }
        Building home = state.farm().buildings().stream().filter(b -> b.hasRoomFor(type)).findFirst().orElse(null);
        if (home == null) {
            return Result.failWith(ErrorCode.SIN_ESPACIO_ANIMALES, Names.animalPlural(type.name()));
        }
        state.spend(type.price());
        home.addAnimal(new Animal(type));
        events.publish(new GameEvent.MoneyChanged(state.money()));
        events.publish(new GameEvent.MapChanged());
        achievements.check(state);
        return Result.ok(mapper.building(home));
    }

    /**
     * Feeds every animal of a building that can eat and has not eaten today: 1 hay and 1 energy each, all or
     * nothing (RN-24). Sick animals are skipped and the player is told.
     *
     * @param buildingId id of the building
     */
    public Result<BuildingActionView> feed(int buildingId) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        Building building = building(state, buildingId);
        List<Animal> hungry = building.animals().stream().filter(Animal::needsFeeding).toList();
        long sick = building.animals().stream().filter(Animal::isSick).count();
        String sickNotice = sick > 0 ? Messages.of(ErrorCode.ANIMAL_ENFERMO) : null;
        if (hungry.isEmpty()) {
            return Result.ok(new BuildingActionView(mapper.building(building), 0,
                    sickNotice != null ? sickNotice : Messages.NOTHING_TO_FEED));
        }
        int hay = hungry.size() * GameRules.HAY_PER_ANIMAL;
        if (state.inventory().count(ItemType.HAY) < hay) {
            return Result.failWith(ErrorCode.SIN_HENO, hay);
        }
        int energyCost = hungry.size() * GameRules.ENERGY_COST_FEED_PER_ANIMAL;
        Energy energy = state.energy();
        if (!energy.canSpend(energyCost)) {
            return Result.failWith(ErrorCode.SIN_ENERGIA, energyCost, energy.current());
        }
        state.inventory().remove(ItemType.HAY, hay);
        energy.trySpend(energyCost);
        hungry.forEach(Animal::feed);
        publishChange(state, true);
        return Result.ok(new BuildingActionView(mapper.building(building), hungry.size(), sickNotice));
    }

    /**
     * Moves every unit waiting in a building to the inventory: 1 energy, all or nothing (RN-26).
     *
     * @param buildingId id of the building
     */
    public Result<BuildingActionView> collect(int buildingId) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        Building building = building(state, buildingId);
        int pending = building.pendingProducts();
        if (pending == 0) {
            return Result.ok(new BuildingActionView(mapper.building(building), 0, Messages.NOTHING_TO_COLLECT));
        }
        Energy energy = state.energy();
        if (!energy.canSpend(GameRules.ENERGY_COST_COLLECT)) {
            return Result.failWith(ErrorCode.SIN_ENERGIA, GameRules.ENERGY_COST_COLLECT, energy.current());
        }
        if (!state.inventory().add(building.product(), pending)) {
            return Result.failWith(ErrorCode.INVENTARIO_LLENO);
        }
        building.collectAll();
        energy.trySpend(GameRules.ENERGY_COST_COLLECT);
        publishChange(state, true);
        return Result.ok(new BuildingActionView(mapper.building(building), pending, null));
    }

    /**
     * Cures a sick animal with one medicine and 1 energy; it is healthy at once (RN-27).
     *
     * @param buildingId  id of the building
     * @param animalIndex position of the animal inside the building
     */
    public Result<BuildingActionView> cure(int buildingId, int animalIndex) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        Building building = building(state, buildingId);
        if (animalIndex < 0 || animalIndex >= building.animals().size()) {
            throw new IllegalArgumentException("No animal " + animalIndex + " in building " + buildingId);
        }
        Animal animal = building.animals().get(animalIndex);
        if (!animal.isSick()) {
            return Result.ok(new BuildingActionView(mapper.building(building), 0, Messages.NOT_SICK));
        }
        if (state.inventory().count(ItemType.MEDICINE) < GameRules.MEDICINE_PER_CURE) {
            return Result.failWith(ErrorCode.SIN_MEDICINA);
        }
        Energy energy = state.energy();
        if (!energy.canSpend(GameRules.ENERGY_COST_CURE)) {
            return Result.failWith(ErrorCode.SIN_ENERGIA, GameRules.ENERGY_COST_CURE, energy.current());
        }
        state.inventory().remove(ItemType.MEDICINE, GameRules.MEDICINE_PER_CURE);
        energy.trySpend(GameRules.ENERGY_COST_CURE);
        animal.cure();
        state.progress().recordCure();
        publishChange(state, true);
        achievements.check(state);
        return Result.ok(new BuildingActionView(mapper.building(building), 1, null));
    }

    private static Building building(GameState state, int buildingId) {
        List<Building> buildings = state.farm().buildings();
        if (buildingId < 0 || buildingId >= buildings.size()) {
            throw new IllegalArgumentException("No building " + buildingId);
        }
        return buildings.get(buildingId);
    }

    private void publishChange(GameState state, boolean inventoryChanged) {
        events.publish(new GameEvent.MapChanged());
        events.publish(new GameEvent.EnergyChanged(state.energy().current(), state.energy().max()));
        if (inventoryChanged) {
            events.publish(new GameEvent.InventoryChanged());
        }
    }
}
