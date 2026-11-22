package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.ui.theme.Texts;

/**
 * Pattern: Observer — Role: ConcreteObserver (keeps the top-bar data up to date from game events).
 *
 * <p>It has no Swing: it listens to the event bus, remembers the last {@link HudView} and exposes the
 * Spanish texts to show. The Swing {@link HudPanel} only paints these texts, so the HUD logic is tested
 * without a display and the screen is updated by events, never by polling.
 *
 * <p>Stories: HU-34.
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
        }
    }

    /** Money, for example {@code 500 m}. */
    public String moneyText() {
        return hud == null ? Texts.NO_VALUE : Texts.money(hud.money());
    }

    /** Date, for example {@code Año 1 · Primavera · Día 1}. */
    public String dateText() {
        return hud == null ? Texts.NO_VALUE : Texts.date(hud.year(), hud.season(), hud.day());
    }

    /** Weather of today, for example {@code Soleado}. */
    public String weatherText() {
        return hud == null ? Texts.NO_VALUE : Texts.weather(hud.weather());
    }

    /** Forecast for tomorrow, for example {@code Mañana: Lluvioso}. */
    public String forecastText() {
        return hud == null ? Texts.NO_VALUE : Texts.forecast(hud.forecast());
    }

    /** Energy as text, for example {@code 98/100}: the information never depends on a color alone. */
    public String energyText() {
        return hud == null ? Texts.NO_VALUE + "/" + Texts.NO_VALUE : Texts.energy(hud.energy(), hud.maxEnergy());
    }

    /** Energy as a fraction from 0 to 1, for the bar. */
    public double energyFraction() {
        return hud == null || hud.maxEnergy() == 0 ? 0.0 : (double) hud.energy() / hud.maxEnergy();
    }
}
