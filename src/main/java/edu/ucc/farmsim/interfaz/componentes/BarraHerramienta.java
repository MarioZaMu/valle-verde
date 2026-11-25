package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * La barra de herramientas: un botón de alternancia por herramienta (con la etiqueta de su tecla) y el selector de
 * semilla de la herramienta Sembrar. Lee y escribe el {@link ModeloHerramienta} compartido, de modo que las teclas y los
 * botones siempre coinciden.
 *
 * <p>Stories: HU-36, HU-48.
 */
public final class BarraHerramienta extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] CULTIVOS_SEMILLA = {"CARROT", "POTATO", "TOMATO", "CORN", "PUMPKIN"};
    private static final String SUFIJO_SEMILLA = "_SEED";

    private final transient ModeloHerramienta modelo;
    private final JToggleButton[] botones = new JToggleButton[Herramienta.values().length];
    private boolean actualizando;

    /**
     * Construye la barra.
     *
     * @param modelo la selección de herramienta compartida
     */
    public BarraHerramienta(ModeloHerramienta modelo) {
        this(modelo, GestorRecurso.compartido());
    }

    /**
     * Construye la barra con íconos de los recursos dados (solo texto si faltan).
     *
     * @param modelo  la selección de herramienta compartida
     * @param recursos de donde vienen los íconos de las herramientas
     */
    public BarraHerramienta(ModeloHerramienta modelo, GestorRecurso recursos) {
        super(new FlowLayout(FlowLayout.CENTER, 6, 2));
        this.modelo = modelo;
        setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Herramienta herramienta : Herramienta.values()) {
            JToggleButton boton = new RanuraMadera(herramienta.clave() + " " + Textos.herramienta(herramienta));
            recursos.icono("icons/" + herramienta.name() + ".png").ifPresent(boton::setIcon);
            boton.addActionListener(evento -> {
                if (!actualizando) {
                    modelo.seleccionar(herramienta);
                }
            });
            botones[herramienta.ordinal()] = boton;
            group.add(boton);
            add(boton);
        }
        JLabel etiquetaSemilla = new JLabel(Textos.ETIQUETA_SEMILLA);
        etiquetaSemilla.setForeground(EstiloMadera.TINTA);
        etiquetaSemilla.setFont(etiquetaSemilla.getFont().deriveFont(java.awt.Font.BOLD));
        add(etiquetaSemilla);
        String[] nombres = new String[CULTIVOS_SEMILLA.length];
        for (int i = 0; i < CULTIVOS_SEMILLA.length; i++) {
            nombres[i] = Nombres.cultivo(CULTIVOS_SEMILLA[i]);
        }
        JComboBox<String> semillas = new JComboBox<>(nombres);
        semillas.setFocusable(false);
        semillas.addActionListener(evento -> {
            if (!actualizando) {
                modelo.semillaSeleccionar(CULTIVOS_SEMILLA[semillas.getSelectedIndex()] + SUFIJO_SEMILLA);
            }
        });
        add(semillas);
        modelo.addChangeListener(this::sincronizarDesdeModelo);
        sincronizarDesdeModelo();
    }

    private void sincronizarDesdeModelo() {
        actualizando = true;
        try {
            Herramienta actual = modelo.actual();
            for (Herramienta herramienta : Herramienta.values()) {
                botones[herramienta.ordinal()].setSelected(herramienta == actual);
            }
            if (actual == null) {
                                // el grupo de botones mantiene uno seleccionado; límpialo explícitamente
                for (JToggleButton boton : botones) {
                    boton.getModel().setGroup(null);
                    boton.setSelected(false);
                }
            }
        } finally {
            actualizando = false;
        }
    }
}
