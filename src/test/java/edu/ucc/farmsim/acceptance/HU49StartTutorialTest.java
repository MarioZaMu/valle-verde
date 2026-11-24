package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucc.farmsim.application.dto.TutorialView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.farm.AvatarSnapshot;
import edu.ucc.farmsim.domain.farm.Direction;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.infrastructure.persistence.Checksum;
import edu.ucc.farmsim.infrastructure.persistence.SaveCodec;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.TestApp;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-49: el tutorial de inicio en el que el avatar guía al jugador (RN-39). */
class HU49StartTutorialTest {

    /** Una parcela de tierra al norte del inicio del avatar, a dos parcelas de él. */
    private static final int SOIL_COL = 8;
    private static final int SOIL_ROW = 6;

    /** Una partida nueva con su tutorial, como la recibe el jugador. */
    private static TestApp newGame() {
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        app.events.clear();
        return app;
    }

    private static TutorialView tutorial(TestApp app) {
        Result<TutorialView> result = app.controller.tutorial();
        assertTrue(result.isOk(), String.valueOf(result.message()));
        return result.value();
    }

    private static List<TutorialView> advances(TestApp app) {
        return app.events.stream().filter(GameEvent.TutorialAdvanced.class::isInstance)
                .map(e -> ((GameEvent.TutorialAdvanced) e).tutorial()).toList();
    }

    /** Las cinco acciones del tutorial, en orden; cada una debe ser aceptada. */
    private static void play(TestApp app, int actions) {
        Runnable[] steps = {
            () -> assertTrue(app.controller.move("EAST").isOk()),
            () -> assertTrue(app.controller.plant(SOIL_COL, SOIL_ROW, "CARROT_SEED").isOk()),
            () -> assertTrue(app.controller.water(SOIL_COL, SOIL_ROW).isOk()),
            () -> {
                HU46NpcDialogueTest.nextTo(app, Npc.MARTA);
                assertTrue(app.controller.talk("MARTA").isOk());
            },
            () -> assertTrue(app.controller.endDay().isOk()),
        };
        for (int i = 0; i < actions; i++) {
            steps[i].run();
        }
    }

    @Test
    void HU49_ca1_aNewGameStartsAtStepOneOfFiveAskingToWalk() {
        TestApp app = newGame();

        TutorialView view = tutorial(app);

        assertEquals("WALK", view.step());
        assertEquals(1, view.number());
        assertEquals(5, view.total());
        assertFalse(view.finished());
        assertEquals("¡Hola! Soy tu granjero. Camina por la granja con las flechas o con W A S D.", view.text());
        assertEquals(ErrorCode.SIN_PARTIDA, TestApp.create().controller.tutorial().code());
    }

    @Test
    void HU49_ca2_walkingSowingWateringTalkingAndEndingTheDayGoThroughTheFiveStepsInOrder() {
        TestApp app = newGame();

        play(app, 5);

        List<TutorialView> seen = advances(app);
        assertEquals(List.of("SOW", "WATER", "TALK", "END_DAY", "DONE"), seen.stream().map(TutorialView::step).toList());
        assertEquals(List.of(2, 3, 4, 5, 5), seen.stream().map(TutorialView::number).toList());
        assertEquals("Elige Sembrar con la tecla 1, ponte frente a la tierra y pulsa Espacio "
                + "(o haz clic en una parcela de tierra).", seen.get(0).text());
        assertEquals("Ahora elige Regar con la tecla 2 y riega lo que sembraste.", seen.get(1).text());
        assertEquals("Los vecinos viven en la aldea, al este por el camino. Acércate a uno y pulsa E.", seen.get(2).text());
        assertEquals("Cuando acabes, pulsa Enter o «Terminar día» para que crezcan tus cultivos.", seen.get(3).text());
        TutorialView done = tutorial(app);
        assertTrue(done.finished());
        assertEquals("¡Listo! Ya sabes lo básico. ¡Buena suerte en el valle!", done.text());
        assertEquals(seen.get(4), done);
    }

    @Test
    void HU49_ca2_theStepChangesRightAfterTheEventsOfTheActionThatCompletedIt() {
        TestApp app = newGame();

        assertTrue(app.controller.move("EAST").isOk());

        assertEquals(2, app.events.size());
        assertTrue(app.events.get(0) instanceof GameEvent.AvatarMoved);
        assertEquals("SOW", ((GameEvent.TutorialAdvanced) app.events.get(1)).tutorial().step());
    }

