package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (tomato).
 *
 * <p>Stories: HU-01.
 */
final class FabricaTomate extends FabricaCultivo {

    static final FabricaTomate INSTANCIA = new FabricaTomate();

    private FabricaTomate() {
                // instancia compartida
    }

    @Override
    public Cultivo cultivoCrear() {
        return new Cultivo(TipoCultivo.TOMATO);
    }
}
