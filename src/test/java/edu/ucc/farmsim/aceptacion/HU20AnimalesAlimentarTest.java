package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaAccionEdificio;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.animal.Animal;
import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import org.junit.jupiter.api.Test;

/** HU-20: alimentar a todos los animales de un edificio a la vez (RN-02, RN-15, RN-24). */
class HU20AnimalesAlimentarTest {

    private static Edificio gallineroConGallinas(AplicacionTest aplicacion, int sano, int enfermo) {
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        for (int i = 0; i < sano; i++) {
            DatosJuego.gallina(gallinero);
        }
        for (int i = 0; i < enfermo; i++) {
            DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.SICK, 3, false, 0);
        }
        return gallinero;
    }

    @Test
    void HU20_ca1_alimentacion_tres_gallinas_cuesta_tres_heno_y_tres_energia() {
                // Dado un gallinero con 3 gallinas sanas que no han comido, 5 de heno y 100 de energía
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = gallineroConGallinas(aplicacion, 3, 0);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 5);

                // Cuando el jugador alimenta el edificio
        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.alimentar(gallinero.id());

                // Entonces quedan 2 de heno y 97 de energía y las 3 gallinas comieron hoy
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(3, resultado.value().monto());
        assertNull(resultado.value().notificacion());
        assertEquals(2, aplicacion.estado().inventario().count(TipoArticulo.HAY));
        assertEquals(97, aplicacion.estado().energia().actual());
        assertTrue(gallinero.animales().stream().allMatch(Animal::esHoyAlimentado));
    }

    @Test
    void HU20_ca2_no_suficiente_heno_rechaza_todo_y_consume_nada() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = gallineroConGallinas(aplicacion, 3, 0);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 2);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.alimentar(gallinero.id());

        assertEquals(CodigoError.SIN_HENO, resultado.codigo());
        assertEquals("Heno insuficiente: necesitas 3.", resultado.mensaje());
        assertEquals(2, aplicacion.estado().inventario().count(TipoArticulo.HAY));
        assertEquals(100, aplicacion.estado().energia().actual());
        assertTrue(gallinero.animales().stream().noneMatch(Animal::esHoyAlimentado));
    }

    @Test
    void HU20_ca3_no_suficiente_energia_rechaza_todo_y_consume_nada() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = gallineroConGallinas(aplicacion, 3, 0);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 5);
        aplicacion.estado().energia().gastarIntentar(98);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.alimentar(gallinero.id());

        assertEquals(CodigoError.SIN_ENERGIA, resultado.codigo());
        assertEquals("Energía insuficiente: necesitas 3 y tienes 2.", resultado.mensaje());
        assertEquals(5, aplicacion.estado().inventario().count(TipoArticulo.HAY));
        assertEquals(2, aplicacion.estado().energia().actual());
        assertTrue(gallinero.animales().stream().noneMatch(Animal::esHoyAlimentado));
    }

    @Test
    void HU20_ca4_aSickHenIsSkippedAndThePlayerIsWarned() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = gallineroConGallinas(aplicacion, 2, 1);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 5);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.alimentar(gallinero.id());

        assertTrue(resultado.isOk());
        assertEquals(2, resultado.value().monto());
        assertEquals("Hay animales enfermos: cúralos con Medicina.", resultado.value().notificacion());
        assertEquals(3, aplicacion.estado().inventario().count(TipoArticulo.HAY));
        assertEquals(98, aplicacion.estado().energia().actual());
        assertEquals(2, gallinero.animales().stream().filter(Animal::esHoyAlimentado).count());
        assertEquals(CondicionAnimal.SICK, gallinero.animales().get(2).condicion());
    }

    @Test
    void HU20_ca5_si_todos_ya_comio_alli_es_nada_a_alimentar_y_nada_es_consumido() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = gallineroConGallinas(aplicacion, 2, 0);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 5);
        aplicacion.controlador.alimentar(gallinero.id());

        Resultado<VistaAccionEdificio> otraVez = aplicacion.controlador.alimentar(gallinero.id());

        assertTrue(otraVez.isOk());
        assertEquals(0, otraVez.value().monto());
        assertEquals("No hay animales por alimentar.", otraVez.value().notificacion());
        assertEquals(3, aplicacion.estado().inventario().count(TipoArticulo.HAY));
        assertEquals(98, aplicacion.estado().energia().actual());
    }

    @Test
    void HU20_aBuildingWithoutAnimalsHasNothingToFeed() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.alimentar(gallinero.id());

        assertEquals(0, resultado.value().monto());
        assertEquals("No hay animales por alimentar.", resultado.value().notificacion());
    }
}
