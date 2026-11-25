package edu.ucc.farmsim.aplicacion.dto;

/**
 * Una fila de la lista de ranuras (RN-31): lo que contiene la ranura.
 *
 * <p>Stories: HU-29, HU-32.
 *
 * @param ranura    número de ranura, desde 1
 * @param status  {@code EMPTY}, {@code OK} or {@code DAMAGED}
 * @param anio    año de la partida guardada, 0 si la ranura no es utilizable
 * @param estacion  id de la estación de la partida guardada, o {@code null}
 * @param dia     día de la estación de la partida guardada, 0 si la ranura no es utilizable
 * @param dinero   dinero de la partida guardada
 * @param archivadoEn fecha y hora locales del guardado como {@code yyyy-MM-dd HH:mm}, o {@code null}
 */
public record VistaRanuraGuardado(int ranura, String status, int anio, String estacion, int dia, int dinero, String archivadoEn) { }
