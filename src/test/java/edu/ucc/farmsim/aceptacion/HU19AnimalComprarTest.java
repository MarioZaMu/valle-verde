package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaAnimal;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import org.junit.jupiter.api.Test;

/** HU-19: comprar gallinas y vacas y alojarlas (RN-14, RN-20, RN-23). */
class HU19AnimalComprarTest {

    @Test
    void HU19_ca1_buyingAHenCosts150AndHousesAHealthyHen() {
                // Dados un gallinero vacío y 500 m
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.gallinero(aplicacion.estado());

                // Cuando el jugador compra una gallina
        Resultado<VistaEdificio> resultado = aplicacion.controlador.animalComprar("HEN");

                // Entonces el saldo es 350 m y una gallina sana vive en el gallinero
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(350, aplicacion.estado().dinero());
        VistaAnimal gallina = resultado.value().animales().get(0);
        assertEquals("HEALTHY", gallina.condicion());
        assertEquals(0, gallina.diasSinAlimentar());
        assertEquals("Gallina", gallina.name());
        assertEquals(1, aplicacion.estado().granja().edificios().get(0).animales().size());
    }

    @Test
    void HU19_ca2_withoutACompatibleBuildingTheAnimalIsRejectedAndTheBalanceDoesNotChange() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 1000);

        Resultado<VistaEdificio> gallina = aplicacion.controlador.animalComprar("HEN");
        Resultado<VistaEdificio> vaca = aplicacion.controlador.animalComprar("COW");

        assertEquals(CodigoError.SIN_ESPACIO_ANIMALES, gallina.codigo());
        assertEquals("No hay espacio para más Gallinas: construye o amplía el edificio.", gallina.mensaje());
        assertEquals(CodigoError.SIN_ESPACIO_ANIMALES, vaca.codigo());
        assertEquals("No hay espacio para más Vacas: construye o amplía el edificio.", vaca.mensaje());
        assertEquals(1000, aplicacion.estado().dinero());
    }

    @Test
    void HU19_ca3_aFullCoopRejectsAFifthHenAndASecondCoopTakesIt() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio first = DatosJuego.gallinero(aplicacion.estado());
        for (int i = 0; i < 4; i++) {
            DatosJuego.gallina(first);
        }

        Resultado<VistaEdificio> rechazado = aplicacion.controlador.animalComprar("HEN");
        assertEquals(CodigoError.SIN_ESPACIO_ANIMALES, rechazado.codigo());
        assertEquals(500, aplicacion.estado().dinero());

        Edificio segundo = DatosJuego.construir(aplicacion.estado(), TipoEdificio.COOP, 5, 7);
        Resultado<VistaEdificio> aceptado = aplicacion.controlador.animalComprar("HEN");

        assertTrue(aceptado.isOk());
        assertEquals(segundo.id(), aceptado.value().id());
        assertEquals(1, segundo.animales().size());
        assertEquals(4, first.animales().size());
    }

    @Test
    void HU19_ca3_aCowGoesToTheBarnAndAHenNeverDoes() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 1000);
        DatosJuego.construir(aplicacion.estado(), TipoEdificio.BARN, 2, 7);

        assertEquals(CodigoError.SIN_ESPACIO_ANIMALES, aplicacion.controlador.animalComprar("HEN").codigo());
        assertTrue(aplicacion.controlador.animalComprar("COW").isOk());
        assertEquals(200, aplicacion.estado().dinero());
    }

    @Test
    void HU19_ca4_no_suficiente_dinero_es_rechazado_con_el_faltante_monto() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.dineroFijar(aplicacion.estado(), 149);

        Resultado<VistaEdificio> resultado = aplicacion.controlador.animalComprar("HEN");

        assertEquals(CodigoError.SIN_FONDOS, resultado.codigo());
        assertEquals("Fondos insuficientes: faltan 1 m.", resultado.mensaje());
        assertEquals(149, aplicacion.estado().dinero());
        assertTrue(aplicacion.estado().granja().edificios().get(0).animales().isEmpty());
    }

    @Test
    void HU19_el_mercado_ofrece_animales_con_su_libre_lugares() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.gallina(gallinero);

        var animales = aplicacion.controlador.mercado().value().animales();

        assertEquals(2, animales.size());
        assertEquals("Gallina", animales.get(0).name());
        assertEquals(150, animales.get(0).precio());
        assertEquals(3, animales.get(0).lugaresLibres());
        assertEquals(800, animales.get(1).precio());
        assertEquals(0, animales.get(1).lugaresLibres());
    }
}
