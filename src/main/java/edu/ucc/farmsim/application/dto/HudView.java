package edu.ucc.farmsim.application.dto;

/**
 * What the top bar of the screen shows: money, date, weather and energy. Immutable.
 * The season and the weather travel as ids (for example {@code "SPRING"}, {@code "SUNNY"}); the UI
 * turns them into Spanish text.
 *
 * <p>Stories: HU-28, HU-34.
 *
 * @param money     money in coins
 * @param year      year, from 1
 * @param season    season id
 * @param day       day of the season, 1 to 28
 * @param weather   id of today's weather
 * @param forecast  id of tomorrow's forecast
 * @param energy    energy available
 * @param maxEnergy maximum energy
 */
public record HudView(int money, int year, String season, int day,
                      String weather, String forecast, int energy, int maxEnergy) { }
