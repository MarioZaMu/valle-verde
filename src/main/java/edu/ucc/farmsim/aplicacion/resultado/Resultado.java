package edu.ucc.farmsim.aplicacion.resultado;

/**
 * Resultado de una operación: un valor, o un código de error con un mensaje en español. El controlador devuelve esto en
 * lugar de lanzar excepciones, de modo que ninguna excepción llega nunca a la UI (R-03, RNF-07).
 *
 * <p>Stories: HU-28.
 *
 * @param <T> tipo del valor de un resultado exitoso
 */
public final class Resultado<T> {

    private final boolean ok;
    private final T value;
    private final CodigoError codigo;
    private final String mensaje;

    private Resultado(boolean ok, T value, CodigoError codigo, String mensaje) {
        this.ok = ok;
        this.value = value;
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    /** Un resultado exitoso que lleva un valor. */
    public static <T> Resultado<T> ok(T value) {
        return new Resultado<>(true, value, null, null);
    }

    /** Un rechazo con su código y un mensaje ya formateado. */
    public static <T> Resultado<T> fallar(CodigoError codigo, String mensaje) {
        return new Resultado<>(false, null, codigo, mensaje);
    }

    /** Un rechazo cuyo mensaje es la plantilla de RN-35 del código rellenada con los argumentos dados. */
    public static <T> Resultado<T> fallarCon(CodigoError codigo, Object... args) {
        return fallar(codigo, Mensajes.of(codigo, args));
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
            throw new IllegalStateException("Failed result has no value: " + codigo);
        }
        return value;
    }

    /** Código de error de un fallo, o {@code null} si fue un éxito. */
    public CodigoError codigo() {
        return codigo;
    }

    /** Mensaje en español de un fallo, o {@code null} si fue un éxito. */
    public String mensaje() {
        return mensaje;
    }
}
