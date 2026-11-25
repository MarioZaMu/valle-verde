package edu.ucc.farmsim.apoyo;

import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.interfaz.pantallas.DialogosJuego;
import java.util.ArrayList;
import java.util.List;

/** Un {@link DialogosJuego} que no abre nada y recuerda lo que pidió la pantalla. */
public final class DialogosGrabacion implements DialogosJuego {

    /**
     * Lo que se abrió, en orden: {@code mercado}, {@code inventario}, {@code construction:col,fila}, {@code edificio:id}.
     */
    public final List<String> abierto = new ArrayList<>();
    /** Los diálogos abiertos. */
    public final List<VistaConversacion> conversaciones = new ArrayList<>();
    /** Los resúmenes mostrados. */
    public final List<VistaResumenDia> resumenes = new ArrayList<>();

    @Override
    public void mercadoMostrar() {
        abierto.add("mercado");
    }

    @Override
    public void inventarioMostrar() {
        abierto.add("inventario");
    }

    @Override
    public void construccionMostrar(int col, int fila) {
        abierto.add("construction:" + col + "," + fila);
    }

    @Override
    public void edificioMostrar(int idEdificio) {
        abierto.add("building:" + idEdificio);
    }

    @Override
    public void logrosMostrar() {
        abierto.add("logros");
    }

    @Override
    public void ranurasMostrar(boolean guardando) {
        abierto.add(guardando ? "slots:save" : "slots:load");
    }

    @Override
    public void resumenMostrar(VistaResumenDia resumen) {
        resumenes.add(resumen);
    }

    @Override
    public void conversacionMostrar(VistaConversacion conversacion) {
        abierto.add("dialogue:" + conversacion.npcId());
        conversaciones.add(conversacion);
    }
}