    @Test
    void HU49_ca3_actionsOfALaterStepAndRejectedActionsDoNotMoveTheTutorial() {
        TestApp app = newGame();

        assertTrue(app.controller.endDay().isOk());
        assertTrue(app.controller.plant(SOIL_COL, SOIL_ROW, "CARROT_SEED").isOk());
        app.state().avatar().restoreFrom(new AvatarSnapshot(7, 8, Direction.NORTH));
        assertEquals(ErrorCode.MOVIMIENTO_BLOQUEADO, app.controller.move("SOUTH").code(), "a tree at (7, 9)");

        assertEquals("WALK", tutorial(app).step());
        assertTrue(advances(app).isEmpty());

        assertTrue(app.controller.move("NORTH").isOk());
        assertFalse(app.controller.plant(0, 0, "CARROT_SEED").isOk(), "an obstacle");
        assertFalse(app.controller.plant(GameFixtures.GRASS_COL, GameFixtures.GRASS_ROW, "CARROT_SEED").isOk());
        assertEquals("SOW", tutorial(app).step(), "a rejected sowing does not count");
    }

    @Test
    void HU49_ca4_skippingFinishesTheTutorialAtAnyStepAndItNeverComesBack() {
        TestApp app = newGame();
        play(app, 2);

        Result<TutorialView> skipped = app.controller.skipTutorial();

        assertTrue(skipped.isOk());
        assertTrue(skipped.value().finished());
        assertEquals("SKIPPED", skipped.value().step());
        assertEquals(skipped.value(), advances(app).get(advances(app).size() - 1));
        app.events.clear();
        play2to5(app);
        assertTrue(tutorial(app).finished());
        assertTrue(advances(app).isEmpty(), "nothing moves a finished tutorial");
        assertTrue(app.controller.skipTutorial().isOk(), "skipping twice is harmless");
        assertTrue(advances(app).isEmpty());
        assertEquals(ErrorCode.SIN_PARTIDA, TestApp.create().controller.skipTutorial().code());
    }

    @Test
    void HU49_ca4_theTutorialNeverChangesEnergyMoneyInventoryNorTheDay() {
        TestApp guided = newGame();
        TestApp skipped = newGame();
        skipped.controller.skipTutorial();

        play(guided, 5);
        play(skipped, 5);

        assertEquals(skipped.controller.hud().value(), guided.controller.hud().value());
        assertEquals(skipped.controller.inventory().value(), guided.controller.inventory().value());
        assertEquals(skipped.controller.map().value(), guided.controller.map().value());
    }

    @Test
    void HU49_ca5_theStepSurvivesSavingAndLoadingAndAnOlderSaveLoadsWithTheTutorialFinished() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        play(app, 2);
        assertEquals("WATER", tutorial(app).step());
        assertTrue(app.controller.save(1).isOk());
        app.controller.save(2);
        mongo.tamper(2, HU49StartTutorialTest::withoutTutorial);
        assertEquals(SaveCodec.SCHEMA_VERSION, mongo.document(2).schemaVersion(), "the version did not change");

        app.controller.newGame(TestApp.SEED + 1);
        assertEquals("WALK", tutorial(app).step());
        assertTrue(app.controller.load(1).isOk());
        assertEquals("WATER", tutorial(app).step());
        assertEquals(3, tutorial(app).number());

        Result<?> older = app.controller.load(2);
        assertTrue(older.isOk(), String.valueOf(older.message()));
        assertTrue(tutorial(app).finished(), "a game that was already being played does not repeat it");
    }

    private static void play2to5(TestApp app) {
        assertTrue(app.controller.water(SOIL_COL, SOIL_ROW).isOk());
        HU46NpcDialogueTest.nextTo(app, Npc.MARTA);
        assertTrue(app.controller.talk("MARTA").isOk());
        assertTrue(app.controller.endDay().isOk());
    }

    /** El mismo documento que una partida escrita antes de que existiera el tutorial: sin tutorial en el estado. */
    private static SaveDocument withoutTutorial(SaveDocument document) {
        try {
            ObjectMapper json = new ObjectMapper();
            ObjectNode state = (ObjectNode) json.readTree(document.state());
            assertTrue(state.has("tutorial"));
            state.remove("tutorial");
            String text = json.writeValueAsString(state);
            return new SaveDocument(document.id(), document.schemaVersion(), document.savedAt(), text,
                    Checksum.of(text));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
