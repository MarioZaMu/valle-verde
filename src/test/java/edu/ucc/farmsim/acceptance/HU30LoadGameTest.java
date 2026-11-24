package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.RichGame;
import edu.ucc.farmsim.support.TestApp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-30: cargar una partida guardada y recuperarla exactamente (RN-32, RN-33). */
class HU30LoadGameTest {

    @Test
    void HU30_ca1_theLoadedStateIsStructurallyIdenticalToTheSavedOne_criticalTest() {
                // Dada una partida rica guardada en la ranura 1
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = RichGame.play();
        TestApp store = TestApp.create(app.session, DayPipeline.standard(), mongo.repository());
        GameMemento saved = app.state().createMemento();
        assertTrue(store.controller.save(1).isOk());

                // Cuando hay otra partida en curso y se carga la ranura
        store.controller.newGame(99L);
        Result<HudView> result = store.controller.load(1);

                // Entonces calendario, energía, dinero, inventario, cultivos, edificios, animales, mercado, clima, logros y
        // contadores, y el generador, son idénticos
        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(saved, store.state().createMemento());
    }

    @Test
    void HU30_ca2_theLoadedGameDrawsTheSameWeatherAsTheOriginal() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp original = RichGame.play();
        TestApp store = TestApp.create(original.session, DayPipeline.standard(), mongo.repository());
        store.controller.save(2);
        GameMemento saved = original.state().createMemento();

        List<String> originalWeather = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            originalWeather.add(original.controller.endDay().value().weather());
        }
        TestApp loaded = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        assertTrue(loaded.controller.load(2).isOk());
        assertEquals(saved, loaded.state().createMemento());
        List<String> loadedWeather = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            loadedWeather.add(loaded.controller.endDay().value().weather());
        }

        assertEquals(originalWeather, loadedWeather);
    }

    @Test
    void HU30_ca3_loadingReplacesTheGameInProgressAndTheHudShowsTheLoadedOne() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(1L);
        app.controller.endDay();
        app.controller.endDay();
        app.controller.save(1);
        app.controller.newGame(2L);
        app.events.clear();

        Result<HudView> result = app.controller.load(1);

        assertEquals(3, result.value().day());
        assertEquals(3, app.controller.hud().value().day());
        assertTrue(app.events.contains(new GameEvent.GameStarted(result.value())));
        assertTrue(app.events.contains(new GameEvent.MapChanged()));
        assertTrue(app.events.contains(new GameEvent.InventoryChanged()));
    }

    @Test
    void HU30_loadingWorksWithoutAGameInProgress() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp first = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        first.controller.newGame(1L);
        first.controller.save(3);
        TestApp second = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());

        assertTrue(second.controller.load(3).isOk());
        assertTrue(second.controller.hasGameInProgress());
    }
}
