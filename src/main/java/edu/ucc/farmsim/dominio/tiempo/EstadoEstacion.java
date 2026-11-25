package edu.ucc.farmsim.dominio.tiempo;

/**
 * Pattern: State — Role: State interface (the behaviour that changes with the season).
 *
 * <p>Cada estación sabe qué estación le sigue. Los objetos de estado no guardan datos y se comparten.
 *
 * <p>Stories: HU-09, HU-11.
 */
public interface EstadoEstacion {

    /** La estación que representa este estado. */
    Estacion estacion();

    /** El estado que sigue a este en el calendario (después del Invierno viene la Primavera). */
    EstadoEstacion next();

    /** La estrategia que sortea el clima de los días de esta estación (RN-11). */
    EstrategiaClima estrategiaClima();

    /** Devuelve el objeto de estado compartido de una estación. */
    static EstadoEstacion of(Estacion estacion) {
        return switch (estacion) {
            case SPRING -> EstadoPrimavera.INSTANCIA;
            case SUMMER -> EstadoVerano.INSTANCIA;
            case AUTUMN -> EstadoOtonio.INSTANCIA;
            case WINTER -> EstadoInvierno.INSTANCIA;
        };
    }
}
