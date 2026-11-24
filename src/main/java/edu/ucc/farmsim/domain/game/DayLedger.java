package edu.ucc.farmsim.domain.game;

/**
 * Lo que el jugador ganó y gastó durante el día en curso. Empieza en cero cada día, así que el resumen nunca mezcla
 * días (HU-17 · CA-4).
 *
 * <p>Stories: HU-17.
 */
public final class DayLedger {

    private int income;
    private int expenses;

    /** m ganados hoy. */
    public int income() {
        return income;
    }

    /** m gastados hoy. */
    public int expenses() {
        return expenses;
    }

    void addIncome(int amount) {
        income += amount;
    }

    void addExpense(int amount) {
        expenses += amount;
    }

    void reset() {
        income = 0;
        expenses = 0;
    }

    LedgerSnapshot snapshot() {
        return new LedgerSnapshot(income, expenses);
    }

    void restoreFrom(LedgerSnapshot snapshot) {
        income = snapshot.income();
        expenses = snapshot.expenses();
    }
}
