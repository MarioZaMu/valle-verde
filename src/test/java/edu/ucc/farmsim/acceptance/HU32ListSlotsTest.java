package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.infrastructure.persistence.SaveDocument;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.SlotsPresenter;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-32: listing the three slots with what they hold (RN-31). */
class HU32ListSlotsTest {

    private final InMemoryMongo mongo = new InMemoryMongo();

    private TestApp app() {
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        return app;
    }

    @Test
    void HU32_ca1_theListShowsOccupiedAndEmptySlots() {
        TestApp app = app();
        GameFixtures.setDate(1, Season.SUMMER, 5);
        GameFixtures.setMoney(app.state(), 1250);
        app.controller.save(1);
        app.controller.save(3);

        List<String> lines = new SlotsPresenter(app.controller).lines();

        assertEquals(List.of("Ranura 1: Año 1 · Verano · día 5 · 1.250 m · 2026-11-22 10:30",
                "Ranura 2: Vacía", "Ranura 3: Año 1 · Verano · día 5 · 1.250 m · 2026-11-22 10:30"), lines);
    }

    @Test
    void HU32_ca2_aDamagedSlotIsListedAsSuchAndTheRestStillWorks() {
        TestApp app = app();
        app.controller.save(1);
        app.controller.save(3);
        mongo.tamper(1, d -> new SaveDocument(d.id(), d.schemaVersion(), d.savedAt(), "{truncated", d.checksum()));

        List<SaveSlotView> slots = app.controller.saveSlots().value();

        assertEquals("DAMAGED", slots.get(0).status());
        assertEquals("EMPTY", slots.get(1).status());
        assertEquals("OK", slots.get(2).status());
        assertEquals("Ranura 1: Dañada", SlotsPresenter.line(slots.get(0)));
    }

    @Test
    void HU32_ca3_savingInAnEmptySlotShowsItsDataWithoutRestarting() {
        TestApp app = app();
        SlotsPresenter presenter = new SlotsPresenter(app.controller);
        assertEquals("Ranura 2: Vacía", presenter.lines().get(1));

        app.controller.save(2);
        presenter.refresh();

        assertEquals("Ranura 2: Año 1 · Primavera · día 1 · 500 m · 2026-11-22 10:30", presenter.lines().get(1));
    }

    @Test
    void HU32_whenTheStorageFailsTheListSaysSo() {
        TestApp app = app();
        mongo.goDown();

        var result = app.controller.saveSlots();

        assertEquals(ErrorCode.ERROR_ES, result.code());
    }
}
