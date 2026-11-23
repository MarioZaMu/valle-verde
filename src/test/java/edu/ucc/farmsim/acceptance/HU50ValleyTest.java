package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucc.farmsim.application.dto.HouseView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.farm.AvatarSnapshot;
import edu.ucc.farmsim.domain.farm.Direction;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.farm.House;
import edu.ucc.farmsim.domain.farm.Terrain;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.infrastructure.persistence.Checksum;
import edu.ucc.farmsim.infrastructure.persistence.SaveCodec;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.TestApp;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** HU-50: a valley with the farm to the west and a village to the east, joined by a path (RN-04, RN-37, RN-40). */
class HU50ValleyTest {

    /** The first village column: everything east of it belongs to the village. */
    private static final int VILLAGE_COL = 26;

    @Test
    void HU50_ca1_theValleyIs48By24WithTheFarmToTheWestAndTheVillageWithItsHousesToTheEast() {
        FarmMap farm = FarmMap.initial();

        assertEquals(48, farm.cols());
        assertEquals(24, farm.rows());
        assertEquals(40, farm.count(Terrain.SOIL));
        assertEquals(976, farm.count(Terrain.GRASS));
        assertEquals(6, farm.count(Terrain.WATER));
        assertEquals(30, farm.count(Terrain.OBSTACLE));
        assertEquals(34, farm.count(Terrain.PATH));
        assertEquals(66, farm.count(Terrain.HOUSE));
        assertEquals(Terrain.SOIL, farm.plotAt(4, 2).terrain(), "the farm keeps its soil where it was");
        assertEquals(Terrain.WATER, farm.plotAt(13, 0).terrain());
        assertTrue(House.HOME.col() < VILLAGE_COL, "your house is on the farm");
        for (House house : List.of(House.SHOP, House.FARMHOUSE, House.CLINIC, House.COTTAGE)) {
            assertTrue(house.col() >= VILLAGE_COL, house + " is in the village");
        }
    }

    @Test
    void HU50_ca1_eachPersonStandsInTheVillageInFrontOfItsHouseAndTheMapShowsTheHouses() {
        TestApp app = TestApp.startedGame();
        MapView map = app.controller.map().value();
        Map<String, House> homes = Map.of("MARTA", House.SHOP, "TOMAS", House.FARMHOUSE, "LUCIA", House.CLINIC,
                "INES", House.COTTAGE);

        assertEquals(48, map.cols());
        assertEquals(5, map.houses().size());
        for (NpcView npc : map.npcs()) {
            House house = homes.get(npc.id());
            assertTrue(npc.col() >= VILLAGE_COL, npc.name() + " lives in the village");
            assertTrue(npc.col() >= house.col() && npc.col() < house.col() + house.width(), npc.name());
            assertTrue(npc.row() == house.row() - 1 || npc.row() == house.row() + house.height(),
                    npc.name() + " stands right in front of its house");
            HouseView view = map.houses().stream().filter(h -> h.id().equals(house.name())).findFirst().orElseThrow();
            assertEquals(house.col(), view.col());
            assertEquals(house.width(), view.width());
        }
        assertEquals(29, Npc.MARTA.col());
        assertEquals(11, Npc.MARTA.row());
    }

    @Test
    void HU50_ca2_housesBlockTheWayThePathIsWalkedAndNeitherIsSownNorBuiltOn() {
        TestApp app = TestApp.startedGame();
        int energy = app.state().energy().current();
        House shop = House.SHOP;
        app.state().avatar().restoreFrom(new AvatarSnapshot(shop.col(), shop.row() - 1, Direction.NORTH));

        assertEquals(ErrorCode.MOVIMIENTO_BLOQUEADO, app.controller.move("SOUTH").code(), "into the shop");
        app.state().avatar().restoreFrom(new AvatarSnapshot(18, 7, Direction.NORTH));
        assertTrue(app.controller.move("SOUTH").isOk(), "along the path");
        assertEquals("PATH", app.controller.map().value().plotAt(18, 8).terrain());

        assertFalse(app.controller.plant(20, 12, "CARROT_SEED").isOk(), "on the path");
        assertFalse(app.controller.plant(shop.col(), shop.row(), "CARROT_SEED").isOk(), "on a house");
        assertEquals(ErrorCode.UBICACION_INVALIDA, app.controller.build("COOP", 20, 12).code());
        assertEquals(ErrorCode.UBICACION_INVALIDA, app.controller.build("COOP", shop.col() - 1, shop.row()).code());
        assertEquals(energy, app.state().energy().current());
    }

    @Test
    void HU50_ca6_aSaveOfTheOld16By10MapLoadsWithEverythingOnTheSamePlotsAndIsSavedAsVersion3() throws Exception {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        app.controller.skipTutorial();
        assertTrue(app.controller.plant(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW, "CARROT_SEED").isOk());
        GameFixtures.build(app.state(), BuildingType.COOP, 9, 7);
        app.state().avatar().restoreFrom(new AvatarSnapshot(12, 3, Direction.WEST));
        assertTrue(app.controller.save(1).isOk());
        assertEquals(3, mongo.document(1).schemaVersion());
        mongo.tamper(1, HU50ValleyTest::asVersionTwo);
        assertEquals(GameRules.LEGACY_MAP_COLS * GameRules.LEGACY_MAP_ROWS,
                new ObjectMapper().readTree(mongo.document(1).state()).path("farm").path("crops").size());

        app.controller.newGame(TestApp.SEED + 1);
        var loaded = app.controller.load(1);

        assertTrue(loaded.isOk(), String.valueOf(loaded.message()));
        assertNotNull(app.controller.inspect(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW).value().crop());
        assertEquals("COOP", app.controller.map().value().buildingAt(9, 7).type());
        assertEquals(12, app.controller.map().value().avatar().col());
        assertEquals(48 * 24, app.state().createMemento().farm().crops().size());
        assertTrue(app.controller.save(1).isOk());
        assertEquals(SaveCodec.SCHEMA_VERSION, mongo.document(1).schemaVersion());
        assertEquals(3, SaveCodec.SCHEMA_VERSION);
    }

    /** The same game as version 2 wrote it: a list of 16 x 10 crops (the north-west corner of the valley). */
    private static SaveDocument asVersionTwo(SaveDocument document) {
        try {
            ObjectMapper json = new ObjectMapper();
            ObjectNode state = (ObjectNode) json.readTree(document.state());
            ArrayNode crops = (ArrayNode) state.path("farm").path("crops");
            ArrayNode old = json.createArrayNode();
            for (int row = 0; row < GameRules.LEGACY_MAP_ROWS; row++) {
                for (int col = 0; col < GameRules.LEGACY_MAP_COLS; col++) {
                    old.add(crops.get(row * GameRules.MAP_COLS + col));
                }
            }
            ((ObjectNode) state.path("farm")).set("crops", old);
            String text = json.writeValueAsString(state);
            return new SaveDocument(document.id(), 2, document.savedAt(), text, Checksum.of(text));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
