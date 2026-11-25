package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (pumpkin).
 *
 * <p>Stories: HU-01.
 */
final class FabricaCalabaza extends FabricaCultivo {

    static final FabricaCalabaza INSTANCIA = new FabricaCalabaza();

    private FabricaCalabaza() {
                // instancia compartida
    }

    @Override
    public Cultivo cultivoCrear() {
        return new Cultivo(TipoCultivo.PUMPKIN);
    }
}
