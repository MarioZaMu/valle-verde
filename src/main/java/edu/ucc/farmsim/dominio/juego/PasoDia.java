package edu.ucc.farmsim.dominio.juego;

/**
 * Un paso del final del día. Cada historia que agrega un efecto diario registra su propio paso en el {@link
 * SecuenciaDia}; el pipeline los ejecuta en el orden de su {@link FaseDia}.
 *
 * <p>Stories: HU-10.
 */
public interface PasoDia {

    /** La fase de RN-03 a la que pertenece este paso. */
    FaseDia fase();

    /**
     * Aplica el paso a la partida.
     *
     * @param estado  la partida que se avanza
     * @param reporte hechos del día que la aplicación convierte en eventos
     */
    void apply(EstadoJuego estado, ReporteDia reporte);
}
