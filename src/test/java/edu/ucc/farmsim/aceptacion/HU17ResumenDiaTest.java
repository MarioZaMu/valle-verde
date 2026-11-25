package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-17: el resumen del día que acaba de terminar (RN-03, RN-14). */
class HU17ResumenDiaTest {

    private static VistaResumenDia resumenDe(AplicacionTest aplicacion) {
        return aplicacion.eventos.stream().filter(EventoJuego.ResumenDiaListo.class::isInstance)
                .map(e -> ((EventoJuego.ResumenDiaListo) e).resumen()).reduce((first, segundo) -> segundo).orElseThrow();
    }

    @Test
    void HU17_ca1_aSaleAndAPurchaseAppearAsIncomeAndExpenses() {
                // Dado un día con una venta de 79 m y una compra de 150 m
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);
        aplicacion.controlador.comprar("CORN_SEED", 5);
        aplicacion.controlador.vender("CARROT", 3);

                // Cuando termina el día
        aplicacion.controlador.diaFin();

                // Entonces el resumen muestra ingresos 79, gastos 150 y los saldos
        VistaResumenDia resumen = resumenDe(aplicacion);
        assertEquals(79, resumen.ingresos());
        assertEquals(150, resumen.gastos());
        assertEquals(500, resumen.antesSaldo());
        assertEquals(429, resumen.despuesSaldo());
        assertEquals(1, resumen.dia());
        assertEquals("SPRING", resumen.estacion());
    }

    @Test
    void HU17_ca2_aWitheredCropAppearsInTheHighlights() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA, TipoCultivo.CARROT, 1, 2, false, false);

        aplicacion.controlador.diaFin();

        assertEquals(List.of("Se marchitó 1 cultivo."), resumenDe(aplicacion).luces());
    }

    @Test
    void HU17_ca2_descuidado_animales_y_su_productos_aparecer_demasiado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        var gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.casa(gallinero, edu.ucc.farmsim.dominio.animal.TipoAnimal.HEN,
                edu.ucc.farmsim.dominio.animal.CondicionAnimal.HUNGRY, 2, false, 0);
        DatosJuego.casa(gallinero, edu.ucc.farmsim.dominio.animal.TipoAnimal.HEN,
                edu.ucc.farmsim.dominio.animal.CondicionAnimal.HEALTHY, 0, true, 0);

        aplicacion.controlador.diaFin();

        assertEquals(List.of("1 animal enfermó por falta de alimento.", "Los animales produjeron 1 unidad."),
                resumenDe(aplicacion).luces());
    }

    @Test
    void HU17_ca2_un_logro_desbloqueado_en_el_fin_de_el_dia_es_listado_con_su_recompensa() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.fechaFijar(1, edu.ucc.farmsim.dominio.tiempo.Estacion.WINTER, 28);

        aplicacion.controlador.diaFin();

        VistaResumenDia resumen = resumenDe(aplicacion);
        assertEquals(List.of("Logro desbloqueado: «Un año en el valle» (+500 m)."), resumen.luces());
        assertEquals(500, resumen.ingresos());
        assertEquals(1000, resumen.despuesSaldo());
        assertEquals(500, resumen.antesSaldo());
        assertTrue(aplicacion.eventos.contains(new EventoJuego.LogroDesbloqueado("LG-09", "Un año en el valle", 500)));
    }

    @Test
    void HU17_ca3_aDayWithoutMovementsSaysSinNovedades() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        aplicacion.controlador.diaFin();

        VistaResumenDia resumen = resumenDe(aplicacion);
        assertEquals(0, resumen.ingresos());
        assertEquals(0, resumen.gastos());
        assertEquals(List.of("Sin novedades"), resumen.luces());
        assertEquals(500, resumen.despuesSaldo());
    }

    @Test
    void HU17_ca4_el_siguiente_resumen_inicia_desde_cero() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);
        aplicacion.controlador.vender("CARROT", 3);
        aplicacion.controlador.diaFin();
        aplicacion.eventos.clear();

        aplicacion.controlador.diaFin();

        VistaResumenDia segundo = resumenDe(aplicacion);
        assertEquals(0, segundo.ingresos());
        assertEquals(0, segundo.gastos());
        assertEquals(2, segundo.dia());
        assertTrue(aplicacion.estado().libro().ingresos() == 0 && aplicacion.estado().libro().gastos() == 0);
    }
}
