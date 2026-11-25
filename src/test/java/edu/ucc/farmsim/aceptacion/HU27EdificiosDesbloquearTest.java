package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaOfertaEdificio;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.dominio.progreso.Logro;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-27: los edificios se desbloquean con logros (RN-28, RN-29). */
class HU27EdificiosDesbloquearTest {

    private static List<Boolean> desbloqueado(AplicacionTest aplicacion) {
        return aplicacion.controlador.edificios().value().catalogo().stream().map(VistaOfertaEdificio::desbloqueado).toList();
    }

    @Test
    void HU27_ca1_aNewGameOnlyHasTheCoopAvailable() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        assertEquals(List.of(true, false, false, false), desbloqueado(aplicacion));
    }

    @Test
    void HU27_ca2_cada_logro_desbloquea_su_edificio() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        DatosJuego.desbloquear(aplicacion.estado(), Logro.FIRST_HARVEST);
        assertEquals(List.of(true, false, true, false), desbloqueado(aplicacion));
        DatosJuego.desbloquear(aplicacion.estado(), Logro.GREEN_THUMB);
        assertEquals(List.of(true, true, true, false), desbloqueado(aplicacion));
        DatosJuego.desbloquear(aplicacion.estado(), Logro.CAPITALIST);
        assertEquals(List.of(true, true, true, true), desbloqueado(aplicacion));
    }

    @Test
    void HU27_ca2_cosechando_para_real_desbloquea_el_almacen() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA, TipoCultivo.CARROT, 4);

        aplicacion.controlador.cosecha(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA);

        assertTrue(aplicacion.controlador.edificios().value().catalogo().get(2).desbloqueado());
        assertTrue(aplicacion.controlador.construir("WAREHOUSE", 2, 7).isOk());
    }

    @Test
    void HU27_ca3_aLockedBuildingNamesTheAchievementItNeeds() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 3000);

        var invernadero = aplicacion.controlador.construir("GREENHOUSE", 2, 7);

        assertEquals(CodigoError.EDIFICIO_BLOQUEADO, invernadero.codigo());
        assertEquals("Edificio bloqueado: requiere el logro «Capitalista».", invernadero.mensaje());
    }

    @Test
    void HU27_ca4_un_desbloqueado_edificio_queda_desbloqueado_despues_guardando_y_cargando() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        DatosJuego.desbloquear(aplicacion.estado(), Logro.GREEN_THUMB);
        aplicacion.controlador.guardar(1);
        aplicacion.controlador.juegoNuevo(5L);
        assertFalse(desbloqueado(aplicacion).get(1));

        assertTrue(aplicacion.controlador.cargar(1).isOk());

        assertEquals(List.of(true, true, true, false), desbloqueado(aplicacion));
    }
}
