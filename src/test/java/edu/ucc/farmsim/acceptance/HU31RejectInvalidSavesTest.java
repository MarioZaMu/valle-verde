package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.infrastructure.persistence.Checksum;
import edu.ucc.farmsim.infrastructure.persistence.SaveCodec;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.RichGame;
import edu.ucc.farmsim.support.TestApp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-31: las partidas dañadas, ausentes o no admitidas se rechazan sin tocar la partida en curso (RN-33). */
class HU31RejectInvalidSavesTest {

    private final InMemoryMongo mongo = new InMemoryMongo();
    private TestApp app;
    private GameMemento before;

    @BeforeEach
    void aGameIsInProgressAndSlot1HoldsAGoodSave() {
        app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        app.controller.endDay();
        app.controller.save(1);
        app.controller.endDay();
        before = app.state().createMemento();
        app.events.clear();
    }

    private void assertRejected(Result<HudView> result, ErrorCode code) {
        assertEquals(code, result.code(), String.valueOf(result.message()));
        assertEquals(before, app.state().createMemento(), "the game in progress must have 0 differences");
        assertTrue(app.events.isEmpty());
    }

    @Test
    void HU31_ca1_anEmptySlotIsRejectedAsInexistent_criticalTest() {
        Result<HudView> result = app.controller.load(3);

        assertRejected(result, ErrorCode.ARCHIVO_INEXISTENTE);
        assertEquals("No existe una partida en la ranura 3.", result.message());
    }

    @Test
    void HU31_ca2_aChangedCharacterIsRejectedAsCorrupt_criticalTest() {
        mongo.tamper(1, d -> new SaveDocument(d.id(), d.schemaVersion(), d.savedAt(),
                d.state().replaceFirst("\"money\":(\\d)", "\"money\":9"), d.checksum()));

        assertRejected(app.controller.load(1), ErrorCode.ARCHIVO_CORRUPTO);
    }

    @Test
    void HU31_ca2_aTruncatedJsonIsRejectedAsCorrupt_criticalTest() {
        mongo.tamper(1, d -> new SaveDocument(d.id(), d.schemaVersion(), d.savedAt(),
                d.state().substring(0, d.state().length() / 2), d.checksum()));

        Result<HudView> result = app.controller.load(1);

        assertRejected(result, ErrorCode.ARCHIVO_CORRUPTO);
        assertEquals("La partida está dañada y no se puede cargar. Tu partida actual no cambió.", result.message());
    }

    @Test
    void HU31_ca3_anUnknownSchemaVersionIsRejected() {
        mongo.tamper(1, d -> new SaveDocument(d.id(), 99, d.savedAt(), d.state(), d.checksum()));

        Result<HudView> result = app.controller.load(1);

        assertRejected(result, ErrorCode.VERSION_NO_SOPORTADA);
        assertEquals("La partida es de una versión no soportada.", result.message());
    }

    @Test
    void HU31_ca4_negativeMoneyWithAValidChecksumIsRejectedByTheInvariants() {
        GameFixtures.setMoney(app.state(), 100);
        GameMemento good = app.state().createMemento();
        GameMemento broken = new GameMemento(good.date(), -5, good.energy(), good.inventory(), good.farm(),
                good.today(), good.forecast(), good.randomState(), good.market(), good.ledger(), good.progress());
        mongo.put(new SaveCodec().encode(2, broken, Instant.now()));
        before = app.state().createMemento();

        assertRejected(app.controller.load(2), ErrorCode.ARCHIVO_CORRUPTO);
    }

    @Test
    void HU31_ca4_aMalformedJsonIsDetectedBeforeTheChecksumAndTheVersion() {
        String broken = "{\"date\":";
        mongo.put(new SaveDocument("slot2", 99, Instant.now(), broken, "not-a-checksum"));

                // primero se comprueba el JSON: la versión y la suma de control equivocadas nunca se alcanzan
        assertRejected(app.controller.load(2), ErrorCode.ARCHIVO_CORRUPTO);
    }

    @Test
    void HU31_ca4_aValidJsonWithAWrongVersionIsRejectedByVersionBeforeTheChecksum() {
        mongo.put(new SaveDocument("slot2", 99, Instant.now(), "{}", "not-a-checksum"));

        assertRejected(app.controller.load(2), ErrorCode.VERSION_NO_SOPORTADA);
    }

    @Test
    void HU31_ca4_everyInvariantOfRn33IsChecked() {
        GameMemento good = RichGame.play().state().createMemento();
        var buildingList = good.farm().buildings();
        var inventory = good.inventory();

        var tooMuchEnergy = new GameMemento(good.date(), good.money(), 101, inventory, good.farm(), good.today(),
                good.forecast(), good.randomState(), good.market(), good.ledger(), good.progress());
        var overlapping = new GameMemento(good.date(), good.money(), good.energy(), inventory,
                new edu.ucc.farmsim.domain.farm.FarmMapSnapshot(good.farm().crops(),
                        java.util.List.of(buildingList.get(0), buildingList.get(0))),
                good.today(), good.forecast(), good.randomState(), good.market(), good.ledger(), good.progress());
        var wrongInventory = new GameMemento(good.date(), good.money(), good.energy(),
                new edu.ucc.farmsim.domain.economy.InventorySnapshot(30, inventory.slots()), good.farm(), good.today(),
                good.forecast(), good.randomState(), good.market(), good.ledger(), good.progress());
        var fakeAchievement = new GameMemento(good.date(), good.money(), good.energy(), inventory, good.farm(),
                good.today(), good.forecast(), good.randomState(), good.market(), good.ledger(),
                new edu.ucc.farmsim.domain.progress.ProgressSnapshot(
                        java.util.Set.of(edu.ucc.farmsim.domain.progress.Achievement.BIG_HARVESTER), 0, 0, 0, 0));

        for (GameMemento bad : java.util.List.of(tooMuchEnergy, overlapping, wrongInventory, fakeAchievement)) {
            assertTrue(!edu.ucc.farmsim.domain.game.GameStateValidator.violations(bad).isEmpty(), String.valueOf(bad));
        }
        assertEquals(java.util.List.of(), edu.ucc.farmsim.domain.game.GameStateValidator.violations(good));
        assertEquals(1, edu.ucc.farmsim.domain.game.GameStateValidator.violations(null).size());
    }

    @Test
    void HU31_ca5_whenMongoDbDoesNotAnswerLoadingReportsErrorEsAndTheGameDoesNotChange() {
        mongo.goDown();

        Result<HudView> result = app.controller.load(1);

        assertRejected(result, ErrorCode.ERROR_ES);
        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", result.message());
    }

    @Test
    void HU31_theChecksumIsTheSha256OfTheStateText() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Checksum.of("abc"));
    }
}
