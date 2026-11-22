package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.ui.assets.AssetManager;
import java.awt.FlowLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * Pattern: Observer — Role: ConcreteObserver (the Swing top bar, repainted when an event arrives).
 *
 * <p>Shows money, date, weather, forecast and an energy bar with its text. It subscribes itself to the
 * event bus and repaints its labels from {@link HudModel} each time an event arrives.
 *
 * <p>Stories: HU-34, HU-48.
 */
public final class HudPanel extends JPanel implements GameListener {

    private static final long serialVersionUID = 1L;
    private static final int GAP = 24;
    private static final int PERCENT = 100;

    private final transient HudModel model = new HudModel();
    private final JLabel money = new JLabel();
    private final JLabel date = new JLabel();
    private final JLabel weather = new JLabel();
    private final JLabel forecast = new JLabel();
    private final JProgressBar energy = new JProgressBar(0, PERCENT);
    private final transient AssetManager assets;

    /**
     * Builds the panel and subscribes it to the event bus.
     *
     * @param bus where game events are published
     */
    public HudPanel(EventBus bus) {
        this(bus, AssetManager.shared());
    }

    /**
     * Builds the panel with icons from the given assets (none if they are missing).
     *
     * @param bus    where game events are published
     * @param assets where the icons come from
     */
    public HudPanel(EventBus bus, AssetManager assets) {
        super(new FlowLayout(FlowLayout.LEFT, GAP, GAP / 2));
        this.assets = assets;
        energy.setStringPainted(true);
        add(money);
        add(date);
        add(weather);
        add(forecast);
        add(energy);
        update();
        bus.subscribe(this);
    }

    @Override
    public void onEvent(GameEvent event) {
        model.onEvent(event);
        if (SwingUtilities.isEventDispatchThread()) {
            update();
        } else {
            SwingUtilities.invokeLater(this::update);
        }
    }

    private void update() {
        money.setText(model.moneyText());
        money.setIcon(icon("COIN"));
        date.setText(model.dateText());
        date.setIcon(icon(model.seasonId()));
        weather.setText(model.weatherText());
        weather.setIcon(icon(model.weatherId()));
        forecast.setText(model.forecastText());
        forecast.setIcon(icon(model.forecastId()));
        energy.setValue((int) Math.round(model.energyFraction() * PERCENT));
        energy.setString(model.energyText());
    }

    private Icon icon(String id) {
        return id == null ? null : assets.icon("icons/" + id + ".png").orElse(null);
    }
}
