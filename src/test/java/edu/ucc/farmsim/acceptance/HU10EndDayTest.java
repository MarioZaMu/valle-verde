package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.game.DayPhase;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.DayReport;
import edu.ucc.farmsim.domain.game.DayStep;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.support.TestApp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-10: terminar el día, resolviendo sus efectos en el orden fijo de RN-03. */
class HU10EndDayTest {

    /** Un paso del pipeline que solo registra que corrió. */
    private record RecordingStep(DayPhase phase, List<String> log) implements DayStep {
        @Override
        public void apply(GameState state, DayReport report) {
            log.add("step:" + phase.name());
        }
    }

    @Test
    void HU10_ca1_stepsRunInTheOrderOfRn03EvenIfRegisteredInAnyOrderAndEventsArePublishedLast() {
                // Dado un paso por fase, registrados en orden mezclado
        List<String> log = new ArrayList<>();
        List<DayPhase> shuffled = new ArrayList<>(Arrays.asList(DayPhase.values()));
        Collections.shuffle(shuffled, new Random(7));
        DayPipeline pipeline = new DayPipeline();
        for (DayPhase phase : shuffled) {
            pipeline.add(new RecordingStep(phase, log));
        }
        TestApp app = TestApp.create(pipeline);
        app.controller.newGame(TestApp.SEED);
        app.bus.subscribe(event -> log.add("event"));

                // Cuando termina el día
        Result<HudView> result = app.controller.endDay();

                // Entonces los pasos corrieron como dice RN-03: efectos del clima, crecimiento, animales, mercado, calendario,
        // pronóstico, energía, logros; y los eventos se publicaron después de todos ellos (paso 9)
        assertTrue(result.isOk());
        List<String> expected = new ArrayList<>();
        for (DayPhase phase : DayPhase.values()) {
            expected.add("step:" + phase.name());
        }
        expected.add("event"); // DayEnded
        expected.add("event"); // DaySummaryReady
        assertEquals(expected, log);
        assertEquals(List.of(DayPhase.WEATHER_EFFECTS, DayPhase.GROWTH, DayPhase.ANIMALS, DayPhase.MARKET,
                DayPhase.CALENDAR, DayPhase.FORECAST, DayPhase.ENERGY, DayPhase.ACHIEVEMENTS),
                Arrays.asList(DayPhase.values()));
    }

    @Test
    void HU10_ca1_theStandardPipelineRunsEveryRegisteredStepInRn03Order() {
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        GameState state = app.session.current();

        DayReport report = DayPipeline.standard().run(state);

        List<DayPhase> ran = report.phasesRun();
        assertTrue(ran.containsAll(List.of(DayPhase.CALENDAR, DayPhase.FORECAST, DayPhase.ENERGY)), ran.toString());
        List<DayPhase> sorted = new ArrayList<>(ran);
        Collections.sort(sorted);
        assertEquals(sorted, ran, "the standard pipeline must run its steps in the order of RN-03");
        assertEquals(new GameDate(1, Season.SPRING, 1), report.previousDate());
        assertEquals(new GameDate(1, Season.SPRING, 2), report.newDate());
        assertEquals(state.weather().today(), report.todayWeather());
    }

    @Test
    void HU10_ca2_endingTheDayRestoresEnergyTo100() {
                // Dada una partida donde se gastaron 60 de energía (quedan 40)
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        assertTrue(app.session.current().energy().trySpend(60));
        assertEquals(40, app.session.current().energy().current());

                // Cuando termina el día
        Result<HudView> result = app.controller.endDay();

                // Entonces la energía es 100 y la vista muestra el día 2
        assertTrue(result.isOk());
        assertEquals(100, app.session.current().energy().current());
        assertEquals(100, result.value().energy());
        assertEquals(2, result.value().day());
                // (las marcas de «regado» de las parcelas las borra HU-03 en la fase 5.2)
    }

