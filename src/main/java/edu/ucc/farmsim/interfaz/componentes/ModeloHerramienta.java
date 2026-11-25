package edu.ucc.farmsim.interfaz.componentes;

import java.util.ArrayList;
import java.util.List;

/**
 * Qué herramienta está seleccionada y qué semilla usa la herramienta Sembrar. Sin Swing: la barra de herramientas, las
 * teclas y el mapa comparten este único modelo, así que nunca pueden discrepar. Se avisa a los oyentes después de cada
 * cambio.
 *
 * <p>Stories: HU-36.
 */
public final class ModeloHerramienta {

    /** Semilla que usa la herramienta Sembrar hasta que el jugador elige otra. */
    public static final String SEMILLA_PREDETERMINADA = "CARROT_SEED";

    private static final String SUFIJO_SEMILLA = "_SEED";

    private Herramienta actual;
    private String semillaSeleccionada = SEMILLA_PREDETERMINADA;
    private final List<Runnable> oyentes = new ArrayList<>();

    /** La herramienta seleccionada, o {@code null} cuando no hay ninguna. */
    public Herramienta actual() {
        return actual;
    }

    /** Selecciona una herramienta. */
    public void seleccionar(Herramienta herramienta) {
        this.actual = herramienta;
        dispararCambiado();
    }

    /** Deselecciona la herramienta (Escape). */
    public void deseleccionar() {
        this.actual = null;
        dispararCambiado();
    }

    /**
     * Maneja un carácter escrito: los dígitos de 1 a 5 seleccionan una herramienta.
     *
     * @return {@code true} si el carácter era una tecla de herramienta
     */
    public boolean claveManejar(char clave) {
        for (Herramienta herramienta : Herramienta.values()) {
            if (herramienta.clave() == clave) {
                seleccionar(herramienta);
                return true;
            }
        }
        return false;
    }

    /** Id de la semilla que usa la herramienta Sembrar, por ejemplo {@code CARROT_SEED}. */
    public String semillaSeleccionada() {
        return semillaSeleccionada;
    }

    /**
     * Elige la semilla que usa la herramienta Sembrar.
     *
     * @throws IllegalArgumentException si el id no es un id de semilla
     */
    public void semillaSeleccionar(String idSemilla) {
        if (idSemilla == null || !idSemilla.endsWith(SUFIJO_SEMILLA)) {
            throw new IllegalArgumentException("Not a seed id: " + idSemilla);
        }
        this.semillaSeleccionada = idSemilla;
        dispararCambiado();
    }

    /** Registra algo que se ejecuta después de cada cambio de la selección. */
    /** Olvida la semilla elegida, por ejemplo cuando se sembró la última (HU-37 · CA-3). */
    public void semillaLimpiar() {
        this.semillaSeleccionada = null;
        dispararCambiado();
    }

    public void addChangeListener(Runnable oyente) {
        oyentes.add(oyente);
    }

    private void dispararCambiado() {
        for (Runnable oyente : new ArrayList<>(oyentes)) {
            oyente.run();
        }
    }
}
