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
 * <p>Muestra fecha, clima, pronóstico, dinero y una barra de energía con su texto, en una caja de madera que flota
 * sobre la esquina superior derecha de la granja. Se suscribe sola al bus de eventos y repinta sus etiquetas desde
 * {@link HudModel} cada vez que llega un evento.
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
     * Construye el panel y lo suscribe al bus de eventos.
     *
     * @param bus donde se publican los eventos del juego
     */
    public HudPanel(EventBus bus) {
        this(bus, AssetManager.shared());
    }

    /**
     * Construye el panel con íconos de los recursos dados (ninguno si faltan).
     *
     * @param bus    donde se publican los eventos del juego
     * @param assets de donde vienen los íconos
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

    /** La caja de madera en sí, que es lo que flota sobre el mapa. */
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
