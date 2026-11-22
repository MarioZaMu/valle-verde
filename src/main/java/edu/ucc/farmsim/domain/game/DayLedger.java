package edu.ucc.farmsim.domain.game;

/**
 * What the player earned and spent during the current day. It starts at zero every day, so the summary never
 * mixes days (HU-17 · CA-4).
 *
 * <p>Stories: HU-17.
 */
public final class DayLedger {

    private int income;
    private int expenses;

    /** m earned today. */
    public int income() {
        return income;
    }

    /** m spent today. */
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
