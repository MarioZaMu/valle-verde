package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (corn).
 *
 * <p>Stories: HU-01.
 */
final class FabricaMaiz extends FabricaCultivo {

    static final FabricaMaiz INSTANCIA = new FabricaMaiz();

    private FabricaMaiz() {
                // instancia compartida
    }

    @Override
    public Cultivo cultivoCrear() {
        return new Cultivo(TipoCultivo.CORN);
    }
}
