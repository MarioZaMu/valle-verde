package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.theme.WoodStyle;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * Pattern: Observer — Role: ConcreteObserver (the Swing top bar, repainted when an event arrives).
 *
 * <p>Shows date, weather, forecast, money and an energy bar with its text, in a wooden box that floats over
 * the top right corner of the farm. It subscribes itself to the event bus and repaints its labels from
 * {@link HudModel} each time an event arrives.
 *
 * <p>Stories: HU-34, HU-48.
 */
public final class HudPanel extends JPanel implements GameListener {

    private static final long serialVersionUID = 1L;
    private static final int PERCENT = 100;
    private static final int GAP = 4;
    private static final float DATE_SIZE = 16f;

    private final transient HudModel model = new HudModel();
    private final JLabel money = new JLabel();
    private final JLabel date = new JLabel();
    private final JLabel weather = new JLabel();
    private final JLabel forecast = new JLabel();
    private final JProgressBar energy = new EnergyBar(PERCENT);
    private final WoodBox box = new WoodBox(new GridBagLayout());
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
        super(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        this.assets = assets;
        setOpaque(false);
        for (JLabel label : new JLabel[] {money, date, weather, forecast}) {
            label.setForeground(WoodStyle.INK);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        }
        date.setFont(date.getFont().deriveFont(DATE_SIZE));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(GAP, GAP, GAP, GAP);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        box.add(date, c);
        c.gridwidth = 1;
        c.gridy = 1;
        box.add(weather, c);
        c.gridx = 1;
        box.add(forecast, c);
        c.gridx = 0;
        c.gridy = 2;
        box.add(money, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        box.add(energy, c);
        add(box);
        update();
        bus.subscribe(this);
    }

    /** The wooden box itself, which is what floats over the map. */
    public JComponent box() {
        return box;
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
