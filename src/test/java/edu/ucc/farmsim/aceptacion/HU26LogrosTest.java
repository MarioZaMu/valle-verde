package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaLogro;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.progreso.Logro;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-26: logros con recompensa, evaluados después de cada acción exitosa y al final del día (RN-28). */
class HU26LogrosTest {

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    private static void cultivoMaduro(AplicacionTest aplicacion, int col) {
        DatosJuego.plantar(aplicacion.estado(), col, FILA, TipoCultivo.CARROT, 4);
    }

    @Test
    void HU26_ca1_theFirstHarvestUnlocksLg01PaysFiftyAndPublishesTheEvent() {
                // Dadas ninguna cosecha y un cultivo maduro
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        cultivoMaduro(aplicacion, COL);

                // Cuando el jugador lo cosecha
        aplicacion.controlador.cosecha(COL, FILA);

                // Entonces LG-01 se desbloquea, se pagan 50 m y se publicó el evento
        assertTrue(aplicacion.estado().progreso().has(Logro.FIRST_HARVEST));
        assertEquals(550, aplicacion.estado().dinero());
        assertTrue(aplicacion.eventos.contains(new EventoJuego.LogroDesbloqueado("LG-01", "Primera cosecha", 50)));
        assertEquals(1, aplicacion.estado().progreso().cosechas());
    }

    @Test
    void HU26_ca2_un_logro_es_pagado_solo_una_vez() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        cultivoMaduro(aplicacion, COL);
        cultivoMaduro(aplicacion, COL + 1);
        aplicacion.controlador.cosecha(COL, FILA);
        aplicacion.eventos.clear();

        aplicacion.controlador.cosecha(COL + 1, FILA);

        assertEquals(550, aplicacion.estado().dinero());
        assertTrue(aplicacion.eventos.stream().noneMatch(EventoJuego.LogroDesbloqueado.class::isInstance));
    }

    @Test
    void HU26_ca3_clearingWitheredPlotsDoesNotCountAndThe25thHarvestPays150() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        var seguidor = aplicacion.estado().progreso();
        seguidor.desbloquear(Logro.FIRST_HARVEST);
        for (int i = 0; i < 24; i++) {
            seguidor.cosecharRegistro();
        }
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 2, 0, false, true);
        aplicacion.controlador.cosecha(COL, FILA); // limpia el cultivo marchito
        assertEquals(24, seguidor.cosechas());
        assertTrue(!seguidor.has(Logro.GREEN_THUMB));

        cultivoMaduro(aplicacion, COL + 1);
        aplicacion.controlador.cosecha(COL + 1, FILA);

        assertTrue(seguidor.has(Logro.GREEN_THUMB));
        assertEquals(500 + 150, aplicacion.estado().dinero());
    }

    @Test
    void HU26_ca4_aBalanceOf3000UnlocksLg05AndReachingYearTwoUnlocksLg09() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.PUMPKIN, 40);
        DatosJuego.dineroFijar(aplicacion.estado(), 2950);

        aplicacion.controlador.vender("PUMPKIN", 10);

        assertTrue(aplicacion.estado().progreso().has(Logro.CAPITALIST));
        assertTrue(aplicacion.eventos.contains(new EventoJuego.LogroDesbloqueado("LG-05", "Capitalista", 200)));

        DatosJuego.fechaFijar(1, Estacion.WINTER, 28);
        long antes = aplicacion.estado().dinero();
        aplicacion.controlador.diaFin();

        assertTrue(aplicacion.estado().progreso().has(Logro.ONE_YEAR_IN_THE_VALLEY));
        assertEquals(antes + 500, aplicacion.estado().dinero());
    }

    @Test
    void HU26_ca5_theFirstBuildingFourAnimalsAndTheFirstCureUnlockLg06Lg07AndLg08() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 2000);

        aplicacion.controlador.construir("COOP", 2, 7);
        assertTrue(aplicacion.estado().progreso().has(Logro.BUILDER));
        long construirDespues = aplicacion.estado().dinero();
        for (int i = 0; i < 4; i++) {
            aplicacion.controlador.animalComprar("HEN");
        }
        assertTrue(aplicacion.estado().progreso().has(Logro.BARNYARD_FARMER));
        assertEquals(construirDespues - 4 * 150 + 150, aplicacion.estado().dinero());

        Edificio gallinero = aplicacion.estado().granja().edificios().get(0);
        gallinero.animales().get(0).curar(); // healthy: nothing happens
        DatosJuego.casa(aplicacion.estado().granja().colocar(edu.ucc.farmsim.dominio.edificio.TipoEdificio.COOP, 5, 7),
                TipoAnimal.HEN, CondicionAnimal.SICK, 3, false, 0);
        aplicacion.estado().inventario().add(TipoArticulo.MEDICINE, 1);
        long curarAntes = aplicacion.estado().dinero();
        aplicacion.controlador.curar(1, 0);

        assertTrue(aplicacion.estado().progreso().has(Logro.VETERINARIAN));
        assertEquals(curarAntes + 80, aplicacion.estado().dinero());
    }

    @Test
    void HU26_theSaleOf50UnitsUnlocksTheTrader() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 50);

        aplicacion.controlador.vender("CARROT", 50);

        assertTrue(aplicacion.estado().progreso().has(Logro.TRADER));
        assertEquals(50, aplicacion.estado().progreso().unidadesVendidos());
    }

    @Test
    void HU26_el_dialogo_lista_cada_logro_con_su_progreso() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().progreso().cosecharRegistro();
        aplicacion.estado().logrosEvaluar();

        List<VistaLogro> lista = aplicacion.controlador.logros().value();

        assertEquals(9, lista.size());
        VistaLogro first = lista.get(0);
        assertEquals("LG-01", first.codigo());
        assertTrue(first.desbloqueado());
        VistaLogro segundo = lista.get(1);
        assertEquals("Manos a la tierra", segundo.name());
        assertEquals(1, segundo.progreso());
        assertEquals(25, segundo.objetivo());
        assertEquals(50, lista.get(3).objetivo());
    }

    @Test
    void HU26_aRewardThatReachesTheBalanceOfLg05ChainsTheNextAchievement() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 2900);
        for (int i = 0; i < 99; i++) {
            aplicacion.estado().progreso().cosecharRegistro();
        }
        aplicacion.estado().progreso().cosecharRegistro();

        var desbloqueado = aplicacion.estado().logrosEvaluar();

        // LG-01 50 + LG-02 150 + LG-03 400 = 600 -> 3500 >= 3000 -> LG-05
        assertTrue(desbloqueado.containsAll(List.of(Logro.FIRST_HARVEST, Logro.GREEN_THUMB,
                Logro.BIG_HARVESTER, Logro.CAPITALIST)));
        assertEquals(2900 + 50 + 150 + 400 + 200, aplicacion.estado().dinero());
    }
}
