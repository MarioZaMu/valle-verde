package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * Lo que pasó durante el día que acaba de terminar (HU-17).
 *
 * <p>Stories: HU-17.
 *
 * @param year          año del día que terminó
 * @param season        id de la estación del día que terminó
 * @param day           día de la estación que terminó
 * @param income        m ganados
 * @param expenses      m gastados
 * @param balanceBefore dinero al inicio del día
 * @param balanceAfter  dinero al final del día
 * @param highlights    Spanish lines about what happened; «Sin novedades» if nothing did
 */
public record DaySummaryView(int year, String season, int day, int income, int expenses, int balanceBefore,
                             int balanceAfter, List<String> highlights) { }
