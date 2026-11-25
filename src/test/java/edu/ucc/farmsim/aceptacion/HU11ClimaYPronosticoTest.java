package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.tiempo.AzarEstado;
import edu.ucc.farmsim.dominio.tiempo.CicloClima;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import edu.ucc.farmsim.dominio.tiempo.EstrategiaClima;
import edu.ucc.farmsim.dominio.tiempo.EstrategiaClimaEstacional;
import edu.ucc.farmsim.dominio.tiempo.EstrategiaClimaFija;
import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-11: clima del día y pronóstico de mañana (RN-11, RN-13). */
class HU11ClimaYPronosticoTest {

    private static final int DIBUJA = 10_000;
    private static final Set<Clima> CLIMA_PRIMAVERA = Set.of(Clima.SUNNY, Clima.CLOUDY, Clima.RAINY);

    private RelojJuego reloj;

    @BeforeEach
    void relojReiniciar() {
        reloj = RelojJuego.getInstance();
        reloj.reiniciarParaTests();
    }

    @Test
    void HU11_ca1_firstDayIsSunnyAndTheForecastForDay2IsAlreadyDrawn() {
                // Cuando empieza una partida nueva
        CicloClima clima = CicloClima.iniciarJuegoNuevo(reloj, new AzarEstado(1));

                // Entonces el día 1 es Sunny (fijo) y existe un pronóstico para el día 2
        assertEquals(Clima.SUNNY, clima.hoy());
        assertNotNull(clima.pronostico());
        assertTrue(CLIMA_PRIMAVERA.contains(clima.pronostico()), "a Spring forecast: " + clima.pronostico());
    }

    @Test
    void HU11_ca2_theForecastBecomesTodaysWeatherAndANewForecastIsDrawn() {
                // Dado el pronóstico «Rainy»
        CicloClima clima = CicloClima.iniciarJuegoNuevo(reloj, new AzarEstado(2));
        clima.restaurar(Clima.SUNNY, Clima.RAINY);

                // Cuando termina el día (primero avanza el calendario, RN-03 paso 5, luego el clima, paso 6)
        reloj.diaAvanzar();
        clima.avanzar(reloj, new AzarEstado(3));

                // Entonces hoy es Rainy y hay un nuevo pronóstico visible
        assertEquals(Clima.RAINY, clima.hoy());
        assertNotNull(clima.pronostico());
    }

    @Test
    void HU11_ca3_mismo_semilla_da_el_mismo_clima_para_el_entero_anio() {
                // Dadas dos partidas con la misma semilla
        List<Clima> first = weatherOfAYear(12345L);
        List<Clima> segundo = weatherOfAYear(12345L);

                // Entonces las secuencias de 112 días son idénticas día por día
        assertEquals(112, first.size());
        assertEquals(first, segundo);
        assertTrue(!first.equals(weatherOfAYear(54321L)), "a different seed should give a different year");
    }

    @Test
    void HU11_ca4_probabilidades_seguir_los_estacional_tablas() {
                // La Primavera nunca trae Drought, Frost ni Snow
        Map<Clima, Integer> primavera = draw(new EstrategiaClimaEstacional(Estacion.SPRING), 10);
        assertEquals(0, primavera.get(Clima.DROUGHT) + primavera.get(Clima.FROST) + primavera.get(Clima.SNOWY));

        // Winter never brings Rain
        Map<Clima, Integer> invierno = draw(new EstrategiaClimaEstacional(Estacion.WINTER), 10);
        assertEquals(0, invierno.get(Clima.RAINY));
        assertTrue(invierno.get(Clima.SNOWY) > 0);

                // El Otoño tiene Frost solo los días 22 a 28
        EstrategiaClimaEstacional otonio = new EstrategiaClimaEstacional(Estacion.AUTUMN);
        assertEquals(0, draw(otonio, 21).get(Clima.FROST));
        assertTrue(draw(otonio, 22).get(Clima.FROST) > 0);
        assertTrue(draw(otonio, 28).get(Clima.FROST) > 0);

                // El Verano es Sunny cerca del 60 % del tiempo (±3 %)
        double soleado = draw(new EstrategiaClimaEstacional(Estacion.SUMMER), 10).get(Clima.SUNNY) / (double) DIBUJA;
        assertTrue(soleado >= 0.57 && soleado <= 0.63, "sunny ratio in Summer: " + soleado);
    }

