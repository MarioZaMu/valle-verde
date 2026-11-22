package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import java.awt.FlowLayout;
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
 * <p>Stories: HU-34.
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

    /**
     * Builds the panel and subscribes it to the event bus.
     *
     * @param bus where game events are published
     */
    public HudPanel(EventBus bus) {
        super(new FlowLayout(FlowLayout.LEFT, GAP, GAP / 2));
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
        date.setText(model.dateText());
        weather.setText(model.weatherText());
        forecast.setText(model.forecastText());
        energy.setValue((int) Math.round(model.energyFraction() * PERCENT));
        energy.setString(model.energyText());
    }
}
