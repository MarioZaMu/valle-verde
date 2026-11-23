package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.game.GameStateValidator;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.infrastructure.persistence.Checksum;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.TestApp;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-47: the errands of the people of the valley (RN-38). */
class HU47ErrandsTest {

    private static TestApp nextToMarta(int carrots) {
        TestApp app = TestApp.startedGame();
        HU46NpcDialogueTest.nextTo(app, Npc.MARTA);
        app.state().inventory().add(ItemType.CARROT, carrots);
        app.events.clear();
        return app;
    }

    @Test
    void HU47_ca1_deliveringFiveCarrotsToMartaPaysSixtyAndDoesTheErrand() {
        TestApp app = nextToMarta(5);

        Result<DialogueView> result = app.controller.deliver("MARTA");

        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(0, app.state().inventory().count(ItemType.CARROT));
        assertEquals(560, app.state().money());
        assertTrue(app.state().errands().isDone(Npc.MARTA));
        assertTrue(result.value().errandDone());
        assertEquals("¡Gracias otra vez por traerme 5 Zanahorias!", result.value().errand());
        assertTrue(app.events.containsAll(List.of(new GameEvent.InventoryChanged(), new GameEvent.MoneyChanged(560),
                new GameEvent.ErrandCompleted("MARTA", "Marta", 60))), app.events.toString());
        assertTrue(app.controller.npcs().value().get(0).errandDone());
    }

    @Test
    void HU47_ca1_theOtherErrandsAskForTheirOwnItems() {
        TestApp app = TestApp.startedGame();
        int money = app.state().money();
        for (Npc npc : List.of(Npc.TOMAS, Npc.LUCIA, Npc.INES)) {
            HU46NpcDialogueTest.nextTo(app, npc);
            app.state().inventory().add(npc.errandItem(), npc.errandAmount());
            assertTrue(app.controller.deliver(npc.name()).isOk(), npc.name());
            money += npc.errandReward();
        }
        assertEquals(500 + 50 + 30 + 80, money);
        assertEquals(money, app.state().money());
    }

    @Test
    void HU47_ca2_withOnlyThreeCarrotsTheDeliveryIsRejectedAndNothingChanges() {
        TestApp app = nextToMarta(3);
        GameMemento before = app.state().createMemento();

        Result<DialogueView> result = app.controller.deliver("MARTA");

        assertEquals(ErrorCode.ENCARGO_INCOMPLETO, result.code());
        assertEquals("Te faltan 2 Zanahorias para el encargo.", result.message());
        assertEquals(before, app.state().createMemento());
        assertTrue(app.events.isEmpty());
        app.state().inventory().add(ItemType.CARROT, 1);
        assertEquals("Te faltan 1 Zanahoria para el encargo.", app.controller.deliver("MARTA").message());
        app.state().avatar().restoreFrom(new edu.ucc.farmsim.domain.farm.AvatarSnapshot(7, 7,
                edu.ucc.farmsim.domain.farm.Direction.SOUTH));
        assertEquals(ErrorCode.NPC_LEJOS, app.controller.deliver("MARTA").code());
    }

    @Test
    void HU47_ca3_aDoneErrandIsThankedAndNeverPaidAgainEvenAfterSavingAndLoading() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        HU46NpcDialogueTest.nextTo(app, Npc.MARTA);
        app.state().inventory().add(ItemType.CARROT, 10);
        assertTrue(app.controller.deliver("MARTA").isOk());

        Result<DialogueView> again = app.controller.deliver("MARTA");

        assertTrue(again.isOk());
        assertEquals(560, app.state().money(), "not paid twice");
        assertEquals(5, app.state().inventory().count(ItemType.CARROT));
        assertEquals("¡Gracias otra vez por traerme 5 Zanahorias!", app.controller.talk("MARTA").value().errand());

        assertTrue(app.controller.save(1).isOk());
        app.controller.newGame(TestApp.SEED);
        assertFalse(app.state().errands().isDone(Npc.MARTA));
        assertTrue(app.controller.load(1).isOk());
        assertTrue(app.state().errands().isDone(Npc.MARTA));
    }

    @Test
    void HU47_ca3_aSaveWrittenBeforeTheErrandsLoadsWithNoneDoneAndABrokenListIsRejected() throws Exception {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        app.state().errands().complete(Npc.INES);
        app.controller.save(2);
        mongo.tamper(2, d -> {
            try {
                ObjectMapper json = new ObjectMapper();
                ObjectNode state = (ObjectNode) json.readTree(d.state());
                state.remove("errandsDone");
                String text = json.writeValueAsString(state);
                return new SaveDocument(d.id(), d.schemaVersion(), d.savedAt(), text, Checksum.of(text));
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });

        assertTrue(app.controller.load(2).isOk());
        assertFalse(app.state().errands().isDone(Npc.INES));

        GameMemento good = app.state().createMemento();
        GameMemento twice = new GameMemento(good.date(), good.money(), good.energy(), good.inventory(), good.farm(),
                good.today(), good.forecast(), good.randomState(), good.market(), good.ledger(), good.progress(),
                good.avatar(), List.of(Npc.MARTA, Npc.MARTA));
        GameMemento empty = new GameMemento(good.date(), good.money(), good.energy(), good.inventory(), good.farm(),
                good.today(), good.forecast(), good.randomState(), good.market(), good.ledger(), good.progress(),
                good.avatar(), Arrays.asList((Npc) null));
        assertFalse(GameStateValidator.violations(twice).isEmpty());
        assertFalse(GameStateValidator.violations(empty).isEmpty());
    }

    @Test
    void HU47_ca4_theRewardIsIncomeInTheSummaryOfTheDay() {
        TestApp app = nextToMarta(5);
        app.controller.deliver("MARTA");

        app.controller.endDay();

        DaySummaryView summary = app.events.stream().filter(GameEvent.DaySummaryReady.class::isInstance)
                .map(e -> ((GameEvent.DaySummaryReady) e).summary()).findFirst().orElseThrow();
        assertEquals(60, summary.income());
        assertEquals(500, summary.balanceBefore());
        assertEquals(560, summary.balanceAfter());
    }
}
