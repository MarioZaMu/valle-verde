package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaInventario;
import edu.ucc.farmsim.aplicacion.dto.VistaRanura;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.evento.OyenteJuego;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que muestra la grilla del inventario (HU-37), sin nada de Swing: una celda por casilla, cuál semilla está elegida
 * y el clic que elige una semilla para la herramienta Sembrar. Escucha los eventos del juego y recarga el inventario
 * cuando cambia; si la semilla elegida se agota, la elección se borra (HU-37 · CA-3).
 *
 * <p>Pattern: Observer — Role: ConcreteObserver (keeps the inventory grid up to date from game events)
 *
 * <p>Stories: HU-37.
 */
public final class ModeloInventario implements OyenteJuego {

    /**
     * Una celda de la grilla.
     *
     * @param idArticulo id del objeto, o {@code null} para una casilla vacía
     * @param name   nombre en español, o un texto vacío
     * @param monto unidades en la casilla (0 si está vacía)
     */
    public record Celda(String idArticulo, String name, int monto) {

        /** Verdadero para una casilla sin objetos. */
        public boolean isEmpty() {
            return idArticulo == null;
        }
    }

    private static final String SUFIJO_SEMILLA = "_SEED";

    private final ComandosJuego juego;
    private final ModeloHerramienta herramientas;
    private final List<Runnable> oyentes = new ArrayList<>();
    private VistaInventario vista = new VistaInventario(0, List.of());

    /**
     * Crea el modelo.
     *
     * @param juego  de dónde viene el inventario
     * @param herramientas la selección de herramienta compartida, cuya semilla elegida cambia este modelo
     */
    public ModeloInventario(ComandosJuego juego, ModeloHerramienta herramientas) {
        this.juego = juego;
        this.herramientas = herramientas;
    }

    @Override
    public void onEvento(EventoJuego evento) {
        if (evento instanceof EventoJuego.JuegoIniciado || evento instanceof EventoJuego.InventarioCambiado) {
            refrescar();
        }
    }

    /** Carga el inventario otra vez y corrige la elección de semilla si esa semilla ya no está. */
    public void refrescar() {
        Resultado<VistaInventario> resultado = juego.inventario();
        if (resultado.isOk()) {
            vista = resultado.value();
            String elegido = herramientas.semillaSeleccionada();
            if (elegido != null && vista.ranuras().stream().noneMatch(ranura -> ranura.idArticulo().equals(elegido))) {
                herramientas.semillaLimpiar();
            }
            dispararCambiado();
        }
    }

    /** Número de casillas de la grilla (24, o 36 con un almacén). */
    public int conteoRanura() {
        return vista.capacidad();
    }

    /** La celda de un índice de casilla, desde 0. */
    public Celda celda(int indice) {
        if (indice < 0 || indice >= vista.capacidad()) {
            throw new IllegalArgumentException("no such slot: " + indice);
        }
        if (indice < vista.ranuras().size()) {
            VistaRanura ranura = vista.ranuras().get(indice);
            return new Celda(ranura.idArticulo(), ranura.name(), ranura.monto());
        }
        return new Celda(null, "", 0);
    }

    /** Si la celda contiene la semilla que usará la herramienta Sembrar. */
    public boolean isSelected(int indice) {
        Celda celda = celda(indice);
        return !celda.isEmpty() && celda.idArticulo().equals(herramientas.semillaSeleccionada());
    }

    /**
     * Un clic en una celda: si contiene una semilla, la herramienta Sembrar la usa desde ahora. Las otras celdas no
     * hacen nada.
     */
    public void seleccionar(int indice) {
        Celda celda = celda(indice);
        if (!celda.isEmpty() && celda.idArticulo().endsWith(SUFIJO_SEMILLA)) {
            herramientas.semillaSeleccionar(celda.idArticulo());
            dispararCambiado();
        }
    }

    /** Registra algo que se ejecuta cuando hay que redibujar la grilla. */
    public void addChangeListener(Runnable oyente) {
        oyentes.add(oyente);
    }

    private void dispararCambiado() {
        for (Runnable oyente : new ArrayList<>(oyentes)) {
            oyente.run();
        }
    }
}
