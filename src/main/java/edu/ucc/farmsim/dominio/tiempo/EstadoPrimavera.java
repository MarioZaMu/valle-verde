package edu.ucc.farmsim.dominio.tiempo;

/**
 * Pattern: State — Role: ConcreteState (spring).
 *
 * <p>Stories: HU-09, HU-11.
 */
final class EstadoPrimavera implements EstadoEstacion {

    private static final EstrategiaClima CLIMA = new EstrategiaClimaEstacional(Estacion.SPRING);

    static final EstadoPrimavera INSTANCIA = new EstadoPrimavera();

    private EstadoPrimavera() {
                // instancia compartida
    }

    @Override
    public Estacion estacion() {
        return Estacion.SPRING;
    }

    @Override
    public EstrategiaClima estrategiaClima() {
        return CLIMA;
    }

    @Override
    public EstadoEstacion next() {
        return EstadoVerano.INSTANCIA;
    }
}
