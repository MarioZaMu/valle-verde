package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: State — Role: State interface (what a crop can do at each growth stage).
 *
 * <p>Los estados no guardan datos y se comparten; un {@link Cultivo} guarda el actual y lo cambia a medida que crece, de
 * modo que ningún código necesita una cadena de {@code if} sobre la etapa.
 *
 * <p>Stories: HU-01, HU-03, HU-05.
 */
public interface EstadoCultivo {

    /** La etapa que representa este estado. */
    EtapaCultivo etapa();

    /** Verdadero salvo que el cultivo esté marchito. */
    boolean esVivo();

    /** Verdadero solo cuando el cultivo está maduro y se puede cosechar. */
    boolean puedeCosechar();

    /**
     * Lo que le pasa al cultivo cuando termina el día (paso 2 de RN-03): un cultivo joven vivo que fue regado crece;
     * uno que no lo fue cuenta un día sin agua y se marchita al llegar al límite. Un cultivo maduro o marchito no hace
     * nada.
     *
     * @param cultivo            el cultivo cuyo día termina
     * @param diasSecosAMarchitar días consecutivos sin agua que matan a un cultivo joven (3, o 2 en una sequía)
     */
    void onFinDia(Cultivo cultivo, int diasSecosAMarchitar);

    /**
     * El estado que corresponde al progreso de un cultivo (RN-08): Semilla con 0 días; Brote hasta la mitad de los días
     * para madurar; Crecimiento hasta completarlos; Maduro después; Marchito si murió.
     */
    static EstadoCultivo paraProgreso(TipoCultivo tipo, int diasCrecidos, boolean marchito) {
        if (marchito) {
            return EstadoMarchito.INSTANCIA;
        }
        if (diasCrecidos >= tipo.diasAMaduro()) {
            return EstadoMaduro.INSTANCIA;
        }
        if (diasCrecidos >= tipo.diasAMedias()) {
            return EstadoCreciendo.INSTANCIA;
        }
        if (diasCrecidos >= 1) {
            return EstadoBrote.INSTANCIA;
        }
        return EstadoSemilla.INSTANCIA;
    }
}
