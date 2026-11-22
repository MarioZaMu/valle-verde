package edu.ucc.farmsim.domain.game;

/**
 * Immutable copy of what was earned and spent today, for the memento.
 *
 * <p>Stories: HU-17.
 *
 * @param income   m earned so far today
 * @param expenses m spent so far today
 */
public record LedgerSnapshot(int income, int expenses) { }
