package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.PlotText;
import org.junit.jupiter.api.Test;

/**
 * HU-07: inspeccionar una parcela: cultivo, etapa, regado y días para madurar, tal como lo muestra el tooltip (RN-05,
 * RN-08, RN-09).
 */
class HU07PlotInspectionTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    private static String tooltip(TestApp app, int col, int row) {
        PlotView view = app.controller.inspect(col, row).value();
        return PlotText.describe(view);
    }

    @Test
    void HU07_ca1_aPlotWithACropShowsCropStageWateredAndDaysLeft() {
                // Dado un tomate con d = 2 (T = 7), regado hoy
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.TOMATO, 2, 0, true, false);

                // Cuando el jugador inspecciona la parcela
        String text = tooltip(app, COL, ROW);

                // Entonces el tooltip dice que es un Sprout al que le faltan 5 días
        assertEquals("Tomate · Brote · Regado: sí · Faltan 5 días", text);
    }

    @Test
    void HU07_ca1_unwateredCropsSayNoAndOneDayLeftIsSingular() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 3);

        assertEquals("Zanahoria · Crecimiento · Regado: no · Falta 1 día", tooltip(app, COL, ROW));
    }

    @Test
    void HU07_ca1_aMatureCropSaysItIsReadyToHarvest() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 4);

        assertEquals("Zanahoria · Maduro · Listo para cosechar", tooltip(app, COL, ROW));
    }

    @Test
    void HU07_ca2_aFreeSoilPlotSaysSo() {
        TestApp app = TestApp.startedGame();

        assertEquals("Tierra de cultivo (libre)", tooltip(app, COL, ROW));
    }

    @Test
    void HU07_ca2_otherTerrainsShowTheirName() {
        TestApp app = TestApp.startedGame();

        assertEquals("Hierba", tooltip(app, 1, 1));
        assertEquals("Agua", tooltip(app, 13, 0));
        assertEquals("Obstáculo", tooltip(app, 0, 0));
    }

    @Test
    void HU07_ca3_aWitheredCropShowsWitheredAndNoDaysToMature() {
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 2, 3, false, true);

        assertEquals("Zanahoria · Marchito", tooltip(app, COL, ROW));
    }

    @Test
    void HU07_ca4_inspectingIsAPureQueryThatNeverChangesTheGame() {
                // Dada una partida con un cultivo regado
        TestApp app = TestApp.startedGame();
        GameFixtures.plant(app.state(), COL, ROW, CropType.CARROT, 1, 0, true, false);
        GameMemento before = app.state().createMemento();
        app.events.clear();

                // Cuando se inspecciona la parcela 10 veces
        for (int i = 0; i < 10; i++) {
            assertTrue(app.controller.inspect(COL, ROW).isOk());
        }

                // Entonces todo el estado es idéntico, no se gastó energía y no se publicó ningún evento
        assertEquals(before, app.state().createMemento());
        assertEquals(100, app.state().energy().current());
        assertTrue(app.events.isEmpty());
    }

    @Test
    void HU07_withoutAGameInProgressInspectingIsRejectedWithSinPartida() {
        assertEquals(ErrorCode.SIN_PARTIDA, TestApp.create().controller.inspect(COL, ROW).code());
    }

    @Test
    void HU07_coordinatesOutsideTheMapAreAnInternalErrorNotACrash() {
        assertEquals(ErrorCode.ERROR_INTERNO, TestApp.startedGame().controller.inspect(50, 50).code());
    }
}
