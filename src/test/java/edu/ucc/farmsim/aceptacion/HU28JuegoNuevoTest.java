package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.granja.MapaGranja;
import edu.ucc.farmsim.dominio.granja.Terreno;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import org.junit.jupiter.api.Test;

/**
 * HU-28: iniciar una partida nueva con el estado inicial de RN-30.
 *
 * <p>La demanda del mercado (100 para todos los productos) y los logros bloqueados con contadores en 0 se comprueban
 * cuando existan esas clases (HU-15 en la fase 5.3 y HU-26 en la fase 5.5). La confirmación antes de descartar una
 * partida en curso se completa del lado de la UI con {@code HU28_ca3_cancellingTheConfirmation...} en las pruebas de la
 * pantalla de inicio (HU-33).
 */
class HU28JuegoNuevoTest {

    @Test
    void HU28_ca1_newGameStartsWithTheInitialValuesOfRn30() {
                // Dado que el jugador elige «Nueva partida»
        AplicacionTest aplicacion = AplicacionTest.create();

                // Cuando se crea la partida
        Resultado<VistaHud> resultado = aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);

                // Entonces el estado es Año 1, Primavera, día 1, Sunny, 500 m y 100 de energía
        assertTrue(resultado.isOk());
        EstadoJuego estado = aplicacion.sesion.actual();
        assertEquals(new FechaJuego(1, Estacion.SPRING, 1), estado.fecha());
        assertEquals(Clima.SUNNY, estado.clima().hoy());
        assertEquals(500, estado.dinero());
        assertEquals(100, estado.energia().actual());

                // Y el inventario tiene 10 semillas de zanahoria y 5 de papa, nada más
        assertEquals(10, estado.inventario().count(TipoArticulo.CARROT_SEED));
        assertEquals(5, estado.inventario().count(TipoArticulo.POTATO_SEED));
        assertEquals(2, estado.inventario().ranurasUsados());

                // Y no hay edificios, ni animales, los logros están bloqueados y todos los contadores en 0
        assertTrue(estado.granja().edificios().isEmpty());
        assertEquals(new edu.ucc.farmsim.dominio.progreso.InstantaneaProgreso(java.util.Set.of(), 0, 0, 0, 0),
                estado.progreso().instantanea());
        assertTrue(aplicacion.controlador.logros().value().stream()
                .noneMatch(edu.ucc.farmsim.aplicacion.dto.VistaLogro::desbloqueado));

                // Y la vista que dibuja la pantalla muestra los mismos valores
        VistaHud hud = resultado.value();
        assertEquals(500, hud.dinero());
        assertEquals(1, hud.anio());
        assertEquals("SPRING", hud.estacion());
        assertEquals(1, hud.dia());
        assertEquals("SUNNY", hud.clima());
        assertEquals(100, hud.energia());
        assertEquals(100, hud.energiaMaxima());
    }

    @Test
    void HU28_ca2_initialValleyHas1152PlotsWith40Soil976Grass6Water30Obstacles34PathAnd66House() {
                // Cuando se crea una partida nueva
        MapaGranja map = EstadoJuego.juegoNuevo(AplicacionTest.SEED).granja();

                // Entonces el valle es de 48 x 24 = 1152 parcelas con la distribución de RN-04
        assertEquals(48, map.cols());
        assertEquals(24, map.filas());
        assertEquals(40, map.count(Terreno.SOIL));
        assertEquals(976, map.count(Terreno.GRASS));
        assertEquals(6, map.count(Terreno.WATER));
        assertEquals(30, map.count(Terreno.OBSTACLE));
        assertEquals(34, map.count(Terreno.PATH));
        assertEquals(66, map.count(Terreno.HOUSE));
        int total = 0;
        for (Terreno terreno : Terreno.values()) {
            total += map.count(terreno);
        }
        assertEquals(1152, total);

                // Y las 40 parcelas de tierra forman un solo bloque de 8 x 5
        for (int fila = 0; fila < map.filas(); fila++) {
            for (int col = 0; col < map.cols(); col++) {
                boolean bloquearDentro = col >= ReglasJuego.COL_TIERRA && col < ReglasJuego.COL_TIERRA + ReglasJuego.ANCHO_TIERRA
                        && fila >= ReglasJuego.FILA_TIERRA && fila < ReglasJuego.FILA_TIERRA + ReglasJuego.ALTO_TIERRA;
                assertEquals(bloquearDentro, map.parcelaEn(col, fila).terreno() == Terreno.SOIL, "plot " + col + "," + fila);
            }
        }
    }

    @Test
    void HU28_ca3_aGameInProgressIsReportedSoTheUiCanAskForConfirmationAndCancellingKeepsIt() {
        AplicacionTest aplicacion = AplicacionTest.create();
        assertFalse(aplicacion.controlador.tieneJuegoEnProgreso());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        assertTrue(aplicacion.controlador.tieneJuegoEnProgreso());

                // Dada una partida en curso en la que el jugador gastó energía
        EstadoJuego antes = aplicacion.sesion.actual();
        antes.energia().gastarIntentar(10);

                // Cuando se le pide confirmar y cancela, la UI simplemente no vuelve a llamar a newGame Entonces la partida
        // actual queda exactamente como estaba
        assertTrue(aplicacion.controlador.tieneJuegoEnProgreso());
        assertEquals(90, aplicacion.sesion.actual().energia().actual());
    }

    @Test
    void HU28_ca3_confirmingReplacesTheGameInProgressWithAFreshOne() {
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.sesion.actual().energia().gastarIntentar(10);

        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);

        assertEquals(100, aplicacion.sesion.actual().energia().actual());
    }

    @Test
    void HU28_ca4_mismo_semilla_da_el_mismo_inicial_juego_y_pronostico() {
                // Dada la misma semilla del generador
        EstadoJuego first = EstadoJuego.juegoNuevo(77L);
        Clima pronosticoPrimero = first.clima().pronostico();
        EstadoJuego segundo = EstadoJuego.juegoNuevo(77L);

                // Entonces ambas partidas tienen el mismo pronóstico para el día 2 y el mismo estado aleatorio
        assertEquals(pronosticoPrimero, segundo.clima().pronostico());
        assertEquals(first.azar().estado(), segundo.azar().estado());
    }
}
