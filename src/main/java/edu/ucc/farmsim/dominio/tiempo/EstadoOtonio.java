package edu.ucc.farmsim.dominio.tiempo;

/**
 * Pattern: State — Role: ConcreteState (autumn).
 *
 * <p>Stories: HU-09, HU-11.
 */
final class EstadoOtonio implements EstadoEstacion {

    private static final EstrategiaClima CLIMA = new EstrategiaClimaEstacional(Estacion.AUTUMN);

    static final EstadoOtonio INSTANCIA = new EstadoOtonio();

    private EstadoOtonio() {
                // instancia compartida
    }

    @Override
    public Estacion estacion() {
        return Estacion.AUTUMN;
    }

    @Override
    public EstrategiaClima estrategiaClima() {
        return CLIMA;
    }

    @Override
    public EstadoEstacion next() {
        return EstadoInvierno.INSTANCIA;
    }
}
