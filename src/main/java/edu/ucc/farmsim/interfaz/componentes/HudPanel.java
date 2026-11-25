package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.evento.OyenteJuego;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
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
 * {@link ModeloHud} cada vez que llega un evento.
 *
 * <p>Stories: HU-34, HU-48.
 */
public final class HudPanel extends JPanel implements OyenteJuego {

    private static final long serialVersionUID = 1L;
    private static final int PORCENTAJE = 100;
    private static final int HUECO = 4;
    private static final float TAMANIO_FECHA = 16f;

    private final transient ModeloHud modelo = new ModeloHud();
    private final JLabel dinero = new JLabel();
    private final JLabel fecha = new JLabel();
    private final JLabel clima = new JLabel();
    private final JLabel pronostico = new JLabel();
    private final JProgressBar energia = new BarraEnergia(PORCENTAJE);
    private final CajaMadera caja = new CajaMadera(new GridBagLayout());
    private final transient GestorRecurso recursos;

    /**
     * Construye el panel y lo suscribe al bus de eventos.
     *
     * @param bus donde se publican los eventos del juego
     */
    public HudPanel(BusEvento bus) {
        this(bus, GestorRecurso.compartido());
    }

    /**
     * Construye el panel con íconos de los recursos dados (ninguno si faltan).
     *
     * @param bus    donde se publican los eventos del juego
     * @param recursos de donde vienen los íconos
     */
    public HudPanel(BusEvento bus, GestorRecurso recursos) {
        super(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        this.recursos = recursos;
        setOpaque(false);
        for (JLabel etiqueta : new JLabel[] {dinero, fecha, clima, pronostico}) {
            etiqueta.setForeground(EstiloMadera.TINTA);
            etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD));
        }
        fecha.setFont(fecha.getFont().deriveFont(TAMANIO_FECHA));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(HUECO, HUECO, HUECO, HUECO);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        caja.add(fecha, c);
        c.gridwidth = 1;
        c.gridy = 1;
        caja.add(clima, c);
        c.gridx = 1;
        caja.add(pronostico, c);
        c.gridx = 0;
        c.gridy = 2;
        caja.add(dinero, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        caja.add(energia, c);
        add(caja);
        actualizar();
        bus.suscribir(this);
    }

    /** La caja de madera en sí, que es lo que flota sobre el mapa. */
    public JComponent caja() {
        return caja;
    }

    @Override
    public void onEvento(EventoJuego evento) {
        modelo.onEvento(evento);
        if (SwingUtilities.isEventDispatchThread()) {
            actualizar();
        } else {
            SwingUtilities.invokeLater(this::actualizar);
        }
    }

    private void actualizar() {
        dinero.setText(modelo.textoDinero());
        dinero.setIcon(icono("COIN"));
        fecha.setText(modelo.textoFecha());
        fecha.setIcon(icono(modelo.idEstacion()));
        clima.setText(modelo.textoClima());
        clima.setIcon(icono(modelo.idClima()));
        pronostico.setText(modelo.textoPronostico());
        pronostico.setIcon(icono(modelo.idPronostico()));
        energia.setValue((int) Math.round(modelo.fraccionEnergia() * PORCENTAJE));
        energia.setString(modelo.textoEnergia());
    }

    private Icon icono(String id) {
        return id == null ? null : recursos.icono("icons/" + id + ".png").orElse(null);
    }
}
