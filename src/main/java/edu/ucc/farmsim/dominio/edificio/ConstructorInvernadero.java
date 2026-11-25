package edu.ucc.farmsim.dominio.edificio;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;

/**
 * Pattern: Builder — Role: ConcreteBuilder (greenhouse: nine protected plots).
 *
 * <p>Stories: HU-18.
 */
final class ConstructorInvernadero extends ConstructorEdificio {

    private int capacidad;
    private int ranurasExtra;
    private int parcelas;

    @Override
    TipoEdificio tipo() {
        return TipoEdificio.GREENHOUSE;
    }

    @Override
    void alojamientoDefinir() {
        capacidad = 0;
    }

    @Override
    void efectosDefinir() {
        ranurasExtra = 0;
        parcelas = ReglasJuego.PARCELAS_INVERNADERO_PROTEGIDOS;
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
