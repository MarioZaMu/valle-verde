package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.support.FakeSaveRepository;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.InMemoryMongo;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.AchievementsDialog;
import edu.ucc.farmsim.ui.components.AchievementsPresenter;
import edu.ucc.farmsim.ui.components.SlotsDialog;
import edu.ucc.farmsim.ui.components.SlotsPresenter;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;

/** Las pantallas de ranuras de guardado y de logros (HU-26, HU-29, HU-30, HU-32). */
class SlotsAndAchievementsUiTest {

    private static TestApp app(InMemoryMongo mongo) {
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), mongo.repository());
        app.controller.newGame(TestApp.SEED);
        return app;
    }

    @Test
    void HU29_ca3_savingInAnOccupiedSlotAsksFirstAndCancellingKeepsTheOldSave() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = app(mongo);
        SlotsPresenter presenter = new SlotsPresenter(app.controller);
        assertFalse(presenter.needsConfirmation(2));
        assertEquals("Partida guardada en la ranura 2.", presenter.save(2, () -> {
            throw new AssertionError("an empty slot must not ask");
        }));
        String first = mongo.document(2).state();
        app.controller.endDay();
        List<String> asked = new ArrayList<>();

        String cancelled = presenter.save(2, () -> {
            asked.add("asked");
            return false;
        });
        assertEquals("", cancelled);
        assertEquals(first, mongo.document(2).state());

        String accepted = presenter.save(2, () -> {
            asked.add("asked");
            return true;
        });

        assertEquals(List.of("asked", "asked"), asked);
        assertEquals("Partida guardada en la ranura 2.", accepted);
        assertTrue(!first.equals(mongo.document(2).state()));
    }

    @Test
    void HU30_loadingFromThePresenterReplacesTheGameAndRejectsWhatCannotBeLoaded() {
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = app(mongo);
        SlotsPresenter presenter = new SlotsPresenter(app.controller);
        presenter.save(1, () -> true);
        app.controller.endDay();

        assertTrue(presenter.load(1).isOk());
        assertEquals(1, app.controller.hud().value().day());
        assertFalse(presenter.load(3).isOk());
        assertEquals("No existe una partida en la ranura 3.", presenter.load(3).message());
        assertTrue(presenter.canLoad(1));
        assertFalse(presenter.canLoad(3));
    }

    @Test
    void HU32_whenTheStorageFailsThePresenterKeepsTheMessage() {
        FakeSaveRepository repository = new FakeSaveRepository().unreachable();
        TestApp app = TestApp.create(new GameSession(), DayPipeline.standard(), repository);
        app.controller.newGame(1L);

        SlotsPresenter presenter = new SlotsPresenter(app.controller);

        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", presenter.error());
        assertTrue(presenter.lines().isEmpty());
    }

    @Test
    void HU26_theAchievementsPresenterShowsTheStateWithoutRelyingOnColour() {
        TestApp app = TestApp.startedGame();
        GameFixtures.unlock(app.state(), Achievement.FIRST_HARVEST);
        AchievementsPresenter presenter = new AchievementsPresenter(app.controller);

        assertEquals(9, presenter.achievements().size());
        assertEquals("Desbloqueado", presenter.state(presenter.achievements().get(0)));
        assertEquals("1/25", presenter.state(presenter.achievements().get(1)));
        Object[] row = presenter.row(presenter.achievements().get(4));
        assertEquals("Capitalista", row[0]);
        assertEquals("Ten un saldo de 3.000 m o más.", row[1]);
        assertEquals("200 m", row[2]);
        assertEquals("500/3000", row[3]);
        assertEquals("¡Logro desbloqueado: Primera cosecha! +50 m", Texts.achievementUnlocked("Primera cosecha", 50));
    }

    @Test
    void HU26_theDialogListsTheNineAchievements() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.startedGame();

        AchievementsDialog dialog = new AchievementsDialog(null, new AchievementsPresenter(app.controller));

        assertTrue(dialog.isModal());
        assertEquals(9, find(dialog, JTable.class).getRowCount());
        dialog.dispose();
    }

    @Test
    void HU29_theDialogSavesAndLoadsWithItsButtonsAndTellsWhichSlotWasUsed() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        InMemoryMongo mongo = new InMemoryMongo();
        TestApp app = app(mongo);
        List<Integer> used = new ArrayList<>();
        List<String> loaded = new ArrayList<>();
        SlotsDialog dialog = new SlotsDialog(null, new SlotsPresenter(app.controller), used::add,
                () -> loaded.add("loaded"));
        @SuppressWarnings("unchecked")
        JList<String> list = find(dialog, JList.class);
        JButton save = named(dialog, "save-button");
        JButton load = named(dialog, "load-button");
        assertFalse(save.isEnabled());
        assertEquals(3, list.getModel().getSize());

        list.setSelectedIndex(1);
        assertTrue(save.isEnabled());
        assertFalse(load.isEnabled());
        save.doClick();
        assertEquals(List.of(2), used);
        assertTrue(load.isEnabled());
        app.controller.endDay();
        load.doClick();

        assertEquals(List.of(2, 2), used);
        assertEquals(List.of("loaded"), loaded);
        assertEquals(1, app.controller.hud().value().day());
        assertFalse(dialog.isDisplayable());
    }

    private static <T extends Component> T find(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found.get(0);
    }

    private static JButton named(Container root, String name) {
        List<JButton> found = new ArrayList<>();
        collect(root, JButton.class, found);
        return found.stream().filter(b -> name.equals(b.getName())).findFirst().orElseThrow();
    }

    private static <T extends Component> void collect(Container container, Class<T> type, List<T> found) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container inner) {
                collect(inner, type, found);
            }
        }
    }
}
