package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.progreso.Logro;
import org.junit.jupiter.api.Test;

/** HU-24: construir un almacén para ampliar el inventario (RN-15, RN-20, RN-29). */
class HU24AlmacenTest {

    @Test
    void HU24_ca1_theWarehouseCosts300AndWidensTheInventoryFrom24To36() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.desbloquear(aplicacion.estado(), Logro.FIRST_HARVEST);
        assertEquals(24, aplicacion.estado().inventario().capacidad());

        Resultado<VistaEdificio> resultado = aplicacion.controlador.build("WAREHOUSE", 2, 7);

        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(300, aplicacion.estado().dinero()); // 500 - 300 + 100 of LG-06
        assertEquals(36, aplicacion.estado().inventario().capacidad());
        assertEquals(36, aplicacion.controlador.inventario().value().capacidad());
    }

    @Test
    void HU24_ca2_aSecondWarehouseIsRejectedByTheLimit() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.desbloquear(aplicacion.estado(), Logro.FIRST_HARVEST);
        DatosJuego.build(aplicacion.estado(), TipoEdificio.WAREHOUSE, 2, 7);

                // (9, 7): pasto libre lejos del avatar, que está en (7, 7) y sobre el que nunca se construye (RN-36)
        Resultado<VistaEdificio> resultado = aplicacion.controlador.build("WAREHOUSE", 9, 7);

        assertEquals(CodigoError.LIMITE_EDIFICIOS, resultado.codigo());
        assertEquals("Ya tienes el máximo de Almacenes.", resultado.mensaje());
    }

    @Test
    void HU24_ca3_sin_el_logro_eso_es_bloqueado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        Resultado<VistaEdificio> resultado = aplicacion.controlador.build("WAREHOUSE", 2, 7);

        assertEquals(CodigoError.EDIFICIO_BLOQUEADO, resultado.codigo());
        assertEquals("Edificio bloqueado: requiere el logro «Primera cosecha».", resultado.mensaje());
        assertEquals(24, aplicacion.estado().inventario().capacidad());
    }

    @Test
    void HU24_ca4_theNewSlotIsUsableWhenTheFirst24AreFull() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.desbloquear(aplicacion.estado(), Logro.FIRST_HARVEST);
        DatosJuego.salvoInventarioRellenar(aplicacion.estado(), TipoArticulo.CARROT, 5);
        assertEquals(false, aplicacion.estado().inventario().add(TipoArticulo.MEDICINE, 1));

        aplicacion.controlador.build("WAREHOUSE", 2, 7);

        assertTrue(aplicacion.estado().inventario().add(TipoArticulo.MEDICINE, 1));
        assertEquals(25, aplicacion.estado().inventario().ranurasUsados());
    }

    @Test
    void HU24_theWidenedInventorySurvivesAMementoRestore() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.desbloquear(aplicacion.estado(), Logro.FIRST_HARVEST);
        aplicacion.controlador.build("WAREHOUSE", 2, 7);
        var archivado = aplicacion.estado().mementoCrear();

        aplicacion.estado().inventario().ranurasAgregar(1);
        aplicacion.estado().restaurar(archivado);

        assertEquals(36, aplicacion.estado().inventario().capacidad());
        assertEquals(1, aplicacion.estado().granja().edificios().size());
    }
}
