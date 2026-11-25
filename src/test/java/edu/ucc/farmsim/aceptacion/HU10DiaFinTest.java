package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.juego.FaseDia;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.juego.PasoDia;
import edu.ucc.farmsim.dominio.juego.ReporteDia;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU-10: terminar el día, resolviendo sus efectos en el orden fijo de RN-03. */
class HU10DiaFinTest {

    /** Un paso del pipeline que solo registra que corrió. */
    private record PasoGrabacion(FaseDia fase, List<String> log) implements PasoDia {
        @Override
        public void apply(EstadoJuego estado, ReporteDia reporte) {
            log.add("step:" + fase.name());
        }
    }

    @Test
    void HU10_ca1_stepsRunInTheOrderOfRn03EvenIfRegisteredInAnyOrderAndEventsArePublishedLast() {
                // Dado un paso por fase, registrados en orden mezclado
        List<String> log = new ArrayList<>();
        List<FaseDia> mezclado = new ArrayList<>(Arrays.asList(FaseDia.values()));
        Collections.shuffle(mezclado, new Random(7));
        SecuenciaDia secuencia = new SecuenciaDia();
        for (FaseDia fase : mezclado) {
            secuencia.add(new PasoGrabacion(fase, log));
        }
        AplicacionTest aplicacion = AplicacionTest.create(secuencia);
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.bus.suscribir(evento -> log.add("event"));

                // Cuando termina el día
        Resultado<VistaHud> resultado = aplicacion.controlador.diaFin();

                // Entonces los pasos corrieron como dice RN-03: efectos del clima, crecimiento, animales, mercado, calendario,
        // pronóstico, energía, logros; y los eventos se publicaron después de todos ellos (paso 9)
        assertTrue(resultado.isOk());
        List<String> esperado = new ArrayList<>();
        for (FaseDia fase : FaseDia.values()) {
            esperado.add("step:" + fase.name());
        }
        esperado.add("event"); // DayEnded
        esperado.add("event"); // DaySummaryReady
        assertEquals(esperado, log);
        assertEquals(List.of(FaseDia.WEATHER_EFFECTS, FaseDia.GROWTH, FaseDia.ANIMALS, FaseDia.MARKET,
                FaseDia.CALENDAR, FaseDia.FORECAST, FaseDia.ENERGY, FaseDia.ACHIEVEMENTS),
                Arrays.asList(FaseDia.values()));
    }

    @Test
    void HU10_ca1_theStandardPipelineRunsEveryRegisteredStepInRn03Order() {
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        EstadoJuego estado = aplicacion.sesion.actual();

        ReporteDia reporte = SecuenciaDia.estandar().run(estado);

        List<FaseDia> corrio = reporte.ejecutarFases();
        assertTrue(corrio.containsAll(List.of(FaseDia.CALENDAR, FaseDia.FORECAST, FaseDia.ENERGY)), corrio.toString());
        List<FaseDia> clasificado = new ArrayList<>(corrio);
        Collections.sort(clasificado);
        assertEquals(clasificado, corrio, "the standard pipeline must run its steps in the order of RN-03");
        assertEquals(new FechaJuego(1, Estacion.SPRING, 1), reporte.fechaAnterior());
        assertEquals(new FechaJuego(1, Estacion.SPRING, 2), reporte.fechaNueva());
        assertEquals(estado.clima().hoy(), reporte.climaHoy());
    }

    @Test
    void HU10_ca2_endingTheDayRestoresEnergyTo100() {
                // Dada una partida donde se gastaron 60 de energía (quedan 40)
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        assertTrue(aplicacion.sesion.actual().energia().gastarIntentar(60));
        assertEquals(40, aplicacion.sesion.actual().energia().actual());

                // Cuando termina el día
        Resultado<VistaHud> resultado = aplicacion.controlador.diaFin();

                // Entonces la energía es 100 y la vista muestra el día 2
        assertTrue(resultado.isOk());
        assertEquals(100, aplicacion.sesion.actual().energia().actual());
        assertEquals(100, resultado.value().energia());
        assertEquals(2, resultado.value().dia());
                // (las marcas de «regado» de las parcelas las borra HU-03 en la fase 5.2)
    }

    @Test
    @Tag("perf")
    @Disabled("needs full-load fixture, see PLAN_DE_PRUEBAS section 9")
    void HU10_ca3_endOfDayTakesAtMost200msAtFullLoad() {
                // Dada una granja a plena carga (40 cultivos al aire libre, 9 en el invernadero, 6 edificios, 16 animales)
        // Cuando el día termina 100 veces, entonces el percentil 95 es como máximo 200 ms (RNF-01). Fase 5.5.
    }

