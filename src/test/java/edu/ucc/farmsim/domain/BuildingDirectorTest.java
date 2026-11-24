package edu.ucc.farmsim.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingDirector;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.farm.FarmMap;
import org.junit.jupiter.api.Test;

/** Los cuatro constructores y su director producen los edificios de RN-20; el mapa los conserva (RN-21). */
class BuildingDirectorTest {

    @Test
    void theCoopHousesFourHensAndNothingElse() {
        Building coop = BuildingDirector.construct(BuildingType.COOP, 0, 2, 7);

        assertEquals(AnimalType.HEN, coop.species());
        assertEquals(4, coop.animalCapacity());
        assertEquals(0, coop.extraInventorySlots());
        assertEquals(ItemType.EGG, coop.product());
        assertTrue(coop.covers(3, 8));
        assertEquals(false, coop.covers(4, 8));
    }

    @Test
    void theBarnHousesFourCowsAndIsThreeColumnsWide() {
        Building barn = BuildingDirector.construct(BuildingType.BARN, 1, 2, 7);

        assertEquals(AnimalType.COW, barn.species());
        assertEquals(4, barn.animalCapacity());
        assertEquals(3, barn.width());
        assertEquals(2, barn.height());
        assertEquals(ItemType.MILK, barn.product());
    }

    @Test
    void theWarehouseAddsTwelveSlotsAndHousesNobody() {
        Building warehouse = BuildingDirector.construct(BuildingType.WAREHOUSE, 0, 2, 7);

        assertEquals(12, warehouse.extraInventorySlots());
        assertEquals(0, warehouse.animalCapacity());
        assertNull(warehouse.product());
        assertEquals(false, warehouse.addAnimal(new Animal(AnimalType.HEN)));
    }

    @Test
    void theGreenhouseProtectsNinePlots() {
        Building greenhouse = BuildingDirector.construct(BuildingType.GREENHOUSE, 0, 2, 7);

        assertEquals(9, greenhouse.protectedPlots());
        assertEquals(3, greenhouse.width());
        assertEquals(3, greenhouse.height());
    }

    @Test
    void aCoopRefusesCowsAndAFifthHen() {
        Building coop = BuildingDirector.construct(BuildingType.COOP, 0, 2, 7);

        assertEquals(false, coop.addAnimal(new Animal(AnimalType.COW)));
        for (int i = 0; i < 4; i++) {
            assertTrue(coop.addAnimal(new Animal(AnimalType.HEN)));
        }
        assertEquals(false, coop.addAnimal(new Animal(AnimalType.HEN)));
    }

    @Test
    void theMapPlacesBuildingsOnlyOnFreeGrassAndRestoresThem() {
        FarmMap map = FarmMap.initial();
        Building coop = map.place(BuildingType.COOP, 2, 7);
        coop.addAnimal(new Animal(AnimalType.HEN));
        var snapshot = map.snapshot();

        assertThrows(IllegalStateException.class, () -> map.place(BuildingType.COOP, 3, 8));
        map.restoreFrom(new edu.ucc.farmsim.domain.farm.FarmMapSnapshot(snapshot.crops(), java.util.List.of()));
        assertTrue(map.buildings().isEmpty());
        assertNull(map.buildingAt(2, 7));
        map.restoreFrom(snapshot);

        assertEquals(1, map.buildings().size());
        assertEquals(1, map.buildings().get(0).animals().size());
        assertEquals(1, map.countBuildings(BuildingType.COOP));
        assertNull(map.buildingAt(-1, 0));
    }

    @Test
    void theHousingOfEachAnimalIsTheMatchingBuilding() {
        assertEquals(BuildingType.COOP, BuildingType.housing(AnimalType.HEN));
        assertEquals(BuildingType.BARN, BuildingType.housing(AnimalType.COW));
    }
}
