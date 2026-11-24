package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.Weather;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.components.HudModel;
import edu.ucc.farmsim.ui.components.HudPanel;
import edu.ucc.farmsim.ui.screens.GameScreenPresenter;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

/**
 * HU-34: la barra superior muestra dinero, fecha, clima y energía y se actualiza cuando recibe eventos. El modelo
 * ({@link HudModel}) es un observador sin Swing, así que se prueba sin pantalla.
 */
class HU34HudTest {

    private static final edu.ucc.farmsim.ui.screens.GameDialogs NO_DIALOGS = new edu.ucc.farmsim.support.RecordingDialogs();

    @Test
    void HU34_ca1_aNewGameShowsMoneyDateWeatherForecastAndFullEnergy() {
                // Dado un HUD que escucha el bus de eventos
        TestApp app = TestApp.create();
        HudModel hud = new HudModel();
        app.bus.subscribe(hud);

                // Cuando empieza una partida nueva
        app.controller.newGame(TestApp.SEED);

                // Entonces el HUD muestra 500 m, Año 1 Primavera Día 1, Sunny con un pronóstico, y energía 100/100
        assertEquals("500 m", hud.moneyText());
        assertEquals("Año 1 · Primavera · Día 1", hud.dateText());
        assertEquals("Soleado", hud.weatherText());
        assertTrue(hud.forecastText().startsWith("Mañana: "), hud.forecastText());
        assertEquals("100/100", hud.energyText());
        assertEquals(1.0, hud.energyFraction(), 0.0001);
    }

    @Test
    void HU34_ca2_energyChangesAreShownWhenTheEventArrivesWithoutAnyPolling() {
                // Dado un HUD con una partida iniciada
        TestApp app = TestApp.create();
        HudModel hud = new HudModel();
        app.bus.subscribe(hud);
        app.controller.newGame(TestApp.SEED);

                // Cuando una acción baja la energía a 98 y se publica el evento
        app.bus.publish(new GameEvent.EnergyChanged(98, 100));

                // Entonces el HUD muestra 98/100 y su barra está al 98 %
        assertEquals("98/100", hud.energyText());
        assertEquals(0.98, hud.energyFraction(), 0.0001);
        assertEquals("500 m", hud.moneyText());
    }

    @Test
    void HU34_ca2_aSaleShowsTheNewMoneyAsSoonAsItsEventArrives() {
                // Dado un HUD con una partida iniciada y 3 zanahorias para vender
        TestApp app = TestApp.create();
        HudModel hud = new HudModel();
        app.bus.subscribe(hud);
        app.controller.newGame(TestApp.SEED);
        app.state().inventory().add(edu.ucc.farmsim.domain.economy.ItemType.CARROT, 3);

                // Cuando el jugador las vende
        int earned = app.controller.sell("CARROT", 3).value();

                // Entonces el HUD muestra el nuevo saldo sin esperar al final del día
        assertEquals((500 + earned) + " m", hud.moneyText());
        assertEquals("100/100", hud.energyText(), "the rest of the HUD stays");
    }

    @Test
    void HU34_ca3_zeroEnergyShowsAnEmptyBarAndTheText0Of100NotJustAColor() {
        TestApp app = TestApp.create();
        HudModel hud = new HudModel();
        app.bus.subscribe(hud);
        app.controller.newGame(TestApp.SEED);

        app.bus.publish(new GameEvent.EnergyChanged(0, 100));

        assertEquals(0.0, hud.energyFraction(), 0.0001);
        assertEquals("0/100", hud.energyText());
    }

    @Test
    void HU34_ca4_endingTheDayShowsTheNextDayAndTheNewForecast() {
        TestApp app = TestApp.create();
        HudModel hud = new HudModel();
        app.bus.subscribe(hud);
        app.controller.newGame(TestApp.SEED);

                // Cuando termina el día
        Result<HudView> result = app.controller.endDay();

                // Entonces el HUD muestra el día 2 y el pronóstico que se sorteó
        assertEquals("Año 1 · Primavera · Día 2", hud.dateText());
        assertEquals(Texts.weather(result.value().weather()), hud.weatherText());
        assertEquals("Mañana: " + Texts.weather(result.value().forecast()), hud.forecastText());
        assertEquals("100/100", hud.energyText());
    }

    @Test
    void HU34_beforeAnyGameTheHudShowsDashes() {
        HudModel hud = new HudModel();

        assertEquals("—", hud.moneyText());
        assertEquals("—", hud.dateText());
        assertEquals("—/—", hud.energyText());
        assertEquals(0.0, hud.energyFraction(), 0.0001);
        hud.onEvent(new GameEvent.EnergyChanged(50, 100)); // no game yet: ignored without failing
        assertEquals("—/—", hud.energyText());
    }

    @Test
    void HU34_everySeasonAndWeatherHasASpanishName() {
        for (Season season : Season.values()) {
            assertNotNull(Texts.season(season.name()));
            assertFalse(Texts.season(season.name()).equals(season.name()), "untranslated " + season);
        }
        for (Weather weather : Weather.values()) {
            assertFalse(Texts.weather(weather.name()).equals(weather.name()), "untranslated " + weather);
        }
        assertEquals("OTHER", Texts.weather("OTHER"));
    }

    @Test
    void HU34_theSwingPanelShowsTheTextsOfTheModelWhenAnEventArrives() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.create();
        HudPanel panel = new HudPanel(app.bus);

        app.controller.newGame(TestApp.SEED);
        javax.swing.SwingUtilities.invokeAndWait(() -> { }); // deja que el panel termine su actualización en el hilo de Swing

        List<String> texts = new ArrayList<>();
        collectLabels(panel, texts);
        assertTrue(texts.contains("500 m"), texts.toString());
        assertTrue(texts.contains("Año 1 · Primavera · Día 1"), texts.toString());
    }

    @Test
    void HU10_theEndDayButtonLogicShowsAMessageOnlyWhenTheDayCouldNotEnd() {
        TestApp app = TestApp.create();
        List<String> messages = new ArrayList<>();
        GameScreenPresenter presenter = new GameScreenPresenter(app.controller, new edu.ucc.farmsim.ui.components.ToolModel(), messages::add, NO_DIALOGS);

        presenter.onEndDay(); // sin partida en curso
        assertEquals(List.of("No hay una partida en curso."), messages);

        messages.clear();
        app.controller.newGame(TestApp.SEED);
        presenter.onEndDay();
        assertTrue(messages.stream().allMatch(String::isBlank), "a successful end of day shows no message");
        assertEquals(2, app.controller.hud().value().day());
    }

    private static void collectLabels(Container container, List<String> texts) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label) {
                texts.add(label.getText());
            }
            if (child instanceof Container nested) {
                collectLabels(nested, texts);
            }
        }
    }
}
