package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * What happened during the day that just ended (HU-17).
 *
 * <p>Stories: HU-17.
 *
 * @param year          year of the day that ended
 * @param season        season id of the day that ended
 * @param day           day of the season that ended
 * @param income        m earned
 * @param expenses      m spent
 * @param balanceBefore money at the start of the day
 * @param balanceAfter  money at the end of the day
 * @param highlights    Spanish lines about what happened; «Sin novedades» if nothing did
 */
public record DaySummaryView(int year, String season, int day, int income, int expenses, int balanceBefore,
                             int balanceAfter, List<String> highlights) { }
