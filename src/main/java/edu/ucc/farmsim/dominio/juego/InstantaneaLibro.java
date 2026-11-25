package edu.ucc.farmsim.dominio.juego;

/**
 * Copia inmutable de lo que se ganó y se gastó hoy, para el memento.
 *
 * <p>Stories: HU-17.
 *
 * @param ingresos   m ganados hasta ahora hoy
 * @param gastos m gastados hasta ahora hoy
 */
public record InstantaneaLibro(int ingresos, int gastos) { }
