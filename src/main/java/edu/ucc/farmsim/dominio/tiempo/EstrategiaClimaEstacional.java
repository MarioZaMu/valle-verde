package edu.ucc.farmsim.dominio.tiempo;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import java.util.Random;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (seasonal probability table).
 *
 * <p>Sortea el clima a partir de la tabla de porcentajes de una estación (RN-11); en otoño la tabla cambia el día
 * {@link ReglasJuego#HELADA_OTONIO_DESDE_DIA}, cuando la helada se vuelve posible.
 *
 * <p>Stories: HU-11.
 */
public final class EstrategiaClimaEstacional implements EstrategiaClima {

    private final Estacion estacion;

    /**
     * Crea la estrategia de una estación.
     *
     * @param estacion estación cuya tabla se usa
     */
    public EstrategiaClimaEstacional(Estacion estacion) {
        this.estacion = estacion;
    }

    @Override
    public Clima climaSiguiente(int dia, Random rnd) {
        int[] pesos = pesos(dia);
        int rodar = rnd.nextInt(ReglasJuego.TOTAL_TABLA_CLIMA);
        int acumulado = 0;
        for (int i = 0; i < pesos.length; i++) {
            acumulado += pesos[i];
            if (rodar < acumulado) {
                return Clima.values()[i];
            }
        }
        throw new IllegalStateException("Weather table does not add up to " + ReglasJuego.TOTAL_TABLA_CLIMA);
    }

    private int[] pesos(int dia) {
        return switch (estacion) {
            case SPRING -> ReglasJuego.PRIMAVERA_PESOS_CLIMA;
            case SUMMER -> ReglasJuego.VERANO_PESOS_CLIMA;
            case AUTUMN -> dia >= ReglasJuego.HELADA_OTONIO_DESDE_DIA
                    ? ReglasJuego.TARDE_OTONIO_PESOS_CLIMA
                    : ReglasJuego.OTONIO_PESOS_CLIMA_TEMPRANO;
            case WINTER -> ReglasJuego.INVIERNO_PESOS_CLIMA;
        };
    }
}
