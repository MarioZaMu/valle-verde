package edu.ucc.farmsim.dominio.edificio;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;

/**
 * Pattern: Builder — Role: ConcreteBuilder (warehouse: twelve more inventory slots).
 *
 * <p>Stories: HU-18.
 */
final class ConstructorAlmacen extends ConstructorEdificio {

    private int capacidad;
    private int ranurasExtra;
    private int parcelas;

    @Override
    TipoEdificio tipo() {
        return TipoEdificio.WAREHOUSE;
    }

    @Override
    void alojamientoDefinir() {
        capacidad = 0;
    }

    @Override
    void efectosDefinir() {
        ranurasExtra = ReglasJuego.RANURAS_EXTRA_ALMACEN;
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
