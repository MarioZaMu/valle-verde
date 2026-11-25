package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaLogro;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.util.List;

/**
 * La lógica del diálogo de logros, sin nada de Swing (HU-26): una fila por logro con lo que pide, su recompensa y si
 * está desbloqueado o cuánto lleva el jugador.
 *
 * <p>Stories: HU-26.
 */
public final class PresentadorLogros {

    private List<VistaLogro> logros = List.of();

    /**
     * Crea el presentador y lee los logros.
     *
     * @param juego la partida a la que pertenecen los logros
     */
    public PresentadorLogros(ComandosJuego juego) {
        Resultado<List<VistaLogro>> resultado = juego.logros();
        if (resultado.isOk()) {
            logros = resultado.value();
        }
    }

    /** Todos los logros. */
    public List<VistaLogro> logros() {
        return logros;
    }

    /** La fila de un logro tal como la muestra la tabla. */
    public Object[] fila(VistaLogro logro) {
        return new Object[] {logro.name(), logro.descripcion(), Textos.dinero(logro.recompensa()),
                estado(logro)};
    }

    /** {@code Desbloqueado} o {@code progreso/meta}, de modo que el estado nunca depende de un color. */
    public String estado(VistaLogro logro) {
        return logro.desbloqueado() ? Textos.DESBLOQUEADO : logro.progreso() + "/" + logro.objetivo();
    }
}
