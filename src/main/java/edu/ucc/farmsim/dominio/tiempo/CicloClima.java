package edu.ucc.farmsim.dominio.tiempo;

import java.util.Random;

/**
 * El clima de hoy y el pronóstico de mañana (RN-13).
 *
 * <p>Una partida nueva empieza soleada con el pronóstico del día 2 ya sorteado. Cada final de día el pronóstico pasa a
 * ser el clima de hoy y se sortea uno nuevo para el día siguiente. El pronóstico siempre es fiable.
 *
 * <p>Stories: HU-11.
 */
public final class CicloClima {

    private Clima hoy;
    private Clima pronostico;
    private final EstrategiaClima anular;

    private CicloClima(Clima hoy, Clima pronostico, EstrategiaClima anular) {
        this.hoy = hoy;
        this.pronostico = pronostico;
        this.anular = anular;
    }

    /**
     * Empieza una partida nueva: el día 1 es soleado y el pronóstico del día 2 se sortea de la tabla de su estación.
     */
    public static CicloClima iniciarJuegoNuevo(RelojJuego reloj, Random rnd) {
        return iniciarJuegoNuevo(reloj, rnd, null);
    }

    /**
     * Empieza una partida nueva usando una estrategia dada para cada pronóstico (para las pruebas que necesitan un
     * clima conocido).
     *
     * @param anular estrategia que se usa en lugar de las tablas por estación, o {@code null} para las tablas
     *                 normales
     */
    public static CicloClima iniciarJuegoNuevo(RelojJuego reloj, Random rnd, EstrategiaClima anular) {
        CicloClima ciclo = new CicloClima(Clima.SUNNY, Clima.SUNNY, anular);
        ciclo.pronostico = ciclo.dibujarParaManana(reloj, rnd);
        return ciclo;
    }

    /**
     * Paso 6 del final del día (RN-03): llámalo después de que el calendario avanzó. El clima de hoy pasa a ser el
     * antiguo pronóstico y se sortea un pronóstico nuevo para el día siguiente.
     */
    public void avanzar(RelojJuego reloj, Random rnd) {
        this.hoy = pronostico;
        this.pronostico = dibujarParaManana(reloj, rnd);
    }

    /** Clima del día actual. */
    public Clima hoy() {
        return hoy;
    }

    /** Pronóstico para el día siguiente. */
    public Clima pronostico() {
        return pronostico;
    }

    /** Fija ambos valores (lo usan las pruebas y el memento al restaurar una partida). */
    public void restaurar(Clima hoyRestaurado, Clima pronosticoRestaurado) {
        this.hoy = hoyRestaurado;
        this.pronostico = pronosticoRestaurado;
    }

    private Clima dibujarParaManana(RelojJuego reloj, Random rnd) {
        FechaJuego manana = reloj.manana();
        EstrategiaClima estrategia = anular != null
                ? anular
                : EstadoEstacion.of(manana.estacion()).estrategiaClima();
        return estrategia.climaSiguiente(manana.dia(), rnd);
    }
}
