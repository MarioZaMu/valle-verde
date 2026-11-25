package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.juego.ValidadorEstadoJuego;
import edu.ucc.farmsim.dominio.granja.Terreno;
import org.junit.jupiter.api.Test;

/** HU-28: la granja de demostración arranca con edificios, animales y cultivos ya puestos para mostrar el juego. */
class HU28GranjaDemostracionTest {

    @Test
    void HU28_ca4_demoFarmStartsWithBuildingsAnimalsAndCrops() {
        AplicacionTest aplicacion = AplicacionTest.create();

        Resultado<VistaHud> resultado = aplicacion.controlador.juegoDemostracion(AplicacionTest.SEED);

        assertTrue(resultado.isOk());
        EstadoJuego estado = aplicacion.sesion.actual();
        for (TipoEdificio tipo : TipoEdificio.values()) {
            assertEquals(1, estado.granja().contarEdificios(tipo), "falta " + tipo);
        }
        assertEquals(4, estado.hechos().animales());
        assertTrue(estado.granja().count(Terreno.SOIL) > 0);
        assertTrue(estado.inventario().ranurasUsados() > 2);
    }

    @Test
    void HU28_ca4_demoFarmIsAValidStateThatCanBeSavedAndRestored() {
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoDemostracion(AplicacionTest.SEED);

        EstadoJuego estado = aplicacion.sesion.actual();

        assertEquals(java.util.List.of(), ValidadorEstadoJuego.violaciones(estado.mementoCrear()));
        assertEquals(estado.mementoCrear(), EstadoJuego.desdeMemento(estado.mementoCrear()).mementoCrear());
    }
}
