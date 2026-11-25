package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (carrot).
 *
 * <p>Stories: HU-01.
 */
final class FabricaZanahoria extends FabricaCultivo {

    static final FabricaZanahoria INSTANCIA = new FabricaZanahoria();

    private FabricaZanahoria() {
                // instancia compartida
    }

    @Override
    public Cultivo cultivoCrear() {
        return new Cultivo(TipoCultivo.CARROT);
    }
}
