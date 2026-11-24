package edu.ucc.farmsim.application.dto;

/**
 * Una fila de la lista de ranuras (RN-31): lo que contiene la ranura.
 *
 * <p>Stories: HU-29, HU-32.
 *
 * @param slot    número de ranura, desde 1
 * @param status  {@code EMPTY}, {@code OK} or {@code DAMAGED}
 * @param year    año de la partida guardada, 0 si la ranura no es utilizable
 * @param season  id de la estación de la partida guardada, o {@code null}
 * @param day     día de la estación de la partida guardada, 0 si la ranura no es utilizable
 * @param money   dinero de la partida guardada
 * @param savedAt fecha y hora locales del guardado como {@code yyyy-MM-dd HH:mm}, o {@code null}
 */
public record SaveSlotView(int slot, String status, int year, String season, int day, int money, String savedAt) { }
