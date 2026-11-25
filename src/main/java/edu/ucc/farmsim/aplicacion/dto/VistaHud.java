package edu.ucc.farmsim.aplicacion.dto;

/**
 * Lo que muestra la barra superior de la pantalla: dinero, fecha, clima y energía. Inmutable. La estación y el clima
 * viajan como ids (por ejemplo {@code "SPRING"}, {@code "SUNNY"}); la UI los convierte en texto en español.
 *
 * <p>Stories: HU-28, HU-34.
 *
 * @param dinero     dinero en monedas
 * @param anio      año, desde 1
 * @param estacion    id de la estación
 * @param dia       día de la estación, de 1 a 28
 * @param clima   id del clima de hoy
 * @param pronostico  id del pronóstico de mañana
 * @param energia    energía disponible
 * @param energiaMaxima energía máxima
 */
public record VistaHud(int dinero, int anio, String estacion, int dia,
                      String clima, String pronostico, int energia, int energiaMaxima) { }
