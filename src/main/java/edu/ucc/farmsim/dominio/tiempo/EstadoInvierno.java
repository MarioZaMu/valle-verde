package edu.ucc.farmsim.dominio.tiempo;

/**
 * Pattern: State — Role: ConcreteState (winter).
 *
 * <p>Stories: HU-09, HU-11.
 */
final class EstadoInvierno implements EstadoEstacion {

    private static final EstrategiaClima CLIMA = new EstrategiaClimaEstacional(Estacion.WINTER);

    static final EstadoInvierno INSTANCIA = new EstadoInvierno();

    private EstadoInvierno() {
                // instancia compartida
    }

    @Override
    public Estacion estacion() {
        return Estacion.WINTER;
    }

    @Override
    public EstrategiaClima estrategiaClima() {
        return CLIMA;
    }

    @Override
    public EstadoEstacion next() {
        return EstadoPrimavera.INSTANCIA;
    }
}
