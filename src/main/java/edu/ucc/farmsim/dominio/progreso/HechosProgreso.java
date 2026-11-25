package edu.ucc.farmsim.dominio.progreso;

/**
 * Lo que miran los logros: una copia simple de los contadores y de las cifras de la partida, de modo que el rastreador
 * nunca necesita el estado de la partida (sin ciclo entre {@code juego} y {@code progreso}, C-01).
 *
 * <p>Stories: HU-26.
 *
 * @param dinero          dinero que tiene el jugador
 * @param cosechas       cultivos cosechados hasta ahora (limpiar una parcela marchita no cuenta)
 * @param unidadesVendidos      unidades vendidas hasta ahora
 * @param edificiosConstruidos edificios construidos hasta ahora
 * @param animales        animales que viven ahora en la granja
 * @param animalesCurados   animales curados hasta ahora
 * @param anio           año actual
 */
public record HechosProgreso(int dinero, int cosechas, int unidadesVendidos, int edificiosConstruidos, int animales,
                            int animalesCurados, int anio) { }
