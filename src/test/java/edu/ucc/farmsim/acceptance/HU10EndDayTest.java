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

/** HU-10: end the day, resolving its effects in the fixed order of RN-03. */
class HU10EndDayTest {

    /** A pipeline step that only records that it ran. */
    private record RecordingStep(DayPhase phase, List<String> log) implements DayStep {
        @Override
        public void apply(GameState state, DayReport report) {
            log.add("step:" + phase.name());
        }
    }

    @Test
    void HU10_ca1_stepsRunInTheOrderOfRn03EvenIfRegisteredInAnyOrderAndEventsArePublishedLast() {
        // Given one step per phase, registered in a shuffled order
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

        // When the day ends
        Result<HudView> result = app.controller.endDay();

        // Then the steps ran as RN-03 says: weather effects, growth, animals, market, calendar, forecast,
        // energy, achievements; and the events were published after all of them (step 9)
        assertTrue(result.isOk());
        List<String> expected = new ArrayList<>();
        for (DayPhase phase : DayPhase.values()) {
            expected.add("step:" + phase.name());
        }
        expected.add("event");
        assertEquals(expected, log);
        assertEquals(List.of(DayPhase.WEATHER_EFFECTS, DayPhase.GROWTH, DayPhase.ANIMALS, DayPhase.MARKET,
                DayPhase.CALENDAR, DayPhase.FORECAST, DayPhase.ENERGY, DayPhase.ACHIEVEMENTS),
                Arrays.asList(DayPhase.values()));
    }

    @Test
    void HU10_ca1_theStandardPipelineAdvancesTheCalendarThenDrawsTheForecastThenRestoresEnergy() {
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        GameState state = app.session.current();

        DayReport report = DayPipeline.standard().run(state);

        assertEquals(List.of(DayPhase.CALENDAR, DayPhase.FORECAST, DayPhase.ENERGY), report.phasesRun());
        assertEquals(new GameDate(1, Season.SPRING, 1), report.previousDate());
        assertEquals(new GameDate(1, Season.SPRING, 2), report.newDate());
        assertEquals(state.weather().today(), report.todayWeather());
    }

    @Test
    void HU10_ca2_endingTheDayRestoresEnergyTo100() {
        // Given a game where 60 energy was spent (40 left)
        TestApp app = TestApp.create();
        app.controller.newGame(TestApp.SEED);
        assertTrue(app.session.current().energy().trySpend(60));
        assertEquals(40, app.session.current().energy().current());

        // When the day ends
        Result<HudView> result = app.controller.endDay();

        // Then energy is 100 and the view shows day 2
        assertTrue(result.isOk());
        assertEquals(100, app.session.current().energy().current());
        assertEquals(100, result.value().energy());
        assertEquals(2, result.value().day());
        // (the "watered" marks of the plots are cleared by HU-03 in phase 5.2)
    }

    @Test
    @Tag("perf")
    @Disabled("needs full-load fixture, see PLAN_DE_PRUEBAS section 9")
    void HU10_ca3_endOfDayTakesAtMost200msAtFullLoad() {
        // Given a farm at full load (40 outdoor crops, 9 in the greenhouse, 6 buildings, 16 animals)
        // When the day ends 100 times, then the 95th percentile is at most 200 ms (RNF-01). Phase 5.5.
    }

    @Test
    void HU10_ca4_anInternalFailureLeavesTheWholeStateIdenticalAndPublishesNothing() {
        // Given a step that changes things and then fails
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

        // When the day ends
        Result<HudView> result = app.controller.endDay();

        // Then the result is an error, the state is exactly as before, and no event was published
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

        assertEquals(1, app.events.size());
        assertEquals(new GameEvent.DayEnded(result.value()), app.events.get(0));
    }

    @Test
    void AR4_controllerNeverThrowsEvenWhenTheSessionFails() {
        // Given a session that blows up as soon as the state is read
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

        // Then every controller call answers with ERROR_INTERNO instead of throwing
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