    @Test
    @Tag("perf")
    @Disabled("needs full-load fixture, see PLAN_DE_PRUEBAS section 9")
    void HU10_ca3_endOfDayTakesAtMost200msAtFullLoad() {
                // Dada una granja a plena carga (40 cultivos al aire libre, 9 en el invernadero, 6 edificios, 16 animales)
        // Cuando el día termina 100 veces, entonces el percentil 95 es como máximo 200 ms (RNF-01). Fase 5.5.
    }

    @Test
    void HU10_ca4_anInternalFailureLeavesTheWholeStateIdenticalAndPublishesNothing() {
                // Dado un paso que cambia cosas y luego falla
        DayStep failing = new DayStep() {
            @Override
            public DayPhase phase() {
                return DayPhase.FORECAST;
            }

            @Override
            public void apply(GameState state, DayReport report) {
                state.energy().trySpend(30);
                GameClock.getInstance().advanceDay();
                throw new IllegalStateException("boom");
            }
        };
        TestApp app = TestApp.create(DayPipeline.standard().add(failing));
        app.controller.newGame(TestApp.SEED);
        GameMemento before = app.session.current().createMemento();
        app.events.clear();

                // Cuando termina el día
        Result<HudView> result = app.controller.endDay();

                // Entonces el resultado es un error, el estado es exactamente el de antes y no se publicó ningún evento
        assertFalse(result.isOk());
        assertEquals(ErrorCode.ERROR_INTERNO, result.code());
        assertEquals("Ocurrió un error inesperado. Tu partida no cambió.", result.message());
        assertEquals(before, app.session.current().createMemento());
        assertEquals(100, app.session.current().energy().current());
        assertEquals(new GameDate(1, Season.SPRING, 1), GameClock.getInstance().date());
        assertTrue(app.events.isEmpty());
    }

    @Test
    void HU10_theMementoRestoresEveryPartOfTheState() {
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        GameState state = app.session.current();
        app.controller.endDay();
        state.energy().trySpend(25);
        state.inventory().add(edu.ucc.farmsim.domain.economy.ItemType.EGG, 7);
        GameMemento saved = state.createMemento();

        app.controller.endDay();
        app.controller.endDay();
        state.inventory().remove(edu.ucc.farmsim.domain.economy.ItemType.CARROT_SEED, 3);
        assertFalse(saved.equals(state.createMemento()));

        state.restore(saved);

        assertEquals(saved, state.createMemento());
        assertEquals(new GameDate(1, Season.SPRING, 2), GameClock.getInstance().date());
    }

    @Test
    void HU10_endingTheDayWithoutAGameIsRejectedWithSinPartida() {
        TestApp app = TestApp.create();

        Result<HudView> result = app.controller.endDay();

        assertFalse(result.isOk());
        assertEquals(ErrorCode.SIN_PARTIDA, result.code());
    }

    @Test
    void HU10_aSuccessfulEndOfDayPublishesTheDayEndedEventWithTheNewHud() {
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        app.events.clear();

        Result<HudView> result = app.controller.endDay();

        assertEquals(2, app.events.size());
        assertEquals(new GameEvent.DayEnded(result.value()), app.events.get(0));
        assertTrue(app.events.get(1) instanceof GameEvent.DaySummaryReady);
    }

    @Test
    void AR4_controllerNeverThrowsEvenWhenTheSessionFails() {
                // Dada una sesión que explota apenas se lee el estado
        GameSession faulty = new GameSession() {
            @Override
            public boolean hasGame() {
                return true;
            }

            @Override
            public GameState current() {
                throw new IllegalStateException("disk on fire");
            }
        };
        TestApp app = TestApp.create(faulty, DayPipeline.standard());

                // Entonces toda llamada del controlador responde con ERROR_INTERNO en lugar de lanzar una excepción
        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.endDay().code());
        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.hud().code());
    }

    @Test
    void HU10_aPipelineRejectsMissingSteps() {
        try {
            new DayPipeline().add(null);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("A null step must be rejected");
    }
}
