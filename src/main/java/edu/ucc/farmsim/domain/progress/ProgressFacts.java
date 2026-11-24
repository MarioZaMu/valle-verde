package edu.ucc.farmsim.domain.progress;

/**
 * Lo que miran los logros: una copia simple de los contadores y de las cifras de la partida, de modo que el rastreador
 * nunca necesita el estado de la partida (sin ciclo entre {@code game} y {@code progress}, C-01).
 *
 * <p>Stories: HU-26.
 *
 * @param money          dinero que tiene el jugador
 * @param harvests       cultivos cosechados hasta ahora (limpiar una parcela marchita no cuenta)
 * @param unitsSold      unidades vendidas hasta ahora
 * @param buildingsBuilt edificios construidos hasta ahora
 * @param animals        animales que viven ahora en la granja
 * @param animalsCured   animales curados hasta ahora
 * @param year           año actual
 */
public record ProgressFacts(int money, int harvests, int unitsSold, int buildingsBuilt, int animals,
                            int animalsCured, int year) { }
