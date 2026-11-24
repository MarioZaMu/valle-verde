package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.farm.AvatarSnapshot;
import edu.ucc.farmsim.domain.farm.Direction;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.game.GameStateValidator;
import edu.ucc.farmsim.infrastructure.persistence.Checksum;
import edu.ucc.farmsim.infrastructure.persistence.SaveCodec;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-45: caminar con el avatar en la granja (RN-36). */
class HU45AvatarMovementTest {

    private static final String BLOCKED = "No puedes pasar por ahí.";

    private static void place(TestApp app, int col, int row, Direction facing) {
        app.state().avatar().restoreFrom(new AvatarSnapshot(col, row, facing));
    }

    private static AvatarView avatar(TestApp app) {
        return app.controller.map().value().avatar();
    }

    @Test
    void HU45_ca1_aStepEastMovesOnePlotLooksEastAndCostsNoEnergyNorTime() {
                // Dada una partida nueva: el avatar en su parcela de pasto, mirando al sur, con la parcela del este libre
        TestApp app = TestApp.startedGame();
        assertEquals(new AvatarView(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW, "SOUTH"), avatar(app));
        HudView before = app.controller.hud().value();

                // Cuando el jugador da un paso al este
        Result<AvatarView> result = app.controller.move("EAST");

                // Entonces está una parcela al este, mira al este, y ni la energía ni el día cambiaron
        AvatarView moved = new AvatarView(GameRules.AVATAR_START_COL + 1, GameRules.AVATAR_START_ROW, "EAST");
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(moved, result.value());
        assertEquals(moved, avatar(app));
        assertEquals(before, app.controller.hud().value());
        assertEquals(java.util.List.of(new GameEvent.AvatarMoved(moved)), app.events);
    }

    @Test
    void HU45_ca1_soilWithACropCanBeWalkedOnAndTheWalkIsUndoneWithTheRestOfTheGame() {
        TestApp app = TestApp.startedGame();
        assertTrue(app.controller.plant(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW - 1, "CARROT_SEED").isOk());

        assertTrue(app.controller.move("NORTH").isOk());

        assertEquals(new AvatarView(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW - 1, "NORTH"), avatar(app));
        GameMemento saved = app.state().createMemento();
        app.controller.move("NORTH");
        app.state().restore(saved);
        assertEquals(new AvatarView(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW - 1, "NORTH"), avatar(app));
    }

    @Test
    void HU45_ca2_waterTheEdgeAnObstacleAndABuildingBlockTheWayButTheAvatarTurns() {
        TestApp app = TestApp.startedGame();
        GameFixtures.coop(app.state());
        int energy = app.state().energy().current();
        Object[][] cases = {
                        // dónde está, hacia dónde mira, hacia dónde intenta ir
            {12, 0, Direction.SOUTH, "EAST"},  // el estanque
            {12, 0, Direction.EAST, "NORTH"},  // el borde superior del mapa
            {7, 8, Direction.NORTH, "SOUTH"},  // un árbol en (7, 9)
            {4, 7, Direction.SOUTH, "WEST"},   // el gallinero cubre (2..3, 7..8)
        };
        for (Object[] c : cases) {
            place(app, (int) c[0], (int) c[1], (Direction) c[2]);
            app.events.clear();

            Result<AvatarView> result = app.controller.move((String) c[3]);

            assertFalse(result.isOk(), "towards " + c[3] + " from (" + c[0] + ", " + c[1] + ")");
            assertEquals(ErrorCode.MOVIMIENTO_BLOQUEADO, result.code());
            assertEquals(BLOCKED, result.message());
            assertEquals(new AvatarView((int) c[0], (int) c[1], (String) c[3]), avatar(app), "it only turned");
            assertTrue(app.events.isEmpty(), "a rejection publishes nothing");
            assertEquals(energy, app.state().energy().current());
        }
    }

