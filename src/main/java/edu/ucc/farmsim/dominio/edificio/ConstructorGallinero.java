package edu.ucc.farmsim.dominio.edificio;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;

/**
 * Pattern: Builder — Role: ConcreteBuilder (coop: four hens).
 *
 * <p>Stories: HU-18.
 */
final class ConstructorGallinero extends ConstructorEdificio {

    private int capacidad;
    private int ranurasExtra;
    private int parcelas;

    @Override
    TipoEdificio tipo() {
        return TipoEdificio.COOP;
    }

    @Override
    void alojamientoDefinir() {
        capacidad = ReglasJuego.ANIMALES_POR_EDIFICIO;
    }

    @Override
    void efectosDefinir() {
        ranurasExtra = 0;
        parcelas = 0;
    }

    @Override
    int capacidadAnimal() {
        return capacidad;
    }

    @Override
    int ranurasInventarioExtra() {
        return ranurasExtra;
    }

    @Override
    int parcelasProtegidos() {
        return parcelas;
    }
}
