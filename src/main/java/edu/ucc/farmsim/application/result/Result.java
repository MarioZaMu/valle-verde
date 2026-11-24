package edu.ucc.farmsim.application.result;

/**
 * Resultado de una operación: un valor, o un código de error con un mensaje en español. El controlador devuelve esto en
 * lugar de lanzar excepciones, de modo que ninguna excepción llega nunca a la UI (R-03, RNF-07).
 *
 * <p>Stories: HU-28.
 *
 * @param <T> tipo del valor de un resultado exitoso
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

    /** Un resultado exitoso que lleva un valor. */
    public static <T> Result<T> ok(T value) {
        return new Result<>(true, value, null, null);
    }

    /** Un rechazo con su código y un mensaje ya formateado. */
    public static <T> Result<T> fail(ErrorCode code, String message) {
        return new Result<>(false, null, code, message);
    }

    /** Un rechazo cuyo mensaje es la plantilla de RN-35 del código rellenada con los argumentos dados. */
    public static <T> Result<T> failWith(ErrorCode code, Object... args) {
        return fail(code, Messages.of(code, args));
    }

    /** Verdadero si la operación tuvo éxito. */
    public boolean isOk() {
        return ok;
    }

    /**
     * El valor de un resultado exitoso.
     *
     * @throws IllegalStateException si el resultado es un fallo
     */
    public T value() {
        if (!ok) {
            throw new IllegalStateException("Failed result has no value: " + code);
        }
        return value;
    }

    /** Código de error de un fallo, o {@code null} si fue un éxito. */
    public ErrorCode code() {
        return code;
    }

    /** Mensaje en español de un fallo, o {@code null} si fue un éxito. */
    public String message() {
        return message;
    }
}
