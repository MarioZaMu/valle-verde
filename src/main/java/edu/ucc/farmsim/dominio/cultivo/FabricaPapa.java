package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (potato).
 *
 * <p>Stories: HU-01.
 */
final class FabricaPapa extends FabricaCultivo {

    static final FabricaPapa INSTANCIA = new FabricaPapa();

    private FabricaPapa() {
                // instancia compartida
    }

    @Override
    public Cultivo cultivoCrear() {
        return new Cultivo(TipoCultivo.POTATO);
    }
}
