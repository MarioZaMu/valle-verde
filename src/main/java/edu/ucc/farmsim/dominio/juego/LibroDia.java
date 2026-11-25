package edu.ucc.farmsim.dominio.juego;

/**
 * Lo que el jugador ganó y gastó durante el día en curso. Empieza en cero cada día, así que el resumen nunca mezcla
 * días (HU-17 · CA-4).
 *
 * <p>Stories: HU-17.
 */
public final class LibroDia {

    private int ingresos;
    private int gastos;

    /** m ganados hoy. */
    public int ingresos() {
        return ingresos;
    }

    /** m gastados hoy. */
    public int gastos() {
        return gastos;
    }

    void ingresosAgregar(int monto) {
        ingresos += monto;
    }

    void gastoAgregar(int monto) {
        gastos += monto;
    }

    void reiniciar() {
        ingresos = 0;
        gastos = 0;
    }

    InstantaneaLibro instantanea() {
        return new InstantaneaLibro(ingresos, gastos);
    }

    void restaurarDesde(InstantaneaLibro instantanea) {
        ingresos = instantanea.ingresos();
        gastos = instantanea.gastos();
    }
}
