package edu.ucc.farmsim.application.result;

/**
 * Outcome of an operation: a value, or an error code with a Spanish message. The controller returns this
 * instead of throwing, so no exception ever reaches the UI (R-03, RNF-07).
 *
 * <p>Stories: HU-28.
 *
 * @param <T> type of the value of a successful result
 */
public final class Result<T> {

    private final boolean ok;
    private final T value;
    private final ErrorCode code;
    private final String message;

    private Result(boolean ok, T value, ErrorCode code, String message) {
        this.ok = ok;
        this.value = value;
        this.code = code;
        this.message = message;
    }

    /** A successful result carrying a value. */
    public static <T> Result<T> ok(T value) {
        return new Result<>(true, value, null, null);
    }

    /** A rejection with its code and an already formatted message. */
    public static <T> Result<T> fail(ErrorCode code, String message) {
        return new Result<>(false, null, code, message);
    }

    /** A rejection whose message is the RN-35 template of the code filled in with the given arguments. */
    public static <T> Result<T> failWith(ErrorCode code, Object... args) {
        return fail(code, Messages.of(code, args));
    }

    /** True if the operation succeeded. */
    public boolean isOk() {
        return ok;
    }

    /**
     * The value of a successful result.
     *
     * @throws IllegalStateException if the result is a failure
     */
    public T value() {
        if (!ok) {
            throw new IllegalStateException("Failed result has no value: " + code);
        }
        return value;
    }

    /** Error code of a failure, or {@code null} for a success. */
    public ErrorCode code() {
        return code;
    }

    /** Spanish message of a failure, or {@code null} for a success. */
    public String message() {
        return message;
    }
}
