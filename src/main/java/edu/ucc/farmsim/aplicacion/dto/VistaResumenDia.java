package edu.ucc.farmsim.aplicacion.dto;

import java.util.List;

/**
 * Lo que pasó durante el día que acaba de terminar (HU-17).
 *
 * <p>Stories: HU-17.
 *
 * @param anio          año del día que terminó
 * @param estacion        id de la estación del día que terminó
 * @param dia           día de la estación que terminó
 * @param ingresos        m ganados
 * @param gastos      m gastados
 * @param antesSaldo dinero al inicio del día
 * @param despuesSaldo  dinero al final del día
 * @param luces    Spanish lines about what happened; «Sin novedades» if nothing did
 */
public record VistaResumenDia(int anio, String estacion, int dia, int ingresos, int gastos, int antesSaldo,
                             int despuesSaldo, List<String> luces) { }
