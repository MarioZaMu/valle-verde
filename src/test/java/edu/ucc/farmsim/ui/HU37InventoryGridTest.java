package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.ui.components.InventoryGrid;
import edu.ucc.farmsim.ui.components.InventoryModel;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.screens.GameDialogs;
import edu.ucc.farmsim.ui.screens.GameScreenPresenter;
import edu.ucc.farmsim.ui.components.Shortcut;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.RecordingDialogs;
import edu.ucc.farmsim.support.TestApp;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.Test;

/** HU-37: el inventario como una grilla de casillas, con la elección de semilla (RN-15, RN-34). */
class HU37InventoryGridTest {

    private static InventoryModel modelOf(TestApp app, ToolModel tools) {
        InventoryModel model = new InventoryModel(app.controller, tools);
        app.bus.subscribe(model);
        model.refresh();
        return model;
    }

    @Test
    void HU37_ca1_aNewGameShows24SlotsWithCarrotAndPotatoSeedsAndTheRestEmpty() {
        TestApp app = TestApp.startedGame();

        InventoryModel model = modelOf(app, new ToolModel());

        assertEquals(24, model.slotCount());
        assertEquals("Semilla de Zanahoria", model.cell(0).name());
        assertEquals(10, model.cell(0).amount());
        assertEquals(5, model.cell(1).amount());
        for (int i = 2; i < 24; i++) {
            assertTrue(model.cell(i).isEmpty(), "slot " + i);
        }
    }

    @Test
    void HU37_ca1_theGridDrawsOneButtonPerSlot() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.startedGame();

        InventoryGrid grid = new InventoryGrid(modelOf(app, new ToolModel()));

        assertEquals(24, grid.getComponentCount());
        assertTrue(((JToggleButton) grid.getComponent(0)).getText().contains("10"));
    }

    @Test
    void HU37_ca2_aWarehouseGivesAGridOf36Slots() {
        TestApp app = TestApp.startedGame();
                // (la vista del inventario informa la capacidad que tenga la partida)
        var inventory = new edu.ucc.farmsim.domain.economy.Inventory(36);
        inventory.add(ItemType.CARROT_SEED, 1);
        app.state().inventory().restoreFrom(inventory.snapshot());

        InventoryModel model = modelOf(app, new ToolModel());

        assertEquals(36, model.slotCount());
    }

    @Test
    void HU37_ca3_clickingASeedChoosesItForTheSowToolAndAnEmptyAmountClearsTheChoice() {
                // Dada la semilla de zanahoria predeterminada elegida
        TestApp app = TestApp.startedGame();
        ToolModel tools = new ToolModel();
        InventoryModel model = modelOf(app, tools);
        assertTrue(model.isSelected(0));

                // Cuando el jugador hace clic en la casilla de la papa
        model.select(1);

                // Entonces la papa queda elegida y marcada
        assertEquals("POTATO_SEED", tools.selectedSeed());
        assertTrue(model.isSelected(1));
        assertFalse(model.isSelected(0));

                // Y cuando se siembra la última semilla de papa, la elección se borra
        app.state().inventory().remove(ItemType.POTATO_SEED, 5);
        app.bus.publish(new edu.ucc.farmsim.application.event.GameEvent.InventoryChanged());
        assertNull(tools.selectedSeed());
    }

    @Test
    void HU37_ca3_sowingTheLastSeedThroughTheGameClearsTheChoiceViaEvents() {
        TestApp app = TestApp.startedGame();
        ToolModel tools = new ToolModel();
        modelOf(app, tools);
        app.state().inventory().remove(ItemType.CARROT_SEED, 9);

        Result<?> sown = app.controller.plant(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW, "CARROT_SEED");

        assertTrue(sown.isOk());
        assertNull(tools.selectedSeed());
    }

    @Test
    void HU37_ca3_sowingWithoutAChosenSeedAsksForOne() {
        TestApp app = TestApp.startedGame();
        ToolModel tools = new ToolModel();
        tools.clearSeed();
        tools.select(edu.ucc.farmsim.ui.components.Tool.SOW);
        List<String> messages = new ArrayList<>();
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, tools, messages::add, noDialogs());

        presenter.onPlotClicked(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW);

        assertEquals(List.of(edu.ucc.farmsim.ui.theme.Texts.NO_SEED_SELECTED), messages);
        assertEquals(10, app.state().inventory().count(ItemType.CARROT_SEED));
    }

    @Test
    void HU37_ca4_aStackOf99ShowsNinetyNineAndTheNextUnitUsesAnotherSlot() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 100);

        InventoryModel model = modelOf(app, new ToolModel());

        assertEquals(99, model.cell(2).amount());
        assertEquals(1, model.cell(3).amount());
        assertEquals(model.cell(2).itemId(), model.cell(3).itemId());
    }

    @Test
    void HU37_theInventoryShortcutOpensTheGridAndClickingAHarvestDoesNotChooseASeed() {
        TestApp app = TestApp.startedGame();
        ToolModel tools = new ToolModel();
        RecordingDialogs dialogs = new RecordingDialogs();
        List<String> opened = dialogs.opened;
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, tools, m -> { }, dialogs);
        app.state().inventory().add(ItemType.CARROT, 1);
        InventoryModel model = modelOf(app, tools);

        presenter.onShortcut(Shortcut.INVENTORY);
        presenter.onShortcut(Shortcut.MARKET);
        model.select(2);

        assertEquals(List.of("inventory", "market"), opened);
        assertEquals("CARROT_SEED", tools.selectedSeed());
    }

    private static GameDialogs noDialogs() {
        return new RecordingDialogs();
    }

    @Test
    void HU37_ca2_buildingTheWarehouseWidensTheGridToThirtySixSlotsThroughTheEvent() {
        TestApp app = TestApp.startedGame();
        edu.ucc.farmsim.support.GameFixtures.unlock(app.state(), edu.ucc.farmsim.domain.progress.Achievement.FIRST_HARVEST);
        InventoryModel model = modelOf(app, new ToolModel());
        assertEquals(24, model.slotCount());

        app.controller.build("WAREHOUSE", 2, 7);

        assertEquals(36, model.slotCount());
    }
}
