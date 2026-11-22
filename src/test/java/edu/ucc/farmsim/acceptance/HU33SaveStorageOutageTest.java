package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.support.FakeSaveRepository;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.application.service.GameSession;
import org.junit.jupiter.api.Test;

/** RN-31 and RNF-11: when the saved games cannot be reached the player is told, and playing is unaffected. */
class HU33SaveStorageOutageTest {

    private static TestApp appWithStorageDown() {
        return TestApp.create(new GameSession(), DayPipeline.standard(), new FakeSaveRepository().unreachable());
    }

    @Test
    void HU33_ca3_askingForSavedGamesWhileMongoDbIsDownIsErrorEsWithTheRn35Message() {
        TestApp app = appWithStorageDown();

        Result<Boolean> result = app.controller.hasSavedGames();

        assertFalse(result.isOk());
        assertEquals(ErrorCode.ERROR_ES, result.code());
        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", result.message());
    }

    @Test
    void HU33_ca3_aStorageOutageNeverStopsTheGame() {
        TestApp app = appWithStorageDown();

        Result<HudView> started = app.controller.newGame(TestApp.SEED);
        app.controller.hasSavedGames();
        Result<HudView> next = app.controller.endDay();

        assertTrue(started.isOk());
        assertTrue(next.isOk());
        assertEquals(2, next.value().day());
    }

    @Test
    void HU33_ca3_aWorkingStorageStillAnswersTrueOrFalse() {
        assertEquals(false, TestApp.create().controller.hasSavedGames().value());
        TestApp withSave = TestApp.create(new GameSession(), DayPipeline.standard(), new FakeSaveRepository().withSlot(1));
        assertEquals(true, withSave.controller.hasSavedGames().value());
    }
}
