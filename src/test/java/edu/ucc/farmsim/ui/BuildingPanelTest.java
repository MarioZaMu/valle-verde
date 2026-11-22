package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.BuildingDialog;
import edu.ucc.farmsim.ui.components.BuildingPresenter;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;

/** The building panel: feed, collect and cure from the animal table (HU-20, HU-22, HU-23). */
class BuildingPanelTest {

    private static Building coopWith(TestApp app, AnimalCondition... conditions) {
        Building coop = GameFixtures.coop(app.state());
        for (AnimalCondition condition : conditions) {
            GameFixtures.house(coop, AnimalType.HEN, condition, condition == AnimalCondition.SICK ? 3 : 0, false, 0);
        }
        return coop;
    }

    @Test
    void HU20_thePanelListsTheAnimalsAndEnablesFeedingOnlyWhenSomeoneCanEat() {
        TestApp app = TestApp.startedGame();
        coopWith(app, AnimalCondition.HEALTHY, AnimalCondition.SICK);
        BuildingPresenter presenter = new BuildingPresenter(app.controller, 0);

        assertEquals("Gallinero 1 — 2/4 animales", presenter.title());
        assertEquals(2, presenter.animals().size());
        assertEquals("Sana", presenter.row(presenter.animals().get(0))[1]);
        assertEquals("Enferma", presenter.row(presenter.animals().get(1))[1]);
        assertTrue(presenter.canFeed());
        assertFalse(presenter.canCollect());
        assertFalse(presenter.canCure(0));
        assertTrue(presenter.canCure(1));
        assertFalse(presenter.canCure(5));
    }

    @Test
    void HU20_feedingFromThePanelAnswersTheMessageAndDisablesTheButton() {
        TestApp app = TestApp.startedGame();
        coopWith(app, AnimalCondition.HEALTHY, AnimalCondition.HEALTHY);
        app.state().inventory().add(ItemType.HAY, 5);
        BuildingPresenter presenter = new BuildingPresenter(app.controller, 0);

        String message = presenter.feed();

        assertEquals("Alimentaste a 2 animal(es).", message);
        assertFalse(presenter.canFeed());
        assertEquals("No hay animales por alimentar.", presenter.feed());
    }

    @Test
    void HU20_aRejectedFeedingShowsTheRn35Message() {
        TestApp app = TestApp.startedGame();
        coopWith(app, AnimalCondition.HEALTHY);

        assertEquals("Heno insuficiente: necesitas 1.", new BuildingPresenter(app.controller, 0).feed());
    }

    @Test
    void HU22_collectingFromThePanelMovesTheProductsAndAnEmptyBuildingSaysSo() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.HEALTHY, 0, false, 2);
        BuildingPresenter presenter = new BuildingPresenter(app.controller, 0);
        assertTrue(presenter.canCollect());

        assertEquals("Recogiste 2 × Huevo.", presenter.collect());

        assertFalse(presenter.canCollect());
        assertEquals(2, app.state().inventory().count(ItemType.EGG));
        assertEquals("No hay productos que recoger.", presenter.collect());
    }

    @Test
    void HU22_aFullInventoryShowsTheRn35Message() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.HEALTHY, 0, false, 1);
        GameFixtures.fillInventoryExcept(app.state(), ItemType.CARROT, 5);

        assertEquals("Inventario lleno: libera espacio o construye un Almacén.",
                new BuildingPresenter(app.controller, 0).collect());
    }

    @Test
    void HU23_curingFromThePanelNeedsMedicineAndLeavesTheHenHealthy() {
        TestApp app = TestApp.startedGame();
        coopWith(app, AnimalCondition.SICK);
        BuildingPresenter presenter = new BuildingPresenter(app.controller, 0);

        assertEquals("No tienes Medicina.", presenter.cure(0));
        app.state().inventory().add(ItemType.MEDICINE, 1);
        assertEquals("El animal se curó.", presenter.cure(0));

        assertFalse(presenter.canCure(0));
        assertEquals("Sana", presenter.row(presenter.animals().get(0))[1]);
        assertEquals("Este animal no está enfermo.", presenter.cure(0));
    }

    @Test
    void HU23_thePanelOfAMissingBuildingIsEmptyAndNeverThrows() {
        TestApp app = TestApp.startedGame();

        BuildingPresenter presenter = new BuildingPresenter(app.controller, 3);

        assertEquals("Edificio", presenter.title());
        assertTrue(presenter.animals().isEmpty());
        assertFalse(presenter.canFeed());
        assertFalse(presenter.canCollect());
    }

    @Test
    void HU20_theDialogFeedsCollectsAndCuresWithItsButtons() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.startedGame();
        Building coop = coopWith(app, AnimalCondition.HEALTHY, AnimalCondition.SICK);
        app.state().inventory().add(ItemType.HAY, 3);
        app.state().inventory().add(ItemType.MEDICINE, 1);
        BuildingDialog dialog = new BuildingDialog(null, new BuildingPresenter(app.controller, 0));
        JTable table = tableNamed(dialog, "animal-table");
        assertEquals(2, table.getRowCount());
        assertTrue(dialog.isModal());
        assertFalse(buttonNamed(dialog, "cure-button").isEnabled());
        assertFalse(buttonNamed(dialog, "collect-button").isEnabled());

        buttonNamed(dialog, "feed-button").doClick();
        table.setRowSelectionInterval(1, 1);
        assertTrue(buttonNamed(dialog, "cure-button").isEnabled());
        buttonNamed(dialog, "cure-button").doClick();

        assertTrue(coop.animals().get(0).isFedToday());
        assertEquals(AnimalCondition.HEALTHY, coop.animals().get(1).condition());
        dialog.dispose();
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
