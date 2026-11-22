package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.MarketDialog;
import edu.ucc.farmsim.ui.components.MarketPresenter;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;

/** HU-38: the market with its Buy and Sell tabs (RN-16, RN-17, RN-19). */
class HU38MarketDialogTest {

    private static MarketPresenter presenterOf(TestApp app) {
        MarketPresenter presenter = new MarketPresenter(app.controller);
        presenter.refresh();
        return presenter;
    }

    @Test
    void HU38_ca2_theSellTabShowsThePriceWithItsTrendAndTheForecastTotal() {
        // Given 3 carrots in Spring
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);
        MarketPresenter presenter = presenterOf(app);

        var carrot = presenter.sells().stream().filter(o -> o.itemId().equals("CARROT")).findFirst().orElseThrow();

        // Then the price is 27 m with a trend that has a symbol and a word, and 3 units forecast 79 m
        assertEquals("27 m ▬ igual", presenter.sellPriceText(carrot));
        assertEquals("Total previsto: 79 m", presenter.previewSale("CARROT", 3));
    }

    @Test
    void HU38_ca2_theTrendIsAlwaysAWordAndASymbolNotJustAColour() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setDemand(app.state(), ItemType.CARROT, 80);
        app.controller.endDay();
        MarketPresenter presenter = presenterOf(app);

        var carrot = presenter.sells().stream().filter(o -> o.itemId().equals("CARROT")).findFirst().orElseThrow();

        assertTrue(presenter.sellPriceText(carrot).endsWith("▲ sube"), presenter.sellPriceText(carrot));
    }

    @Test
    void HU38_ca3_withoutMoneyThePurchaseIsBlockedWithTheSinFondosMessage() {
        // Given 100 m
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 100);
        MarketPresenter presenter = presenterOf(app);

        // Then buying 3 medicines (120 m) cannot be done and says why; the balance does not change
        assertFalse(presenter.canBuy("MEDICINE", 3));
        assertEquals("Fondos insuficientes: faltan 20 m.", presenter.purchaseBlock("MEDICINE", 3));
        assertEquals(100, app.state().money());
        assertTrue(presenter.canBuy("MEDICINE", 2));
    }

    @Test
    void HU38_ca4_theQuantityRangeIs1To99WhenBuyingAndUpToTheUnitsOwnedWhenSelling() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);
        MarketPresenter presenter = presenterOf(app);

        assertEquals(99, presenter.maxQuantity(true, "HAY"));
        assertEquals(3, presenter.maxQuantity(false, "CARROT"));
        assertEquals(0, presenter.maxQuantity(false, "POTATO"));
    }

    @Test
    void HU38_buyingAndSellingAnswerTheMessagesThePlayerSees() {
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);
        MarketPresenter presenter = presenterOf(app);

        assertEquals("Compraste 5 × Semilla de Maíz por 150 m.", presenter.buyUnits("CORN_SEED", 5));
        assertEquals("Vendiste 3 × Zanahoria por 79 m.", presenter.sellUnits("CARROT", 3));
        assertEquals("429 m", presenter.moneyText());
        assertEquals("La cantidad debe estar entre 1 y 0.", presenter.sellUnits("CARROT", 1));
    }

    @Test
    void HU38_ca1_theMarketOpensAsAModalDialogWithTheBuyAndSellTabs() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);

        MarketDialog dialog = new MarketDialog(null, new MarketPresenter(app.controller));

        assertTrue(dialog.isModal());
        JTabbedPane tabs = first(dialog, JTabbedPane.class);
        assertEquals("Comprar", tabs.getTitleAt(0));
        assertEquals("Vender", tabs.getTitleAt(1));
        JTable sell = tableNamed(dialog, "sell-table");
        assertEquals(7, sell.getRowCount());
        dialog.dispose();
    }

    @Test
    void HU38_ca2_selectingARowInTheSellTabForecastsAndSells() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.startedGame();
        app.state().inventory().add(ItemType.CARROT, 3);
        MarketDialog dialog = new MarketDialog(null, new MarketPresenter(app.controller));
        JTable sell = tableNamed(dialog, "sell-table");
        int row = indexOfRow(sell, "Zanahoria");
        sell.setRowSelectionInterval(row, row);
        JSpinner quantity = spinnerOf(sell);

        quantity.setValue(3);
        JButton button = buttonNamed(dialog, "sell-button");

        assertTrue(button.isEnabled());
        button.doClick();
        assertEquals(579, app.state().money());
        assertEquals(0, app.state().inventory().count(ItemType.CARROT));
        dialog.dispose();
    }

    @Test
    void HU38_ca3_theBuyButtonIsDisabledWhenThePlayerCannotPay() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 100);
        MarketDialog dialog = new MarketDialog(null, new MarketPresenter(app.controller));
        JTable buy = tableNamed(dialog, "buy-table");
        int row = indexOfRow(buy, "Medicina");
        buy.setRowSelectionInterval(row, row);
        JSpinner quantity = spinnerOf(buy);

        quantity.setValue(3);

        assertFalse(buttonNamed(dialog, "buy-button").isEnabled());
        quantity.setValue(2);
        assertTrue(buttonNamed(dialog, "buy-button").isEnabled());
        dialog.dispose();
    }

    private static int indexOfRow(JTable table, String name) {
        for (int i = 0; i < table.getRowCount(); i++) {
            if (name.equals(table.getValueAt(i, 0))) {
                return i;
            }
        }
        throw new AssertionError("no row " + name);
    }

    private static JSpinner spinnerOf(JTable table) {
        Container tab = table.getParent().getParent().getParent();
        return first(tab, JSpinner.class);
    }

    private static JTable tableNamed(Container root, String name) {
        List<JTable> found = new ArrayList<>();
        collect(root, JTable.class, found);
        return found.stream().filter(t -> name.equals(t.getName())).findFirst().orElseThrow();
    }

    private static JButton buttonNamed(Container root, String name) {
        List<JButton> found = new ArrayList<>();
        collect(root, JButton.class, found);
        return found.stream().filter(b -> name.equals(b.getName())).findFirst().orElseThrow();
    }

    private static <T extends Component> T first(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found.get(0);
    }

    private static <T extends Component> void collect(Container container, Class<T> type, List<T> found) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container nested) {
                collect(nested, type, found);
            }
        }
    }

    @Test
    void HU38_ca3_aCowWithoutMoneyIsBlockedWithTheSinFondosMessage() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 100);
        MarketPresenter presenter = presenterOf(app);

        assertEquals(true, presenter.isAnimal("COW"));
        assertFalse(presenter.canBuy("COW", 1));
        assertEquals("Fondos insuficientes: faltan 700 m.", presenter.purchaseBlock("COW", 1));
        assertEquals(1, presenter.maxQuantity(true, "COW"));
        assertEquals(100, app.state().money());
    }

    @Test
    void HU38_theHenCanBeBoughtWithACoopAndIsBlockedWithoutRoom() {
        TestApp app = TestApp.startedGame();
        MarketPresenter presenter = presenterOf(app);
        assertFalse(presenter.canBuy("HEN", 1));
        assertEquals("No hay espacio para más Gallinas: construye o amplía el edificio.",
                presenter.purchaseBlock("HEN", 1));

        GameFixtures.coop(app.state());
        presenter.refresh();

        assertTrue(presenter.canBuy("HEN", 1));
        assertEquals("Compraste: Gallina. Vive en el Gallinero 1.", presenter.buyUnits("HEN", 1));
        assertEquals(350, app.state().money());
        assertFalse(presenter.canBuy("HEN", 2));
    }
}
