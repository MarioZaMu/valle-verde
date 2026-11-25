package edu.ucc.farmsim.dominio.progreso;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import java.util.function.Predicate;

/**
 * Los logros de RN-28 con su recompensa y la condición que concede cada uno. Algunos desbloquean edificios (RN-29).
 *
 * <p>Stories: HU-18, HU-26.
 */
public enum Logro {
    FIRST_HARVEST("LG-01", ReglasJuego.COSECHAR_RECOMPENSA_PRIMERO, f -> f.cosechas() >= 1),
    GREEN_THUMB("LG-02", ReglasJuego.MINIATURA_RECOMPENSA_VERDE, f -> f.cosechas() >= ReglasJuego.MINIATURA_COSECHAS_VERDE),
    BIG_HARVESTER("LG-03", ReglasJuego.COSECHADOR_RECOMPENSA_GRANDE, f -> f.cosechas() >= ReglasJuego.COSECHADOR_COSECHAS_GRANDE),
    TRADER("LG-04", ReglasJuego.COMERCIANTE_RECOMPENSA, f -> f.unidadesVendidos() >= ReglasJuego.COMERCIANTE_UNIDADES_VENDIDO),
    CAPITALIST("LG-05", ReglasJuego.RECOMPENSA_CAPITALISTA, f -> f.dinero() >= ReglasJuego.SALDO_CAPITALISTA),
    BUILDER("LG-06", ReglasJuego.CONSTRUCTOR_RECOMPENSA, f -> f.edificiosConstruidos() >= 1),
    BARNYARD_FARMER("LG-07", ReglasJuego.GRANJERO_CORRAL_RECOMPENSA,
            f -> f.animales() >= ReglasJuego.GRANJERO_CORRAL_ANIMALES),
    VETERINARIAN("LG-08", ReglasJuego.VETERINARIA_RECOMPENSA, f -> f.animalesCurados() >= 1),
    ONE_YEAR_IN_THE_VALLEY("LG-09", ReglasJuego.ANIO_UNO_RECOMPENSA_EN_VALLE_EL,
            f -> f.anio() >= ReglasJuego.ANIO_UNO_ANIO_EN_VALLE_EL);

    private final String codigo;
    private final int recompensa;
    private final Predicate<HechosProgreso> condicion;

    Logro(String codigo, int recompensa, Predicate<HechosProgreso> condicion) {
        this.codigo = codigo;
        this.recompensa = recompensa;
        this.condicion = condicion;
    }

    /** El identificador de RN-28, por ejemplo {@code LG-02}. */
    public String codigo() {
        return codigo;
    }

    /** Dinero en m que se paga al desbloquear el logro. */
    public int recompensa() {
        return recompensa;
    }

    /** Verdadero si los hechos cumplen la condición de este logro. */
    public boolean esCumplidoPor(HechosProgreso hechos) {
        return condicion.test(hechos);
    }
}
