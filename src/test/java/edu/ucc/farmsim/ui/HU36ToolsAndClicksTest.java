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
 * HU-36: pick a tool with the keys 1 to 5 or the bar and act on a plot by clicking it. The tool model and the
 * presenter have no Swing, so the whole behaviour is tested without a display. The market, inventory,
 * achievements and save shortcuts are bound when their screens exist (phases 5.3 and 5.5).
 */
class HU36ToolsAndClicksTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    private final List<String> messages = new ArrayList<>();
    private final ToolModel tools = new ToolModel();

    private GameScreenPresenter presenter(TestApp app) {
        return new GameScreenPresenter(app.controller, tools, messages::add);
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
        // Given the Sow tool with carrot seeds selected
        TestApp app = TestApp.startedGame();
        tools.select(Tool.SOW);
        tools.selectSeed("CARROT_SEED");

        // When the player clicks a soil plot
        presenter(app).onPlotClicked(COL, ROW);

        // Then the crop was sown (HU-01) and no error is shown
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
        // Given the Sow tool and no energy
        TestApp app = TestApp.startedGame();
        app.state().energy().set(0);
        tools.select(Tool.SOW);
        GameMemento before = app.state().createMemento();

        // When the player clicks a soil plot
        presenter(app).onPlotClicked(COL, ROW);

        // Then the state is the same and the message explains why
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
        // (collecting the products of a building with this same tool arrives with HU-22)
    }

    @Test
    void HU36_theBuildAndFeedToolsCanBeSelectedButSayTheyAreNotAvailableYet() {
        TestApp app = TestApp.startedGame();
        GameMemento before = app.state().createMemento();

        tools.select(Tool.BUILD);
        presenter(app).onPlotClicked(COL, ROW);
        tools.select(Tool.FEED);
        presenter(app).onPlotClicked(COL, ROW);

        assertEquals(List.of(Texts.TOOL_NOT_AVAILABLE_YET, Texts.TOOL_NOT_AVAILABLE_YET), messages);
        assertEquals(before, app.state().createMemento());
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
    void HU36_ca5_enterEndsTheDayAndTheShortcutsOfLaterPhasesExplainThemselves() {
        TestApp app = TestApp.startedGame();

        presenter(app).onShortcut(Shortcut.END_DAY);
        assertEquals(2, app.controller.hud().value().day());
        assertTrue(messages.stream().allMatch(String::isBlank));

        messages.clear();
        presenter(app).onShortcut(Shortcut.MARKET);
        assertEquals(List.of(Texts.SHORTCUT_NOT_AVAILABLE_YET), messages);
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
}
