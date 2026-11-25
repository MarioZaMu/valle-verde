package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.RepositorioGuardadoFalso;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import org.junit.jupiter.api.Test;

/**
 * RN-31 y RNF-11: cuando no se puede llegar a las partidas guardadas se le avisa al jugador, y jugar no se ve afectado.
 */
class HU33CaidaAlmacenamientoGuardadoTest {

    private static AplicacionTest aplicacionConAbajoAlmacenamiento() {
        return AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), new RepositorioGuardadoFalso().inalcanzable());
    }

    @Test
    void HU33_ca3_askingForSavedGamesWhileMongoDbIsDownIsErrorEsWithTheRn35Message() {
        AplicacionTest aplicacion = aplicacionConAbajoAlmacenamiento();

        Resultado<Boolean> resultado = aplicacion.controlador.tieneJuegosArchivados();

        assertFalse(resultado.isOk());
        assertEquals(CodigoError.ERROR_ES, resultado.codigo());
        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", resultado.mensaje());
    }

    @Test
    void HU33_ca3_aStorageOutageNeverStopsTheGame() {
        AplicacionTest aplicacion = aplicacionConAbajoAlmacenamiento();

        Resultado<VistaHud> iniciado = aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.controlador.tieneJuegosArchivados();
        Resultado<VistaHud> next = aplicacion.controlador.diaFin();

        assertTrue(iniciado.isOk());
        assertTrue(next.isOk());
        assertEquals(2, next.value().dia());
    }

    @Test
    void HU33_ca3_aWorkingStorageStillAnswersTrueOrFalse() {
        assertEquals(false, AplicacionTest.create().controlador.tieneJuegosArchivados().value());
        AplicacionTest conGuardado = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), new RepositorioGuardadoFalso().conRanura(1));
        assertEquals(true, conGuardado.controlador.tieneJuegosArchivados().value());
    }
}
