package edu.ucc.farmsim.dominio.juego;

import edu.ucc.farmsim.dominio.progreso.Logro;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hechos de un final de día, llenados por los pasos y leídos por la capa de aplicación.
 *
 * <p>Stories: HU-05, HU-10, HU-17, HU-21, HU-22, HU-26.
 */
public final class ReporteDia {

    private final List<FaseDia> ejecutarFases = new ArrayList<>();
    private FechaJuego fechaAnterior;
    private FechaJuego fechaNueva;
    private Clima climaHoy;
    private Clima pronostico;
    private int cultivosMarchitos;
    private int animalesEnfermos;
    private int productos;
    private final List<Logro> desbloqueado = new ArrayList<>();
    private int antesSaldo;
    private int despuesSaldo;
    private int ingresos;
    private int gastos;

    /** Las fases que corrieron, en orden de ejecución. */
    public List<FaseDia> ejecutarFases() {
        return Collections.unmodifiableList(ejecutarFases);
    }

    /** Fecha antes de que avanzara el calendario, o {@code null} si no corrió el paso del calendario. */
    public FechaJuego fechaAnterior() {
        return fechaAnterior;
    }

    /** Fecha después de que avanzó el calendario, o {@code null} si no corrió el paso del calendario. */
    public FechaJuego fechaNueva() {
        return fechaNueva;
    }

    /** Clima del día nuevo, o {@code null} si no corrió el paso del pronóstico. */
    public Clima climaHoy() {
        return climaHoy;
    }

    /** Pronóstico sorteado para el día siguiente, o {@code null} si no corrió el paso del pronóstico. */
    public Clima pronostico() {
        return pronostico;
    }

    /** Número de cultivos que se marchitaron durante este final de día. */
    public int cultivosMarchitos() {
        return cultivosMarchitos;
    }

    /** Logros desbloqueados durante este final de día (paso 8). */
    public List<Logro> desbloqueado() {
        return Collections.unmodifiableList(desbloqueado);
    }

    void agregarDesbloqueado(List<Logro> logros) {
        desbloqueado.addAll(logros);
    }

    /** Animales que se enfermaron durante este final de día. */
    public int animalesEnfermos() {
        return animalesEnfermos;
    }

    /** Unidades de producto que hicieron los animales durante este final de día. */
    public int productos() {
        return productos;
    }

    /** Dinero al inicio del día que terminó. */
    public int antesSaldo() {
        return antesSaldo;
    }

    /** Dinero al final del día que terminó. */
    public int despuesSaldo() {
        return despuesSaldo;
    }

    /** m ganados durante el día que terminó. */
    public int ingresos() {
        return ingresos;
    }

    /** m gastados durante el día que terminó. */
    public int gastos() {
        return gastos;
    }

    void animalAgregarEnfermo() {
        animalesEnfermos++;
    }

    void productosAgregar(int unidades) {
        productos += unidades;
    }

    void dineroRegistro(int despues, int ganado, int gastado) {
        this.antesSaldo = despues - ganado + gastado;
        this.despuesSaldo = despues;
        this.ingresos = ganado;
        this.gastos = gastado;
    }

    void agregarMarchito(int count) {
        cultivosMarchitos += count;
    }

    void ejecutarMarca(FaseDia fase) {
        ejecutarFases.add(fase);
    }

    void calendarioRegistro(FechaJuego anterior, FechaJuego actual) {
        this.fechaAnterior = anterior;
        this.fechaNueva = actual;
    }

    void climaRegistro(Clima hoy, Clima manana) {
        this.climaHoy = hoy;
        this.pronostico = manana;
    }
}
