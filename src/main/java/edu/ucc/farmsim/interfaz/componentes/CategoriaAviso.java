package edu.ucc.farmsim.interfaz.componentes;

/**
 * El tipo de un aviso (HU-39): decide cuánto tiempo se queda y el símbolo que lo acompaña, de modo que el color nunca
 * sea la única señal (RNF-14).
 *
 * <p>Stories: HU-39.
 */
public enum CategoriaAviso {
    /** Información neutra, como el comienzo de una estación. */
    INFO(3_000, "ℹ"),
    /** Algo funcionó. */
    SUCCESS(3_000, "✔"),
    /** Se rechazó una orden; el texto es el mensaje de RN-35. */
    ERROR(3_000, "✖"),
    /** Se desbloqueó un logro: se queda más tiempo. */
    ACHIEVEMENT(5_000, "★");

    private final int msDuracion;
    private final String simbolo;

    CategoriaAviso(int msDuracion, String simbolo) {
        this.msDuracion = msDuracion;
        this.simbolo = simbolo;
    }

    /** Milisegundos totalmente visible, antes de que empiece el desvanecimiento. */
    public int msDuracion() {
        return msDuracion;
    }

    /** Símbolo dibujado antes del texto. */
    public String simbolo() {
        return simbolo;
    }
}
