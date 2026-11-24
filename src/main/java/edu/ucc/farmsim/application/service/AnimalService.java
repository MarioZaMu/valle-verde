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
 * Los casos de uso de animales: comprar y alojar (RN-23), alimentar un edificio (RN-24), recoger sus productos (RN-26)
 * y curar a un animal enfermo (RN-27). Cada operación comprueba primero todas sus reglas y solo después cambia la
 * partida, de modo que un rechazo nunca deja nada a medias.
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
     * Crea el servicio.
     *
     * @param session la partida en curso
     * @param mapper  construye las vistas que dibuja la UI
     * @param events  donde se publican los eventos
     * @param achievements evalúa los logros después de comprar y curar
     */
    public AnimalService(GameSession session, ViewMapper mapper, EventBus events, AchievementService achievements) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
        this.achievements = achievements;
    }

    /**
     * Compra un animal y lo aloja en el primer edificio de su tipo con un lugar libre (RN-23).
     *
     * @param animalId id del animal, por ejemplo {@code HEN}
     * @return el edificio donde vive ahora el animal
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
     * Alimenta a todos los animales de un edificio que pueden comer y no han comido hoy: 1 de heno y 1 de energía cada
     * uno, todo o nada (RN-24). Los animales enfermos se omiten y se avisa al jugador.
     *
     * @param buildingId id del edificio
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
     * Mueve al inventario todas las unidades que esperan en un edificio: 1 de energía, todo o nada (RN-26).
     *
     * @param buildingId id del edificio
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
     * Cura a un animal enfermo con una medicina y 1 de energía; queda sano de inmediato (RN-27).
     *
     * @param buildingId  id del edificio
     * @param animalIndex posición del animal dentro del edificio
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
