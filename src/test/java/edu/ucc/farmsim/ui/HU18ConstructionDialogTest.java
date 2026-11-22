package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.application.dto.BuildingOfferView;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.ConstructionDialog;
import edu.ucc.farmsim.ui.components.ConstructionPresenter;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;

/** The construction dialog opened with the Build tool (HU-18). */
class HU18ConstructionDialogTest {

    private static BuildingOfferView offer(ConstructionPresenter presenter, String type) {
        return presenter.catalog().stream().filter(o -> o.type().equals(type)).findFirst().orElseThrow();
    }

    @Test
    void HU18_ca1_theCatalogueShowsSizeCostAndState() {
        TestApp app = TestApp.startedGame();
        ConstructionPresenter presenter = new ConstructionPresenter(app.controller, 2, 7);

        Object[] coop = presenter.row(offer(presenter, "COOP"));

        assertEquals("Esquina superior izquierda en (2, 7)", presenter.placeText());
        assertEquals("500 m", presenter.moneyText());
        assertEquals("Gallinero", coop[0]);
        assertEquals("2 × 2", coop[1]);
        assertEquals("400 m", coop[2]);
        assertEquals("0/2 construidos", coop[3]);
        assertTrue(presenter.canBuild(offer(presenter, "COOP")));
    }

    @Test
    void HU18_ca5_aLockedBuildingSaysWhichAchievementItNeedsAndCannotBeBuilt() {
        TestApp app = TestApp.startedGame();
        ConstructionPresenter presenter = new ConstructionPresenter(app.controller, 2, 7);

        BuildingOfferView barn = offer(presenter, "BARN");

        assertEquals("Bloqueado: requiere el logro «Manos a la tierra»", presenter.state(barn));
        assertEquals("Edificio bloqueado: requiere el logro «Manos a la tierra».", presenter.block(barn));
        assertFalse(presenter.canBuild(barn));
    }

    @Test
    void HU18_ca3_withoutMoneyTheBuildButtonIsBlockedWithTheMissingAmount() {
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 399);
        ConstructionPresenter presenter = new ConstructionPresenter(app.controller, 2, 7);

        assertEquals("Fondos insuficientes: faltan 1 m.", presenter.block(offer(presenter, "COOP")));
        assertFalse(presenter.canBuild(offer(presenter, "COOP")));
    }

    @Test
    void HU18_ca4_atTheLimitTheBuildingIsBlocked() {
        TestApp app = TestApp.startedGame();
        GameFixtures.build(app.state(), BuildingType.COOP, 2, 7);
        GameFixtures.build(app.state(), BuildingType.COOP, 5, 7);
        ConstructionPresenter presenter = new ConstructionPresenter(app.controller, 8, 7);

        assertEquals("Ya tienes el máximo de Gallineros.", presenter.block(offer(presenter, "COOP")));
    }

    @Test
    void HU18_ca1_buildingFromTheDialogUsesTheClickedPlotAsTheCorner() {
        TestApp app = TestApp.startedGame();
        ConstructionPresenter presenter = new ConstructionPresenter(app.controller, 2, 7);

        String message = presenter.build(offer(presenter, "COOP"));

        assertEquals("Construiste: Gallinero.", message);
        assertEquals(0, presenter.lastBuilt().orElseThrow().id());
        assertEquals(100, app.state().money());
        assertEquals("100 m", presenter.moneyText());
    }

    @Test
    void HU18_ca2_anInvalidPlaceAnswersTheRn35MessageAndBuildsNothing() {
        TestApp app = TestApp.startedGame();
        ConstructionPresenter presenter = new ConstructionPresenter(app.controller, 4, 2);

        String message = presenter.build(offer(presenter, "COOP"));

        assertEquals("No se puede construir aquí: el área debe ser hierba libre dentro del mapa.", message);
        assertTrue(presenter.lastBuilt().isEmpty());
        assertEquals(500, app.state().money());
    }

    @Test
    void HU18_theDialogBuildsTheSelectedRowAndClosesAndAnUnlockedBarnEnablesTheButton() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.startedGame();
        GameFixtures.setMoney(app.state(), 2000);
        GameFixtures.unlock(app.state(), Achievement.GREEN_THUMB);
        ConstructionDialog dialog = new ConstructionDialog(null, new ConstructionPresenter(app.controller, 2, 7));
        JTable table = first(dialog, JTable.class);
        JButton build = named(dialog, "build-button");
        assertTrue(dialog.isModal());
        assertEquals(4, table.getRowCount());
        assertFalse(build.isEnabled());

        table.setRowSelectionInterval(1, 1);
        assertTrue(build.isEnabled());
        table.setRowSelectionInterval(3, 3);
        assertFalse(build.isEnabled());
        table.setRowSelectionInterval(0, 0);
        build.doClick();

        assertEquals(1, app.state().farm().buildings().size());
        assertFalse(dialog.isDisplayable());
    }

    private static JButton named(Container root, String name) {
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
            if (child instanceof Container inner) {
                collect(inner, type, found);
            }
        }
    }
}