    @Test
    void HU11_everyWeatherTableAddsUpTo100Percent() {
        for (int[] tabla : new int[][] {ReglasJuego.PRIMAVERA_PESOS_CLIMA, ReglasJuego.VERANO_PESOS_CLIMA,
                ReglasJuego.OTONIO_PESOS_CLIMA_TEMPRANO, ReglasJuego.TARDE_OTONIO_PESOS_CLIMA,
                ReglasJuego.INVIERNO_PESOS_CLIMA}) {
            assertEquals(Clima.values().length, tabla.length);
            int sum = 0;
            for (int peso : tabla) {
                sum += peso;
            }
            assertEquals(100, sum);
        }
    }

    @Test
    void HU11_aFixedStrategyAlwaysReturnsTheSameWeather() {
        EstrategiaClima fixed = new EstrategiaClimaFija(Clima.DROUGHT);
        for (int dia = 1; dia <= ReglasJuego.DIAS_POR_ESTACION; dia++) {
            assertEquals(Clima.DROUGHT, fixed.climaSiguiente(dia, new AzarEstado(dia)));
        }
    }

    @Test
    void HU11_aTestStrategyOverridesTheSeasonalOne() {
        CicloClima clima = CicloClima.iniciarJuegoNuevo(reloj, new AzarEstado(1), new EstrategiaClimaFija(Clima.FROST));
        assertEquals(Clima.FROST, clima.pronostico());
    }

    @Test
    void RNF08_estado_azar_restaura_exactamente_la_mismo_secuencia() {
        AzarEstado original = new AzarEstado(99);
        original.nextInt(100);
        long archivado = original.estado();
        int esperadoSiguiente = original.nextInt(100);

        AzarEstado restaurado = new AzarEstado(0);
        restaurado.restaurar(archivado);

        assertEquals(esperadoSiguiente, restaurado.nextInt(100));
        assertEquals(original.nextInt(1000), restaurado.nextInt(1000));
    }

    @Test
    void RNF08_mismo_semilla_da_la_mismo_secuencia() {
        AzarEstado a = new AzarEstado(7);
        AzarEstado b = new AzarEstado(7);
        for (int i = 0; i < 50; i++) {
            assertEquals(a.nextInt(1000), b.nextInt(1000));
        }
    }

    private List<Clima> weatherOfAYear(long semilla) {
        reloj.reiniciarParaTests();
        AzarEstado azar = new AzarEstado(semilla);
        CicloClima ciclo = CicloClima.iniciarJuegoNuevo(reloj, azar);
        List<Clima> anio = new ArrayList<>();
        anio.add(ciclo.hoy());
        for (int dia = 2; dia <= 112; dia++) {
            reloj.diaAvanzar();
            ciclo.avanzar(reloj, azar);
            anio.add(ciclo.hoy());
        }
        return anio;
    }

    private static Map<Clima, Integer> draw(EstrategiaClima estrategia, int dia) {
        AzarEstado azar = new AzarEstado(42);
        Map<Clima, Integer> conteos = new EnumMap<>(Clima.class);
        for (Clima clima : Clima.values()) {
            conteos.put(clima, 0);
        }
        for (int i = 0; i < DIBUJA; i++) {
            conteos.merge(estrategia.climaSiguiente(dia, azar), 1, Integer::sum);
        }
        return conteos;
    }

    @Test
    void HU11_fecha_de_el_pronostico_es_el_dia_despues_el_actual_uno() {
        reloj.restaurar(new FechaJuego(1, Estacion.WINTER, ReglasJuego.DIAS_POR_ESTACION));
                // El pronóstico del último día de Invierno se sortea con la tabla de Primavera del año siguiente
        CicloClima ciclo = CicloClima.iniciarJuegoNuevo(reloj, new AzarEstado(5));
        assertTrue(CLIMA_PRIMAVERA.contains(ciclo.pronostico()));
    }
}
