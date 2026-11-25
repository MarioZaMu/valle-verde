package edu.ucc.farmsim.dominio.cultivo;

import edu.ucc.farmsim.dominio.tiempo.Estacion;

/**
 * Pattern: State — Role: Context (a plant whose behaviour depends on its growth state).
 *
 * <p>Guarda lo que siguen RN-06 a RN-10 de una planta sembrada: días de crecimiento, días sin agua y si fue regada hoy.
 * Su etapa se deriva del progreso y se guarda como un objeto de estado.
 *
 * <p>Stories: HU-01, HU-02, HU-03, HU-05.
 */
public final class Cultivo {

    private final TipoCultivo tipo;
    private int diasCrecidos;
    private int diasSinAgua;
    private boolean regado;
    private EstadoCultivo estado;

    /** Crea un cultivo recién sembrado: etapa Semilla, sin crecimiento, sin regar (RN-06). */
    public Cultivo(TipoCultivo tipo) {
        this(tipo, 0, 0, false, false);
    }

    private Cultivo(TipoCultivo tipo, int diasCrecidos, int diasSinAgua, boolean regado, boolean marchito) {
        this.tipo = tipo;
        this.diasCrecidos = diasCrecidos;
        this.diasSinAgua = diasSinAgua;
        this.regado = regado;
        this.estado = EstadoCultivo.paraProgreso(tipo, diasCrecidos, marchito);
    }

    /** Reconstruye un cultivo desde una instantánea (memento, pruebas). */
    public static Cultivo desdeInstantanea(InstantaneaCultivo instantanea) {
        return new Cultivo(instantanea.tipo(), instantanea.diasCrecidos(), instantanea.diasSinAgua(),
                instantanea.regado(), instantanea.marchito());
    }

    /** Una copia inmutable de este cultivo. */
    public InstantaneaCultivo instantanea() {
        return new InstantaneaCultivo(tipo, diasCrecidos, diasSinAgua, regado, esMarchito());
    }

    /** Tipo de cultivo. */
    public TipoCultivo tipo() {
        return tipo;
    }

    /** Días que ha crecido el cultivo. */
    public int diasCrecidos() {
        return diasCrecidos;
    }

    /** Días consecutivos sin agua. */
    public int diasSinAgua() {
        return diasSinAgua;
    }

    /** Si fue regado hoy. */
    public boolean esRegado() {
        return regado;
    }

    /** Etapa de crecimiento actual. */
    public EtapaCultivo etapa() {
        return estado.etapa();
    }

    /** Verdadero salvo que el cultivo esté marchito. */
    public boolean esVivo() {
        return estado.esVivo();
    }

    /** Verdadero cuando el cultivo se puede cosechar. */
    public boolean esMaduro() {
        return estado.puedeCosechar();
    }

    /** Verdadero cuando el cultivo murió. */
    public boolean esMarchito() {
        return estado.etapa() == EtapaCultivo.WITHERED;
    }

    /**
     * Riega el cultivo por el resto del día (RN-07).
     *
     * @return {@code true} si fue regado; {@code false} si está marchito o ya fue regado hoy
     */
    public boolean agua() {
        if (!esVivo() || regado) {
            return false;
        }
        regado = true;
        return true;
    }

    /**
     * Resuelve el final del día de este cultivo (paso 2 de RN-03): el estado actual decide qué pasa y la marca de riego
     * se borra para el día siguiente.
     */
    public void terminarDeDia(int diasSecosAMarchitar) {
        estado.onFinDia(this, diasSecosAMarchitar);
        regado = false;
    }

    /**
     * Marchita el cultivo por una helada a la intemperie (RN-09-b), sea cual sea su etapa.
     *
     * @return {@code true} si se marchitó ahora; {@code false} si ya lo estaba
     */
    public boolean marchitarPorHelada() {
        if (!esVivo()) {
            return false;
        }
        marchitar();
        return true;
    }

    /**
     * Marchita el cultivo si la estación dada no es una de sus estaciones válidas (RN-09-c), sea cual sea su etapa.
     *
     * @return {@code true} si se marchitó ahora
     */
    public boolean marchitarSiFueraDeEstacion(Estacion estacion) {
        if (esVivo() && !tipo.estacionesValidos().contains(estacion)) {
            marchitar();
            return true;
        }
        return false;
    }

    void diaUnoCrecer() {
        diasCrecidos++;
        diasSinAgua = 0;
        estado = EstadoCultivo.paraProgreso(tipo, diasCrecidos, false);
    }

    void marchitar() {
        estado = EstadoMarchito.INSTANCIA;
    }

    void aguaFalta() {
        diasSinAgua++;
    }

    /** Días que aún faltan para madurar (0 cuando está maduro o marchito). */
    public int izquierdaDias() {
        return esVivo() ? Math.max(0, tipo.diasAMaduro() - diasCrecidos) : 0;
    }
}
