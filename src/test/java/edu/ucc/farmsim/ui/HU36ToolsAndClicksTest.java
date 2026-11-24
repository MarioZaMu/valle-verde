package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.Shortcut;
import edu.ucc.farmsim.ui.components.Tool;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.screens.GameScreenPresenter;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * HU-36: elegir una herramienta con las teclas 1 a 5 o con la barra y actuar sobre una parcela haciendo clic en ella.
 * El modelo de herramientas y el presentador no tienen Swing, así que todo el comportamiento se prueba sin pantalla.
 * Los atajos de mercado, inventario, logros y guardado se enlazan cuando existen sus pantallas (fases 5.3 y 5.5).
 */
class HU36ToolsAndClicksTest {

    private static final edu.ucc.farmsim.ui.screens.GameDialogs NO_DIALOGS = new edu.ucc.farmsim.support.RecordingDialogs();

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    private final List<String> messages = new ArrayList<>();
    private final ToolModel tools = new ToolModel();

    private GameScreenPresenter presenter(TestApp app) {
        return new GameScreenPresenter(app.controller, tools, messages::add, NO_DIALOGS);
    }

    @Test
    void HU36_ca1_keysOneToFiveSelectTheFiveToolsAndEscapeDeselects() {
        assertNull(tools.current());

        assertTrue(tools.handleKey('1'));
        assertEquals(Tool.SOW, tools.current());
        tools.handleKey('2');
        assertEquals(Tool.WATER, tools.current());
        tools.handleKey('3');
        assertEquals(Tool.HARVEST, tools.current());
        tools.handleKey('4');
        assertEquals(Tool.BUILD, tools.current());
        tools.handleKey('5');
        assertEquals(Tool.FEED, tools.current());

        tools.deselect();
        assertNull(tools.current());
        assertFalse(tools.handleKey('9'), "other keys are not tool keys");
    }

    @Test
    void HU36_ca1_theBarAndTheKeysShareTheSameSelectionAndNotifyListeners() {
        List<Tool> seen = new ArrayList<>();
        tools.addChangeListener(() -> seen.add(tools.current()));

        tools.select(Tool.WATER);
        tools.handleKey('1');
        tools.deselect();

        assertEquals(java.util.Arrays.asList(Tool.WATER, Tool.SOW, null), seen);
    }

    @Test
    void HU36_ca1_everyToolHasASpanishLabelAndItsKey() {
        assertEquals("Sembrar", Texts.tool(Tool.SOW));
        assertEquals("Regar", Texts.tool(Tool.WATER));
        assertEquals("Cosechar", Texts.tool(Tool.HARVEST));
        assertEquals("Construir", Texts.tool(Tool.BUILD));
        assertEquals("Alimentar", Texts.tool(Tool.FEED));
        assertEquals('1', Tool.SOW.key());
        assertEquals(KeyEvent.VK_5, Tool.FEED.keyCode());
    }

    @Test
    void HU36_ca2_clickingAPlotWithTheSowToolAndASeedSowsIt() {
                // Dada la herramienta Sembrar con semillas de zanahoria seleccionadas
        TestApp app = TestApp.startedGame();
        tools.select(Tool.SOW);
        tools.selectSeed("CARROT_SEED");

                // Cuando el jugador hace clic en una parcela de tierra
        presenter(app).onPlotClicked(COL, ROW);

                // Entonces el cultivo se sembró (HU-01) y no se muestra ningún error
        assertNotNull(app.state().farm().plotAt(COL, ROW).crop());
        assertEquals(9, app.state().inventory().count(ItemType.CARROT_SEED));
        assertTrue(messages.isEmpty() || messages.stream().allMatch(String::isEmpty), messages.toString());
    }

