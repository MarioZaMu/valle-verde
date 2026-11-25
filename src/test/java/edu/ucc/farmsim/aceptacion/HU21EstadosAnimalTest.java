package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.animal.Animal;
import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import org.junit.jupiter.api.Test;

/** HU-21: los animales pasan hambre y se enferman cuando se los descuida (RN-03, RN-25). */
class HU21EstadosAnimalTest {

    @Test
    void HU21_ca1_aHenThatIsNeverFedIsHungryTwiceAndThenSick() {
                // Dada una gallina sana a la que no se alimenta
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Animal gallina = DatosJuego.gallina(DatosJuego.gallinero(aplicacion.estado()));

                // Cuando pasan 1, 2 y 3 días
        aplicacion.controlador.diaFin();
        assertEquals(CondicionAnimal.HUNGRY, gallina.condicion());
        assertEquals(1, gallina.diasSinAlimentar());
        aplicacion.controlador.diaFin();
        assertEquals(CondicionAnimal.HUNGRY, gallina.condicion());
        assertEquals(2, gallina.diasSinAlimentar());
        aplicacion.controlador.diaFin();

                // Entonces está hambrienta con 1, hambrienta con 2 y enferma con 3
        assertEquals(CondicionAnimal.SICK, gallina.condicion());
        assertEquals(3, gallina.diasSinAlimentar());
    }

    @Test
    void HU21_ca2_aHungryHenThatAteTodayIsHealthyAgainWhenTheDayEnds() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        Animal gallina = DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.HUNGRY, 2, false, 0);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 1);
        aplicacion.controlador.alimentar(gallinero.id());

        aplicacion.controlador.diaFin();

        assertEquals(CondicionAnimal.HEALTHY, gallina.condicion());
        assertEquals(0, gallina.diasSinAlimentar());
    }

    @Test
    void HU21_ca3_aSickHenStaysSickEatsNothingAndItsCounterDoesNotChange() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        Animal gallina = DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.SICK, 3, false, 0);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 2);

        aplicacion.controlador.alimentar(gallinero.id());
        aplicacion.controlador.diaFin();
        aplicacion.controlador.diaFin();

        assertEquals(CondicionAnimal.SICK, gallina.condicion());
        assertEquals(3, gallina.diasSinAlimentar());
        assertEquals(2, aplicacion.estado().inventario().count(TipoArticulo.HAY));
    }

    @Test
    void HU21_ca4_theDailyMarkIsClearedAndAnUnfedAnimalCountsADayWithoutFood() {
        // Given a hen fed yesterday
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        Animal gallina = DatosJuego.gallina(gallinero);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 1);
        aplicacion.controlador.alimentar(gallinero.id());
        aplicacion.controlador.diaFin();

                // Cuando empieza el día nuevo
        assertFalse(gallina.esHoyAlimentado());

                // Entonces, si el jugador no la alimenta, el día cuenta como un día sin comida
        aplicacion.controlador.diaFin();
        assertEquals(CondicionAnimal.HUNGRY, gallina.condicion());
        assertEquals(1, gallina.diasSinAlimentar());
    }

    @Test
    void HU21_aFailedEndOfDayRestoresTheAnimalsExactly() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.HUNGRY, 1, true, 1);
        var antes = aplicacion.estado().mementoCrear();

        aplicacion.estado().granja().edificios().get(0).animales().get(0).terminarDeDia();
        aplicacion.estado().restaurar(antes);

        var restaurado = aplicacion.estado().granja().edificios().get(0).animales().get(0);
        assertEquals(CondicionAnimal.HUNGRY, restaurado.condicion());
        assertEquals(1, restaurado.diasSinAlimentar());
        assertEquals(1, restaurado.pendiente());
        assertEquals(true, restaurado.esHoyAlimentado());
    }
}
