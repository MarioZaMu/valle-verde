package edu.ucc.farmsim.dominio.juego;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ejecuta el final del día como una lista ordenada de {@link PasoDia} (RN-03).
 *
 * <p>Los pasos se pueden registrar en cualquier orden: el pipeline siempre los ejecuta ordenados por {@link FaseDia},
 * de modo que quien agrega un paso no puede romper el orden de RN-03. Cada historia agrega su propio paso.
 *
 * <p>También cierra el libro diario: el dinero del día se registra en el informe y los contadores vuelven a cero para
 * el día siguiente (HU-17).
 *
 * <p>Stories: HU-03, HU-10, HU-17.
 */
public final class SecuenciaDia {

    private final List<PasoDia> pasos = new ArrayList<>();

    /** El pipeline con todos los pasos implementados hasta ahora. */
    public static SecuenciaDia estandar() {
        return new SecuenciaDia()
                .add(new PasoEfectosClima())
                .add(new PasoCrecimiento())
                .add(new PasoAnimales())
                .add(new PasoMercado())
                .add(new PasoCalendario())
                .add(new PasoPronostico())
                .add(new PasoReiniciarEnergia())
                .add(new PasoLogros());
    }

    /**
     * Registra un paso.
     *
     * @return este pipeline, para encadenar llamadas
     * @throws IllegalArgumentException si el paso es {@code null}
     */
    public SecuenciaDia add(PasoDia paso) {
        if (paso == null) {
            throw new IllegalArgumentException("step is required");
        }
        pasos.add(paso);
        return this;
    }

    /**
     * Ejecuta todos los pasos en orden de fase sobre la partida dada.
     *
     * @return los hechos registrados por los pasos
     */
    public ReporteDia run(EstadoJuego estado) {
        List<PasoDia> ordenado = new ArrayList<>(pasos);
        ordenado.sort(Comparator.comparing(PasoDia::fase));
        ReporteDia reporte = new ReporteDia();
        for (PasoDia paso : ordenado) {
            paso.apply(estado, reporte);
            reporte.ejecutarMarca(paso.fase());
        }
        reporte.dineroRegistro(estado.dinero(), estado.libro().ingresos(), estado.libro().gastos());
        estado.libro().reiniciar();
        return reporte;
    }
}
