package edu.ucc.farmsim.ui.components;

/**
 * The kind of a notice (HU-39): it decides how long it stays and the symbol that goes with it, so the colour
 * is never the only cue (RNF-14).
 *
 * <p>Stories: HU-39.
 */
public enum ToastKind {
    /** Neutral information, such as the start of a season. */
    INFO(3_000, "ℹ"),
    /** Something worked. */
    SUCCESS(3_000, "✔"),
    /** An order was rejected; the text is the message of RN-35. */
    ERROR(3_000, "✖"),
    /** An achievement was unlocked: it stays longer. */
    ACHIEVEMENT(5_000, "★");

    private final int durationMs;
    private final String symbol;

    ToastKind(int durationMs, String symbol) {
        this.durationMs = durationMs;
        this.symbol = symbol;
    }

    /** Milliseconds fully visible, before the fade starts. */
    public int durationMs() {
        return durationMs;
    }

    /** Symbol drawn before the text. */
    public String symbol() {
        return symbol;
    }
}