    @Test
    void HU45_ca2_aBuildingIsNeverPlacedOnTheAvatarAndWithoutAGameThereIsNoStep() {
        TestApp app = TestApp.startedGame();
        place(app, GameFixtures.GRASS_COL + 1, GameFixtures.GRASS_ROW + 1, Direction.SOUTH);

        Result<?> build = app.controller.build("COOP", GameFixtures.GRASS_COL, GameFixtures.GRASS_ROW);

        assertEquals(ErrorCode.UBICACION_INVALIDA, build.code());
        assertEquals(ErrorCode.SIN_PARTIDA, TestApp.create().controller.move("EAST").code());
        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.move("UP").code(), "not a direction");
    }

    @Test
    void HU45_ca4_positionAndDirectionSurviveSavingAndLoading() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        place(app, 12, 3, Direction.WEST);
        assertTrue(app.controller.save(1).isOk());
        assertEquals(SaveCodec.SCHEMA_VERSION, mongo.document(1).schemaVersion());

        app.controller.newGame(TestApp.SEED + 1);
        assertEquals(new AvatarView(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW, "SOUTH"), avatar(app));
        Result<HudView> loaded = app.controller.load(1);

        assertTrue(loaded.isOk(), String.valueOf(loaded.message()));
        assertEquals(new AvatarView(12, 3, "WEST"), avatar(app));
    }

    @Test
    void HU45_ca4_aSaveOfVersionOneWithoutAvatarLoadsWithTheAvatarAtItsStart() throws Exception {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        place(app, 12, 3, Direction.WEST);
        app.controller.save(2);
        mongo.tamper(2, HU45AvatarMovementTest::asVersionOne);
        assertFalse(mongo.document(2).state().contains("avatar"));
        place(app, 1, 1, Direction.EAST);

        Result<HudView> loaded = app.controller.load(2);

        assertTrue(loaded.isOk(), String.valueOf(loaded.message()));
        assertEquals(new AvatarView(GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW, "SOUTH"), avatar(app));
    }

    @Test
    void HU45_ca4_withoutAvatarItGoesToTheNearestFreePlotAndASaveWithTheAvatarInThePondIsRejected() {
        FarmMap farm = FarmMap.initial();
        assertArrayEquals(new int[] {7, 7}, farm.nearestWalkable(7, 7));
        farm.place(BuildingType.COOP, 7, 7);
        assertArrayEquals(new int[] {7, 6}, farm.nearestWalkable(7, 7), "the soil just north of the coop");
        assertArrayEquals(new int[] {12, 1}, farm.nearestWalkable(13, 1), "next to the pond");

        TestApp app = TestApp.startedGame();
        GameFixtures.build(app.state(), BuildingType.COOP, GameRules.AVATAR_START_COL, GameRules.AVATAR_START_ROW);
        GameMemento good = app.state().createMemento();
        GameMemento v1 = new GameMemento(good.date(), good.money(), good.energy(), good.inventory(), good.farm(),
                good.today(), good.forecast(), good.randomState(), good.market(), good.ledger(), good.progress());
        app.state().restore(v1);
        assertEquals(new AvatarView(7, 6, "SOUTH"), avatar(app));
        assertTrue(GameStateValidator.violations(v1).isEmpty());

        GameMemento inThePond = new GameMemento(good.date(), good.money(), good.energy(), good.inventory(),
                good.farm(), good.today(), good.forecast(), good.randomState(), good.market(), good.ledger(),
                good.progress(), new AvatarSnapshot(13, 0, Direction.NORTH));
        GameMemento onTheCoop = new GameMemento(good.date(), good.money(), good.energy(), good.inventory(),
                good.farm(), good.today(), good.forecast(), good.randomState(), good.market(), good.ledger(),
                good.progress(), new AvatarSnapshot(7, 7, Direction.NORTH));
        GameMemento lookingNowhere = new GameMemento(good.date(), good.money(), good.energy(), good.inventory(),
                good.farm(), good.today(), good.forecast(), good.randomState(), good.market(), good.ledger(),
                good.progress(), new AvatarSnapshot(5, 8, null));
        assertFalse(GameStateValidator.violations(inThePond).isEmpty());
        assertFalse(GameStateValidator.violations(onTheCoop).isEmpty());
        assertFalse(GameStateValidator.violations(lookingNowhere).isEmpty());
    }

    @Test
    void HU45_ca4_documentsOfAnUnknownVersionAreStillRejected() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        app.controller.save(1);
        for (int version : new int[] {0, SaveCodec.SCHEMA_VERSION + 1}) {
            mongo.tamper(1, d -> new SaveDocument(d.id(), version, d.savedAt(), d.state(), d.checksum()));
            assertEquals(ErrorCode.VERSION_NO_SOPORTADA, app.controller.load(1).code(), "version " + version);
        }
    }

    /**
     * La misma partida escrita como la escribía la versión 1: sin avatar en el estado, versión 1, suma de control del
     * texto.
     */
    private static SaveDocument asVersionOne(SaveDocument document) {
        try {
            ObjectMapper json = new ObjectMapper();
            ObjectNode state = (ObjectNode) json.readTree(document.state());
            state.remove("avatar");
            String text = json.writeValueAsString(state);
            return new SaveDocument(document.id(), 1, document.savedAt(), text, Checksum.of(text));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
