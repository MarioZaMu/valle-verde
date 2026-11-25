package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import edu.ucc.farmsim.dominio.tiempo.EstadoEstacion;
import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-09: calendario con 4 estaciones de 28 días y años de 112 días (RN-01). */
class HU09CalendarioYEstacionesTest {

    private RelojJuego reloj;

    @BeforeEach
    void relojReiniciar() {
        reloj = RelojJuego.getInstance();
        reloj.reiniciarParaTests();
    }

    @Test
    void HU09_ca1_advancesOneDayAtATime() {
        // Given Year 1, Spring, day 1
        assertEquals(new FechaJuego(1, Estacion.SPRING, 1), reloj.fecha());

                // Cuando termina el día
        reloj.diaAvanzar();

                // Entonces es Año 1, Primavera, día 2
        assertEquals(new FechaJuego(1, Estacion.SPRING, 2), reloj.fecha());
    }

    @Test
    void HU09_ca2_changesSeasonAfterDay28InOrderSpringSummerAutumnWinter() {
                // Dado el día 28 de Primavera
        reloj.restaurar(new FechaJuego(1, Estacion.SPRING, ReglasJuego.DIAS_POR_ESTACION));

                // Cuando termina el día, entonces es el día 1 de Verano del mismo año
        reloj.diaAvanzar();
        assertEquals(new FechaJuego(1, Estacion.SUMMER, 1), reloj.fecha());

                // Y el orden continúa Verano -> Otoño -> Invierno
        diasAvanzar(ReglasJuego.DIAS_POR_ESTACION);
        assertEquals(new FechaJuego(1, Estacion.AUTUMN, 1), reloj.fecha());
        diasAvanzar(ReglasJuego.DIAS_POR_ESTACION);
        assertEquals(new FechaJuego(1, Estacion.WINTER, 1), reloj.fecha());
    }

    @Test
    void HU09_ca3_changesYearAfterDay28OfWinter() {
                // Dado el día 28 de Invierno del Año 1
        reloj.restaurar(new FechaJuego(1, Estacion.WINTER, ReglasJuego.DIAS_POR_ESTACION));

                // Cuando termina el día, entonces es Año 2, Primavera, día 1
        reloj.diaAvanzar();

        assertEquals(new FechaJuego(2, Estacion.SPRING, 1), reloj.fecha());
    }

    @Test
    void HU09_ca4_aFullCycleOf112DaysReturnsToSpringAndTheGameNeverEnds() {
        // Given a new game
        int diasPorAnio = ReglasJuego.DIAS_POR_ESTACION * Estacion.values().length;
        assertEquals(112, diasPorAnio);

                // Cuando pasan 112 días, entonces es Año 2, Primavera, día 1
        diasAvanzar(diasPorAnio);
        assertEquals(new FechaJuego(2, Estacion.SPRING, 1), reloj.fecha());

                // Y el juego simplemente sigue
        diasAvanzar(1);
        assertEquals(new FechaJuego(2, Estacion.SPRING, 2), reloj.fecha());
    }

    @Test
    void HU09_manana_asoma_en_la_siguiente_fecha_sin_avanzando() {
        reloj.restaurar(new FechaJuego(1, Estacion.WINTER, ReglasJuego.DIAS_POR_ESTACION));

        assertEquals(new FechaJuego(2, Estacion.SPRING, 1), reloj.manana());
        assertEquals(new FechaJuego(1, Estacion.WINTER, ReglasJuego.DIAS_POR_ESTACION), reloj.fecha());
    }

    @Test
    void HU09_clockIsASingleSharedInstance() {
        assertSame(RelojJuego.getInstance(), RelojJuego.getInstance());
    }

    @Test
    void HU09_cada_estacion_estado_sabe_su_estacion_y_el_siguiente_uno() {
        for (Estacion estacion : Estacion.values()) {
            EstadoEstacion estado = EstadoEstacion.of(estacion);
            assertEquals(estacion, estado.estacion());
            assertEquals(Estacion.values()[(estacion.ordinal() + 1) % Estacion.values().length], estado.next().estacion());
        }
    }

    @Test
    void HU09_invalido_fechas_son_rechazado() {
        assertThrows(IllegalArgumentException.class, () -> new FechaJuego(0, Estacion.SPRING, 1));
        assertThrows(IllegalArgumentException.class, () -> new FechaJuego(1, Estacion.SPRING, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new FechaJuego(1, Estacion.SPRING, ReglasJuego.DIAS_POR_ESTACION + 1));
    }

    private void diasAvanzar(int dias) {
        for (int i = 0; i < dias; i++) {
            reloj.diaAvanzar();
        }
    }
}
