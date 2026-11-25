package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** HU-23: curar a un animal enfermo con medicina (RN-02, RN-16, RN-27). */
class HU23AnimalCurarTest {

    private static Animal gallinaEnferma(AplicacionTest aplicacion, int medicinas) {
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        Animal gallina = DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.SICK, 3, false, 0);
        if (medicinas > 0) {
            aplicacion.estado().inventario().add(TipoArticulo.MEDICINE, medicinas);
        }
        return gallina;
    }

    @Test
    void HU23_ca1_aMedicineMakesTheHenHealthyAtOnceForOneEnergy() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Animal gallina = gallinaEnferma(aplicacion, 1);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.curar(0, 0);

        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(1, resultado.value().monto());
        assertEquals(CondicionAnimal.HEALTHY, gallina.condicion());
        assertEquals(0, gallina.diasSinAlimentar());
        assertEquals(0, aplicacion.estado().inventario().count(TipoArticulo.MEDICINE));
        assertEquals(99, aplicacion.estado().energia().actual());
    }

    @Test
    void HU23_ca2_sin_medicina_la_gallina_queda_enfermo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Animal gallina = gallinaEnferma(aplicacion, 0);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.curar(0, 0);

        assertEquals(CodigoError.SIN_MEDICINA, resultado.codigo());
        assertEquals("No tienes Medicina.", resultado.mensaje());
        assertEquals(CondicionAnimal.SICK, gallina.condicion());
        assertEquals(100, aplicacion.estado().energia().actual());
    }

    @Test
    void HU23_ca3_sin_energia_la_medicina_es_conservado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Animal gallina = gallinaEnferma(aplicacion, 1);
        aplicacion.estado().energia().gastarIntentar(100);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.curar(0, 0);

        assertEquals(CodigoError.SIN_ENERGIA, resultado.codigo());
        assertEquals(1, aplicacion.estado().inventario().count(TipoArticulo.MEDICINE));
        assertEquals(CondicionAnimal.SICK, gallina.condicion());
    }

    @Test
    void HU23_ca4_aCuredHenThatIsFedTheSameDayLaysAnEgg() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        gallinaEnferma(aplicacion, 1);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 1);
        aplicacion.controlador.curar(0, 0);
        aplicacion.controlador.alimentar(0);

        aplicacion.controlador.diaFin();

        assertEquals(1, aplicacion.estado().granja().edificios().get(0).productosPendientes());
    }

    @Test
    void HU23_curingAHealthyAnimalOnlyTellsThePlayer() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.gallina(gallinero);
        aplicacion.estado().inventario().add(TipoArticulo.MEDICINE, 1);

        Resultado<VistaAccionEdificio> resultado = aplicacion.controlador.curar(0, 0);

        assertTrue(resultado.isOk());
        assertEquals(0, resultado.value().monto());
        assertEquals("Este animal no está enfermo.", resultado.value().notificacion());
        assertEquals(1, aplicacion.estado().inventario().count(TipoArticulo.MEDICINE));
        assertEquals(100, aplicacion.estado().energia().actual());
    }

    @Test
    void HU23_un_animal_que_hace_no_existir_es_un_interno_error() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.gallinero(aplicacion.estado());

        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.curar(0, 3).codigo());
    }
}
