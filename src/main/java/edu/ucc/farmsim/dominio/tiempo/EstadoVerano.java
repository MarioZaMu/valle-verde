package edu.ucc.farmsim.dominio.tiempo;

/**
 * Pattern: State — Role: ConcreteState (summer).
 *
 * <p>Stories: HU-09, HU-11.
 */
final class EstadoVerano implements EstadoEstacion {

    private static final EstrategiaClima CLIMA = new EstrategiaClimaEstacional(Estacion.SUMMER);

    static final EstadoVerano INSTANCIA = new EstadoVerano();

    private EstadoVerano() {
                // instancia compartida
    }

    @Override
    public Estacion estacion() {
        return Estacion.SUMMER;
    }

    @Override
    public EstrategiaClima estrategiaClima() {
        return CLIMA;
    }

    @Override
    public EstadoEstacion next() {
        return EstadoOtonio.INSTANCIA;
    }
}
