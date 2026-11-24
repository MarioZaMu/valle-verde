package edu.ucc.farmsim.domain.game;

/**
 * Copia inmutable de lo que se ganó y se gastó hoy, para el memento.
 *
 * <p>Stories: HU-17.
 *
 * @param income   m ganados hasta ahora hoy
 * @param expenses m gastados hasta ahora hoy
 */
public record LedgerSnapshot(int income, int expenses) { }
