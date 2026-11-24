package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.components.HudPanel;
import edu.ucc.farmsim.ui.components.InventoryGrid;
import edu.ucc.farmsim.ui.components.InventoryModel;
import edu.ucc.farmsim.ui.components.Tool;
import edu.ucc.farmsim.ui.components.ToolBar;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.screens.GameScreen;
import edu.ucc.farmsim.ui.screens.ScreenNavigator;
import edu.ucc.farmsim.ui.theme.Texts;
import edu.ucc.farmsim.ui.theme.UiTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/**
 * HU-48: íconos en la barra de herramientas, la barra superior y el inventario, y ningún texto cortado a 1024 x 768.
 */
class HU48FarmInterfaceTest {

    private static <T extends Component> List<T> all(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                found.addAll(all(container, type));
            }
        }
        return found;
    }

    private static String described(Icon icon) {
        return icon instanceof ImageIcon image ? image.getDescription() : null;
    }

    @Test
    void HU48_ca1_everyToolHasA32PixelIconItsNameAndItsKey() {
        ToolBar bar = new ToolBar(new ToolModel());

        List<javax.swing.JToggleButton> buttons = all(bar, javax.swing.JToggleButton.class);
        assertEquals(Tool.values().length, buttons.size());
        for (Tool tool : Tool.values()) {
            javax.swing.JToggleButton button = buttons.get(tool.ordinal());
            assertEquals(32, button.getIcon().getIconWidth());
            assertEquals(32, button.getIcon().getIconHeight());
            assertEquals("icons/" + tool.name() + ".png", described(button.getIcon()));
            assertTrue(button.getText().contains(Texts.tool(tool)));
            assertTrue(button.getText().contains(String.valueOf(tool.key())));
        }
    }

    @Test
    void HU48_ca2_theTopBarShowsIconsForMoneySeasonWeatherAndForecastAndAnEnergyBarWithText() throws Exception {
        EventBus bus = new EventBus();
        HudPanel hud = new HudPanel(bus);
        bus.publish(new GameEvent.GameStarted(new HudView(500, 1, "SUMMER", 3, "RAINY", "FROST", 80, 100)));
        SwingUtilities.invokeAndWait(() -> { });

        List<String> icons = all(hud, JLabel.class).stream().map(l -> described(l.getIcon())).toList();
        assertTrue(icons.contains("icons/COIN.png"), icons.toString());
        assertTrue(icons.contains("icons/SUMMER.png"), icons.toString());
        assertTrue(icons.contains("icons/RAINY.png"), icons.toString());
        assertTrue(icons.contains("icons/FROST.png"), icons.toString());
        JProgressBar energy = all(hud, JProgressBar.class).get(0);
        assertEquals("80/100", energy.getString());
        assertTrue(all(hud, JLabel.class).stream().anyMatch(l -> l.getText().contains("Verano")));
    }

    @Test
    void HU48_ca3_everyOccupiedSlotShowsTheIconOfItsItemAndTheAmount() {
        TestApp app = TestApp.startedGame();
        InventoryModel model = new InventoryModel(app.controller, new ToolModel());
        model.refresh();
        InventoryGrid grid = new InventoryGrid(model);

        List<AbstractButton> slots = all(grid, AbstractButton.class);
        AbstractButton first = slots.get(0);
        assertEquals("items/" + model.cell(0).itemId() + ".png", described(first.getIcon()));
        assertTrue(first.getText().contains(String.valueOf(model.cell(0).amount())));
        AbstractButton empty = slots.get(slots.size() - 1);
        assertNull(empty.getIcon());
    }

    @Test
    void HU48_ca4_atTheMinimumWindowSizeNoComponentOfTheGameScreenIsCutOff() {
        UiTheme.install();
        TestApp withTutorial = TestApp.create();
        withTutorial.controller.newGame(TestApp.SEED);
        for (TestApp app : List.of(TestApp.startedGame(), withTutorial)) {
            GameScreen screen = new GameScreen(app.controller, app.bus, new SilentNavigator());
            screen.setSize(1024, 768);
            layoutEverything(screen);
            assertNothingCut(screen);
        }
    }

    private static boolean shown(Component component) {
        for (Component c = component; c != null; c = c.getParent()) {
            if (!c.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static void assertNothingCut(GameScreen screen) {
        for (Component child : allComponents(screen)) {
            Container parent = child.getParent();
            if (parent == null || !shown(child) || child.getWidth() == 0) {
                continue;
            }
            Rectangle inside = new Rectangle(0, 0, parent.getWidth(), parent.getHeight());
            assertTrue(inside.contains(child.getBounds()),
                    child.getClass().getSimpleName() + " " + child.getBounds() + " sticks out of " + inside);
            if (child instanceof AbstractButton || child instanceof JLabel) {
                assertTrue(child.getWidth() >= child.getPreferredSize().width,
                        child.getClass().getSimpleName() + " is narrower than its text");
            }
        }
    }

    @Test
    void HU48_ca5_withoutSpritesTheControlsShowOnlyText() {
        ToolBar bar = new ToolBar(new ToolModel(), AssetManager.vectorOnly());
        for (javax.swing.JToggleButton button : all(bar, javax.swing.JToggleButton.class)) {
            assertNull(button.getIcon());
            assertNotNull(button.getText());
        }
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 500);
        InventoryModel plain = new InventoryModel(app.controller, new ToolModel());
        plain.refresh();
        InventoryGrid grid = new InventoryGrid(plain, AssetManager.vectorOnly());
        assertNull(all(grid, AbstractButton.class).get(0).getIcon());
        HudPanel hud = new HudPanel(new EventBus(), AssetManager.vectorOnly());
        assertTrue(all(hud, JLabel.class).stream().allMatch(l -> l.getIcon() == null));
    }

    private static void layoutEverything(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container inner) {
                layoutEverything(inner);
            }
        }
    }

    private static List<Component> allComponents(Container root) {
        List<Component> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            found.add(child);
            if (child instanceof Container inner) {
                found.addAll(allComponents(inner));
            }
        }
        return found;
    }

    private static final class SilentNavigator implements ScreenNavigator {
        @Override
        public void showStart() { }

        @Override
        public void showGame() { }

        @Override
        public void showMessage(String text) { }

        @Override
        public boolean confirm(String text) {
            return true;
        }

        @Override
        public int choose(String title, List<String> options) {
            return 0;
        }

        @Override
        public void exit() { }
    }
}
