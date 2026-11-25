package edu.ucc.farmsim.dominio.guardado;

/**
 * El lugar donde se guardan las partidas no se pudo alcanzar o rechazó la operación. La infraestructura convierte todo
 * fallo técnico en esta excepción no comprobada, de modo que el dominio y la aplicación nunca ven tipos de MongoDB; la
 * aplicación la traduce a {@code ERROR_ES} (RN-31, RN-35).
 *
 * <p>Stories: HU-33.
 */
public final class ExcepcionAlmacenamientoGuardado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param detalle explicación corta en español que se muestra al jugador
     * @param causa  el fallo técnico
     */
    public ExcepcionAlmacenamientoGuardado(String detalle, Throwable causa) {
        super(detalle, causa);
    }
}