    @Test
    void HU36_ca2_theWaterToolWatersTheClickedCrop() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 0);
        tools.select(Tool.WATER);

        presenter(app).onPlotClicked(COL, ROW);

        assertTrue(app.state().farm().plotAt(COL, ROW).crop().isWatered());
    }

    @Test
    void HU36_ca3_aRejectedActionShowsTheSpanishMessageAndChangesNothing() {
                // Dada la herramienta Sembrar y sin energía
        TestApp app = TestApp.startedGame();
        app.state().energy().set(0);
        tools.select(Tool.SOW);
        GameMemento before = app.state().createMemento();

                // Cuando el jugador hace clic en una parcela de tierra
        presenter(app).onPlotClicked(COL, ROW);

                // Entonces el estado es el mismo y el mensaje explica por qué
        assertEquals(before, app.state().createMemento());
        assertEquals(List.of("Energía insuficiente: necesitas 2 y tienes 0."), messages);
    }

    @Test
    void HU36_ca3_withoutAToolAClickDoesNothingAtAll() {
        TestApp app = TestApp.startedGame();
        GameMemento before = app.state().createMemento();

        presenter(app).onPlotClicked(COL, ROW);

        assertEquals(before, app.state().createMemento());
        assertTrue(messages.isEmpty());
    }

    @Test
    void HU36_ca4_theHarvestToolHarvestsAMatureCropAndClearsAWitheredOne() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 4);
        GameFixtures.plant(app.state(), COL + 1, ROW, CropType.CARROT, 2, 3, false, true);
        tools.select(Tool.HARVEST);

        presenter(app).onPlotClicked(COL, ROW);
        presenter(app).onPlotClicked(COL + 1, ROW);

        assertEquals(1, app.state().inventory().count(ItemType.CARROT));
        assertNull(app.state().farm().plotAt(COL, ROW).crop());
        assertNull(app.state().farm().plotAt(COL + 1, ROW).crop());
        assertEquals(100 - 2 - 1, app.state().energy().current());
                // (recoger los productos de un edificio con esta misma herramienta llega con HU-22)
    }

    @Test
    void HU36_ca5_everyShortcutIsMappedToItsKey() {
        assertEquals(KeyEvent.VK_ENTER, Shortcut.END_DAY.keyCode());
        assertEquals(KeyEvent.VK_M, Shortcut.MARKET.keyCode());
        assertEquals(KeyEvent.VK_I, Shortcut.INVENTORY.keyCode());
        assertEquals(KeyEvent.VK_L, Shortcut.ACHIEVEMENTS.keyCode());
        assertEquals(KeyEvent.VK_F5, Shortcut.QUICK_SAVE.keyCode());
        assertEquals(KeyEvent.VK_F9, Shortcut.QUICK_LOAD.keyCode());
        assertEquals(6, Shortcut.values().length);
    }

    @Test
    void HU36_ca5_enterEndsTheDayAndLOpensTheAchievements() {
        TestApp app = TestApp.startedGame();
        edu.ucc.farmsim.support.RecordingDialogs dialogs = new edu.ucc.farmsim.support.RecordingDialogs();
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, tools, messages::add, dialogs);

        presenter.onShortcut(Shortcut.END_DAY);
        presenter.onShortcut(Shortcut.ACHIEVEMENTS);

        assertEquals(2, app.controller.hud().value().day());
        assertTrue(messages.stream().allMatch(String::isBlank));
        assertEquals(List.of("achievements"), dialogs.opened);
    }

    @Test
    void HU29_ca1_theSaveButtonAlwaysOpensTheSlotsToChooseWhereToSave() {
        TestApp app = TestApp.startedGame();
        edu.ucc.farmsim.support.RecordingDialogs dialogs = new edu.ucc.farmsim.support.RecordingDialogs();
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, tools, messages::add, dialogs);

        presenter.onSave();
        presenter.useSlot(2);
        presenter.onSave();

        assertEquals(List.of("slots:save", "slots:save"), dialogs.opened, "even after F5 used a slot");
        assertTrue(messages.isEmpty(), "nothing is saved until a slot is chosen");
    }

    @Test
    void HU36_ca5_f5AndF9OpenTheSlotsTheFirstTimeAndThenUseTheSlotUsedLast() {
        TestApp app = TestApp.startedGame();
        edu.ucc.farmsim.support.RecordingDialogs dialogs = new edu.ucc.farmsim.support.RecordingDialogs();
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, tools, messages::add, dialogs);

        presenter.onShortcut(Shortcut.QUICK_SAVE);
        presenter.onShortcut(Shortcut.QUICK_LOAD);
        assertEquals(List.of("slots:save", "slots:load"), dialogs.opened);

        presenter.useSlot(2);
        presenter.onShortcut(Shortcut.QUICK_SAVE);
        app.controller.endDay();
        presenter.onShortcut(Shortcut.QUICK_LOAD);

        assertEquals(List.of("Partida guardada en la ranura 2.", "Partida cargada de la ranura 2."), messages);
        assertEquals(1, app.controller.hud().value().day());
        assertEquals(2, presenter.lastSlot());
    }

    @Test
    void HU36_quickSaveAndLoadShowTheRn35MessageWhenTheStorageFails() {
        edu.ucc.farmsim.support.FakeSaveRepository repository = new edu.ucc.farmsim.support.FakeSaveRepository();
        TestApp app = TestApp.create(new edu.ucc.farmsim.application.service.GameSession(),
                edu.ucc.farmsim.domain.game.DayPipeline.standard(), repository);
        app.controller.newGame(TestApp.SEED);
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, tools, messages::add,
                new edu.ucc.farmsim.support.RecordingDialogs());
        presenter.useSlot(1);
        repository.unreachable();

        presenter.onShortcut(Shortcut.QUICK_SAVE);
        presenter.onShortcut(Shortcut.QUICK_LOAD);

        assertEquals(List.of("No se pudo acceder a las partidas guardadas: MongoDB no respondió.",
                "No se pudo acceder a las partidas guardadas: MongoDB no respondió."), messages);
    }

    @Test
    void HU36_theSelectedSeedDefaultsToCarrotAndOnlySeedsAreAccepted() {
        assertEquals("CARROT_SEED", tools.selectedSeed());
        tools.selectSeed("PUMPKIN_SEED");
        assertEquals("PUMPKIN_SEED", tools.selectedSeed());
        try {
            tools.selectSeed("EGG");
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("A non-seed item must be rejected");
    }

    @Test
    void HU36_ca4_theHarvestToolCollectsTheProductsOfABuilding() {
        TestApp app = TestApp.startedGame();
        var coop = GameFixtures.coop(app.state());
        GameFixtures.house(coop, edu.ucc.farmsim.domain.animal.AnimalType.HEN,
                edu.ucc.farmsim.domain.animal.AnimalCondition.HEALTHY, 0, false, 2);
        tools.select(Tool.HARVEST);

        presenter(app).onPlotClicked(GameFixtures.GRASS_COL + 1, GameFixtures.GRASS_ROW + 1);

        assertEquals(2, app.state().inventory().count(ItemType.EGG));
        assertEquals(99, app.state().energy().current());
        assertEquals(List.of(""), messages);
    }

    @Test
    void HU36_ca4_collectingFromABuildingWithNothingWaitingShowsTheNotice() {
        TestApp app = TestApp.startedGame();
        GameFixtures.coop(app.state());
        tools.select(Tool.HARVEST);

        presenter(app).onPlotClicked(GameFixtures.GRASS_COL, GameFixtures.GRASS_ROW);

        assertEquals(List.of("No hay productos que recoger."), messages);
    }

    @Test
    void HU36_theFeedToolFeedsTheBuildingUnderTheClickAndTellsWhenThereIsNone() {
        TestApp app = TestApp.startedGame();
        var coop = GameFixtures.coop(app.state());
        GameFixtures.hen(coop);
        app.state().inventory().add(ItemType.HAY, 1);
        tools.select(Tool.FEED);

        presenter(app).onPlotClicked(COL, ROW);
        presenter(app).onPlotClicked(GameFixtures.GRASS_COL, GameFixtures.GRASS_ROW);

        assertEquals(List.of(Texts.NO_BUILDING_HERE, ""), messages);
        assertTrue(coop.animals().get(0).isFedToday());
        assertEquals(0, app.state().inventory().count(ItemType.HAY));
    }

    @Test
    void HU36_theFeedToolShowsTheRejectionOfTheController() {
        TestApp app = TestApp.startedGame();
        GameFixtures.hen(GameFixtures.coop(app.state()));
        tools.select(Tool.FEED);

        presenter(app).onPlotClicked(GameFixtures.GRASS_COL, GameFixtures.GRASS_ROW);

        assertEquals(List.of("Heno insuficiente: necesitas 1."), messages);
    }

    @Test
    void HU36_theBuildToolOpensTheConstructionDialogOnTheClickedPlot() {
        TestApp app = TestApp.startedGame();
        edu.ucc.farmsim.support.RecordingDialogs dialogs = new edu.ucc.farmsim.support.RecordingDialogs();
        tools.select(Tool.BUILD);

        new GameScreenPresenter(app.controller, tools, messages::add, dialogs).onPlotClicked(2, 7);

        assertEquals(List.of("construction:2,7"), dialogs.opened);
        assertTrue(app.state().farm().buildings().isEmpty());
    }

    @Test
    void HU36_withoutAToolClickingABuildingOpensItsPanelAndClickingAPlotDoesNothing() {
        TestApp app = TestApp.startedGame();
        GameFixtures.coop(app.state());
        edu.ucc.farmsim.support.RecordingDialogs dialogs = new edu.ucc.farmsim.support.RecordingDialogs();
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, tools, messages::add, dialogs);

        presenter.onPlotClicked(COL, ROW);
        presenter.onPlotClicked(GameFixtures.GRASS_COL + 1, GameFixtures.GRASS_ROW);

        assertEquals(List.of("building:0"), dialogs.opened);
        assertTrue(messages.isEmpty());
    }
}
