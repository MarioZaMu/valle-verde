package edu.ucc.farmsim.application.dto;

/**
 * Lo que muestra la barra superior de la pantalla: dinero, fecha, clima y energía. Inmutable. La estación y el clima
 * viajan como ids (por ejemplo {@code "SPRING"}, {@code "SUNNY"}); la UI los convierte en texto en español.
 *
 * <p>Stories: HU-28, HU-34.
 *
 * @param money     dinero en monedas
 * @param year      año, desde 1
 * @param season    id de la estación
 * @param day       día de la estación, de 1 a 28
 * @param weather   id del clima de hoy
 * @param forecast  id del pronóstico de mañana
 * @param energy    energía disponible
 * @param maxEnergy energía máxima
 */
public record HudView(int money, int year, String season, int day,
                      String weather, String forecast, int energy, int maxEnergy) { }