    @Test
    void HU10_ca4_un_interno_fallo_hojas_el_entero_estado_identico_y_publica_nada() {
                // Dado un paso que cambia cosas y luego falla
        PasoDia fallando = new PasoDia() {
            @Override
            public FaseDia fase() {
                return FaseDia.FORECAST;
            }

            @Override
            public void apply(EstadoJuego estado, ReporteDia reporte) {
                estado.energia().gastarIntentar(30);
                RelojJuego.getInstance().diaAvanzar();
                throw new IllegalStateException("boom");
            }
        };
        AplicacionTest aplicacion = AplicacionTest.create(SecuenciaDia.estandar().add(fallando));
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        MementoJuego antes = aplicacion.sesion.actual().mementoCrear();
        aplicacion.eventos.clear();

                // Cuando termina el día
        Resultado<VistaHud> resultado = aplicacion.controlador.diaFin();

                // Entonces el resultado es un error, el estado es exactamente el de antes y no se publicó ningún evento
        assertFalse(resultado.isOk());
        assertEquals(CodigoError.ERROR_INTERNO, resultado.codigo());
        assertEquals("Ocurrió un error inesperado. Tu partida no cambió.", resultado.mensaje());
        assertEquals(antes, aplicacion.sesion.actual().mementoCrear());
        assertEquals(100, aplicacion.sesion.actual().energia().actual());
        assertEquals(new FechaJuego(1, Estacion.SPRING, 1), RelojJuego.getInstance().fecha());
        assertTrue(aplicacion.eventos.isEmpty());
    }

    @Test
    void HU10_el_memento_restaura_cada_parte_de_el_estado() {
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        EstadoJuego estado = aplicacion.sesion.actual();
        aplicacion.controlador.diaFin();
        estado.energia().gastarIntentar(25);
        estado.inventario().add(edu.ucc.farmsim.dominio.economia.TipoArticulo.EGG, 7);
        MementoJuego archivado = estado.mementoCrear();

        aplicacion.controlador.diaFin();
        aplicacion.controlador.diaFin();
        estado.inventario().remove(edu.ucc.farmsim.dominio.economia.TipoArticulo.CARROT_SEED, 3);
        assertFalse(archivado.equals(estado.mementoCrear()));

        estado.restaurar(archivado);

        assertEquals(archivado, estado.mementoCrear());
        assertEquals(new FechaJuego(1, Estacion.SPRING, 2), RelojJuego.getInstance().fecha());
    }

    @Test
    void HU10_endingTheDayWithoutAGameIsRejectedWithSinPartida() {
        AplicacionTest aplicacion = AplicacionTest.create();

        Resultado<VistaHud> resultado = aplicacion.controlador.diaFin();

        assertFalse(resultado.isOk());
        assertEquals(CodigoError.SIN_PARTIDA, resultado.codigo());
    }

    @Test
    void HU10_aSuccessfulEndOfDayPublishesTheDayEndedEventWithTheNewHud() {
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.eventos.clear();

        Resultado<VistaHud> resultado = aplicacion.controlador.diaFin();

        assertEquals(2, aplicacion.eventos.size());
        assertEquals(new EventoJuego.DiaTerminado(resultado.value()), aplicacion.eventos.get(0));
        assertTrue(aplicacion.eventos.get(1) instanceof EventoJuego.ResumenDiaListo);
    }

    @Test
    void AR4_controlador_nunca_lanza_incluso_cuando_la_sesion_falla() {
                // Dada una sesión que explota apenas se lee el estado
        SesionJuego defectuoso = new SesionJuego() {
            @Override
            public boolean tieneJuego() {
                return true;
            }

            @Override
            public EstadoJuego actual() {
                throw new IllegalStateException("disk on fire");
            }
        };
        AplicacionTest aplicacion = AplicacionTest.create(defectuoso, SecuenciaDia.estandar());

                // Entonces toda llamada del controlador responde con ERROR_INTERNO en lugar de lanzar una excepción
        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.diaFin().codigo());
        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.hud().codigo());
    }

    @Test
    void HU10_aPipelineRejectsMissingSteps() {
        try {
            new SecuenciaDia().add(null);
        } catch (IllegalArgumentException esperado) {
            return;
        }
        throw new AssertionError("A null step must be rejected");
    }
}
