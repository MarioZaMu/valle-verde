package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaAccionEdificio;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import org.junit.jupiter.api.Test;

/** HU-22: los animales sanos y alimentados producen y el jugador recoge (RN-02, RN-15, RN-26). */
class HU22ProduccionTest {

    @Test
    void HU22_ca1_aFedHenLaysOneEggAndCollectingMovesItToTheInventory() {
        // Given a healthy hen fed today
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.gallina(gallinero);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 1);
        aplicacion.controlador.alimentar(gallinero.id());

                // Cuando termina el día el gallinero tiene 1 huevo
        aplicacion.controlador.diaFin();
        assertEquals(1, gallinero.productosPendientes());

                // Y cuando el jugador recoge, el inventario gana 1 huevo y la energía baja 1
        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.collect(gallinero.id());
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(1, resultado.value().monto());
        assertEquals(1, aplicacion.estado().inventario().count(TipoArticulo.EGG));
        assertEquals(99, aplicacion.estado().energia().actual());
        assertEquals(0, gallinero.productosPendientes());
    }

    @Test
    void HU22_ca2_aSickHenProducesNothing_criticalTest() {
        // Given a sick hen
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.SICK, 3, false, 0);

                // Cuando termina el día
        aplicacion.controlador.diaFin();

                // Entonces el gallinero no tiene producto pendiente
        assertEquals(0, gallinero.productosPendientes());
    }

    @Test
    void HU22_ca2_aHungryHenThatDidNotEatProducesNothingEither() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.gallina(gallinero);

        aplicacion.controlador.diaFin();

        assertEquals(0, gallinero.productosPendientes());
    }

    @Test
    void HU22_ca3_aHenFedThreeDaysWithoutCollectingKeepsOnlyTwoEggs() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.gallina(gallinero);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 3);
        for (int dia = 0; dia < 3; dia++) {
            aplicacion.controlador.alimentar(gallinero.id());
            aplicacion.controlador.diaFin();
        }

        assertEquals(2, gallinero.productosPendientes());
    }

    @Test
    void HU22_ca3_aCowFollowsTheSameRuleAndGivesMilk() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio establo = DatosJuego.construir(aplicacion.estado(), TipoEdificio.BARN, 2, 7);
        DatosJuego.casa(establo, TipoAnimal.COW, CondicionAnimal.HEALTHY, 0, false, 0);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 3);
        for (int dia = 0; dia < 3; dia++) {
            aplicacion.controlador.alimentar(establo.id());
            aplicacion.controlador.diaFin();
        }

        assertEquals(2, establo.productosPendientes());
        aplicacion.controlador.collect(establo.id());
        assertEquals(2, aplicacion.estado().inventario().count(TipoArticulo.MILK));
    }

    @Test
    void HU22_ca4_sin_espacio_en_el_inventario_nada_mueve_y_nada_es_cobrado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.HEALTHY, 0, false, 2);
        DatosJuego.salvoInventarioRellenar(aplicacion.estado(), TipoArticulo.CARROT, 5);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.collect(gallinero.id());

        assertEquals(CodigoError.INVENTARIO_LLENO, resultado.codigo());
        assertEquals(2, gallinero.productosPendientes());
        assertEquals(100, aplicacion.estado().energia().actual());
    }

    @Test
    void HU22_ca4_con_nada_esperando_el_jugador_es_dicho_y_nada_es_cobrado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.gallina(gallinero);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.collect(gallinero.id());

        assertTrue(resultado.isOk());
        assertEquals(0, resultado.value().monto());
        assertEquals("No hay productos que recoger.", resultado.value().notificacion());
        assertEquals(100, aplicacion.estado().energia().actual());
    }

    @Test
    void HU22_sin_energia_recolectando_es_rechazado_y_los_productos_quedar() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.HEALTHY, 0, false, 1);
        aplicacion.estado().energia().gastarIntentar(100);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.collect(gallinero.id());

        assertEquals(CodigoError.SIN_ENERGIA, resultado.codigo());
        assertEquals(1, gallinero.productosPendientes());
    }
}
