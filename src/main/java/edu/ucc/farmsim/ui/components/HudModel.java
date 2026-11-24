package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.ui.theme.Texts;

/**
 * Pattern: Observer — Role: ConcreteObserver (keeps the top-bar data up to date from game events).
 *
 * <p>No tiene Swing: escucha el bus de eventos, recuerda el último {@link HudView} y expone los textos en español que
 * se muestran. El {@link HudPanel} de Swing solo pinta esos textos, de modo que la lógica del HUD se prueba sin
 * pantalla y la pantalla se actualiza por eventos, nunca por sondeo.
 *
 * <p>Stories: HU-34, HU-48.
 */
public class HudModel implements GameListener {

    private HudView hud;

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof GameEvent.GameStarted started) {
            hud = started.hud();
        } else if (event instanceof GameEvent.DayEnded ended) {
            hud = ended.hud();
        } else if (event instanceof GameEvent.EnergyChanged changed && hud != null) {
            hud = new HudView(hud.money(), hud.year(), hud.season(), hud.day(), hud.weather(), hud.forecast(),
                    changed.energy(), changed.maxEnergy());
        } else if (event instanceof GameEvent.MoneyChanged changed && hud != null) {
            hud = new HudView(changed.money(), hud.year(), hud.season(), hud.day(), hud.weather(), hud.forecast(),
                    hud.energy(), hud.maxEnergy());
        }
    }

    /** Dinero, por ejemplo {@code 500 m}. */
    public String moneyText() {
        return hud == null ? Texts.NO_VALUE : Texts.money(hud.money());
    }

    /** Fecha, por ejemplo {@code Año 1 · Primavera · Día 1}. */
    public String dateText() {
        return hud == null ? Texts.NO_VALUE : Texts.date(hud.year(), hud.season(), hud.day());
    }

    /** Clima de hoy, por ejemplo {@code Soleado}. */
    public String weatherText() {
        return hud == null ? Texts.NO_VALUE : Texts.weather(hud.weather());
    }

    /** Pronóstico de mañana, por ejemplo {@code Mañana: Lluvioso}. */
    public String forecastText() {
        return hud == null ? Texts.NO_VALUE : Texts.forecast(hud.forecast());
    }

    /** Id de la estación, por ejemplo {@code SPRING}, o {@code null} antes de que empiece una partida. */
    public String seasonId() {
        return hud == null ? null : hud.season();
    }

    /** Id del clima de hoy, por ejemplo {@code RAINY}, o {@code null} antes de que empiece una partida. */
    public String weatherId() {
        return hud == null ? null : hud.weather();
    }

    /** Id del clima de mañana, o {@code null} antes de que empiece una partida. */
    public String forecastId() {
        return hud == null ? null : hud.forecast();
    }

    /** Energía como texto, por ejemplo {@code 98/100}: la información nunca depende solo de un color. */
    public String energyText() {
        return hud == null ? Texts.NO_VALUE + "/" + Texts.NO_VALUE : Texts.energy(hud.energy(), hud.maxEnergy());
    }

    /** Energía como una fracción de 0 a 1, para la barra. */
    public double energyFraction() {
        return hud == null || hud.maxEnergy() == 0 ? 0.0 : (double) hud.energy() / hud.maxEnergy();
    }
}
