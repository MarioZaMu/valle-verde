package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: Factory Method — Role: Creator (declares the factory method that builds a crop).
 *
 * <p>Sembrar solo conoce esta clase: cada creador concreto decide qué tipo de cultivo construir, de modo que un cultivo
 * nuevo no obliga a cambiar el código que siembra.
 *
 * <p>Stories: HU-01.
 */
public abstract class FabricaCultivo {

    /** El método fábrica: construye un cultivo nuevo en la etapa Semilla. */
    public abstract Cultivo cultivoCrear();

    /** Devuelve el creador de un tipo de cultivo. */
    public static FabricaCultivo paraTipo(TipoCultivo tipo) {
        return switch (tipo) {
            case CARROT -> FabricaZanahoria.INSTANCIA;
            case POTATO -> FabricaPapa.INSTANCIA;
            case TOMATO -> FabricaTomate.INSTANCIA;
            case CORN -> FabricaMaiz.INSTANCIA;
            case PUMPKIN -> FabricaCalabaza.INSTANCIA;
        };
    }
}
