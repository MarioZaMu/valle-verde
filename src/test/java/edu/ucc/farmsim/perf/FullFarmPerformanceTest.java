package edu.ucc.farmsim.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.infrastructure.persistence.SaveCodec;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.TestApp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Rendimiento a plena carga (RNF-01, RNF-03): una granja con 40 cultivos a la intemperie, 9 en el invernadero, seis
 * edificios y 16 animales.
 */
class FullFarmPerformanceTest {

    private static final int DAYS = 100;
    private static final int P95_LIMIT_MS = 200;
    private static final int SAVE_LIMIT_MS = 1000;
    private static final int DOCUMENT_LIMIT_BYTES = 200 * 1024;

    /** Una granja con todas las parcelas de tierra sembradas, seis edificios y dieciséis animales. */
    static TestApp fullFarm(InMemoryMongo mongo) {
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        GameState state = app.state();
        for (int col = GameRules.SOIL_COL; col < GameRules.SOIL_COL + GameRules.SOIL_WIDTH; col++) {
            for (int row = GameRules.SOIL_ROW; row < GameRules.SOIL_ROW + GameRules.SOIL_HEIGHT; row++) {
                GameFixtures.plant(state, col, row, CropType.CORN, 3, 1, true, false);
            }
        }
        List<Building> animalHomes = new ArrayList<>();
        animalHomes.add(GameFixtures.build(state, BuildingType.COOP, 2, 7));
        animalHomes.add(GameFixtures.build(state, BuildingType.COOP, 5, 7));
        animalHomes.add(GameFixtures.build(state, BuildingType.BARN, 8, 7));
        animalHomes.add(GameFixtures.build(state, BuildingType.BARN, 11, 7));
        GameFixtures.build(state, BuildingType.WAREHOUSE, 2, 5);
        GameFixtures.build(state, BuildingType.GREENHOUSE, 12, 2);
        for (int col = 12; col < 15; col++) {
            for (int row = 2; row < 5; row++) {
                GameFixtures.plant(state, col, row, CropType.PUMPKIN, 3, 1, true, false);
            }
        }
        state.inventory().addSlots(12);
        for (Building home : animalHomes) {
            for (int i = 0; i < GameRules.ANIMALS_PER_BUILDING; i++) {
                GameFixtures.house(home, home.species(), i == 3 ? AnimalCondition.SICK : AnimalCondition.HEALTHY,
                        i == 3 ? 3 : 0, i % 2 == 0, 1);
            }
        }
        for (ItemType item : ItemType.values()) {
            state.inventory().add(item, 10);
        }
        GameFixtures.setMoney(state, 2500);
        return app;
    }

    @Test
    void HU10_ca3_endingTheDayOnAFullFarmTakesAtMost200msAtThe95thPercentile() {
        TestApp app = fullFarm(new InMemoryMongo());
        for (int i = 0; i < 10; i++) { // warm-up
            app.controller.endDay();
        }
        List<Long> millis = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            long start = System.nanoTime();
            assertTrue(app.controller.endDay().isOk());
            millis.add((System.nanoTime() - start) / 1_000_000);
        }
        Collections.sort(millis);
        long p95 = millis.get((int) Math.ceil(DAYS * 0.95) - 1);

        assertTrue(p95 <= P95_LIMIT_MS, "p95 was " + p95 + " ms");
    }

    @Test
    void HU29_ca4_aFullFarmIsSavedInAtMostOneSecondInAtMost200Kb() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = fullFarm(mongo);
        assertEquals(6, app.state().farm().buildings().size());
        assertEquals(16, app.state().farm().buildings().stream().mapToInt(b -> b.animals().size()).sum());

        long start = System.nanoTime();
        assertTrue(app.controller.save(1).isOk());
        long millis = (System.nanoTime() - start) / 1_000_000;

        int bytes = mongo.document(1).state().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        assertTrue(millis <= SAVE_LIMIT_MS, "saving took " + millis + " ms");
        assertTrue(bytes <= DOCUMENT_LIMIT_BYTES, "the document weighs " + bytes + " bytes");
    }

    @Test
    void HU30_ca4_aFullFarmIsLoadedInAtMostOneSecondAndIsIdentical() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = fullFarm(mongo);
        GameMemento saved = app.state().createMemento();
        app.controller.save(2);
        TestApp other = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());

        long start = System.nanoTime();
        assertTrue(other.controller.load(2).isOk());
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(millis <= SAVE_LIMIT_MS, "loading took " + millis + " ms");
        assertEquals(saved, other.state().createMemento());
    }

    @Test
    void theCodecRoundTripOfAFullFarmKeepsEveryPart() {
        TestApp app = fullFarm(new InMemoryMongo());
        GameMemento memento = app.state().createMemento();
        SaveCodec codec = new SaveCodec();

        SaveDocument document = codec.encode(1, memento, Instant.EPOCH);

        assertEquals(memento, codec.decode(document));
    }
}
