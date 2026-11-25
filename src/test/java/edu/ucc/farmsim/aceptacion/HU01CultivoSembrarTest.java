package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.cultivo.EtapaCultivo;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import org.junit.jupiter.api.Test;

/** HU-01: sembrar una semilla del inventario en una parcela de tierra libre (RN-02, RN-05, RN-06, RN-15). */
class HU01CultivoSembrarTest {

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    @Test
    void HU01_ca1_siembra_planta_el_cultivo_en_semilla_etapa_y_consume_uno_semilla_y_dos_energia() {
                // Dado Primavera, 10 semillas de zanahoria, 100 de energía y una parcela de tierra libre
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        EstadoJuego estado = aplicacion.estado();

                // Cuando el jugador siembra zanahoria allí
        Resultado<VistaParcela> resultado = aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED");

                // Entonces la parcela tiene una zanahoria en la etapa Seed con 0 días de crecimiento, quedan 9 semillas y la
        // energía es 98
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        Cultivo cultivo = estado.granja().parcelaEn(COL, FILA).cultivo();
        assertNotNull(cultivo);
        assertEquals(TipoCultivo.CARROT, cultivo.tipo());
        assertEquals(EtapaCultivo.SEED, cultivo.etapa());
        assertEquals(0, cultivo.diasCrecidos());
        assertEquals(9, estado.inventario().count(TipoArticulo.CARROT_SEED));
        assertEquals(98, estado.energia().actual());
        assertEquals("CARROT", resultado.value().cultivo());
        assertEquals("SEED", resultado.value().etapa());
    }

    @Test
    void HU01_ca2_siembra_fuera_de_estacion_es_rechazado_y_nada_cambia() {
                // Dado Primavera y 5 semillas de calabaza (la calabaza solo es válida en Otoño)
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        EstadoJuego estado = aplicacion.estado();
        estado.inventario().add(TipoArticulo.PUMPKIN_SEED, 5);

                // Cuando el jugador intenta sembrarlas
        Resultado<VistaParcela> resultado = aplicacion.controlador.plantar(COL, FILA, "PUMPKIN_SEED");

                // Entonces se rechaza con FUERA_DE_ESTACION, las semillas se conservan y la energía queda intacta
        assertFalse(resultado.isOk());
        assertEquals(CodigoError.FUERA_DE_ESTACION, resultado.codigo());
        assertEquals("No se puede sembrar Calabaza en Primavera: solo en Otoño.", resultado.mensaje());
        assertEquals(5, estado.inventario().count(TipoArticulo.PUMPKIN_SEED));
        assertEquals(100, estado.energia().actual());
        assertNull(estado.granja().parcelaEn(COL, FILA).cultivo());
    }

    @Test
    void HU01_ca2_aCropValidInTwoSeasonsNamesBothInTheMessage() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CORN_SEED, 1);

        Resultado<VistaParcela> resultado = aplicacion.controlador.plantar(COL, FILA, "CORN_SEED");

        assertEquals("No se puede sembrar Maíz en Primavera: solo en Verano y Otoño.", resultado.mensaje());
    }

    @Test
    void HU01_ca3_aPlotThatIsNotSoilOrIsAlreadyOccupiedIsRejected() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        EstadoJuego estado = aplicacion.estado();

                // Dada una parcela de pasto, sembrar se rechaza con PARCELA_NO_CULTIVABLE
        Resultado<VistaParcela> onHierba = aplicacion.controlador.plantar(1, 1, "CARROT_SEED");
        assertEquals(CodigoError.PARCELA_NO_CULTIVABLE, onHierba.codigo());

                // Y una parcela de tierra que ya tiene un cultivo se rechaza con PARCELA_OCUPADA
        assertTrue(aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED").isOk());
        Resultado<VistaParcela> onCultivo = aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED");
        assertEquals(CodigoError.PARCELA_OCUPADA, onCultivo.codigo());

                // Y ninguno de los dos fallos gastó una semilla ni energía (solo lo hizo la primera siembra exitosa)
        assertEquals(9, estado.inventario().count(TipoArticulo.CARROT_SEED));
        assertEquals(98, estado.energia().actual());
    }

    @Test
    void HU01_ca3_coordenadas_afuera_el_mapa_son_rechazado_sin_fallando() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        assertEquals(CodigoError.PARCELA_NO_CULTIVABLE, aplicacion.controlador.plantar(-1, 0, "CARROT_SEED").codigo());
        assertEquals(CodigoError.PARCELA_NO_CULTIVABLE, aplicacion.controlador.plantar(16, 3, "CARROT_SEED").codigo());
        assertEquals(CodigoError.PARCELA_NO_CULTIVABLE, aplicacion.controlador.plantar(3, 10, "CARROT_SEED").codigo());
    }

    @Test
    void HU01_ca4_siembra_sin_semillas_es_rechazado_y_la_parcela_queda_libre() {
                // Dado Verano y 0 semillas de maíz
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.fechaFijar(1, Estacion.SUMMER, 1);

                // Cuando el jugador intenta sembrar maíz
        Resultado<VistaParcela> resultado = aplicacion.controlador.plantar(COL, FILA, "CORN_SEED");

                // Entonces se rechaza con SIN_SEMILLAS y la parcela sigue libre
        assertEquals(CodigoError.SIN_SEMILLAS, resultado.codigo());
        assertEquals("No tienes semillas de Maíz.", resultado.mensaje());
        assertNull(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo());
    }

    @Test
    void HU08_ca2_siembra_sin_suficiente_energia_es_rechazado_con_sin_energia_y_nada_es_gastado() {
        // Given 1 energy (sowing costs 2)
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().energia().set(1);

                // Cuando el jugador intenta sembrar
        Resultado<VistaParcela> resultado = aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED");

                // Entonces se rechaza: «necesitas 2 y tienes 1»; la semilla y la energía quedan intactas
        assertEquals(CodigoError.SIN_ENERGIA, resultado.codigo());
        assertEquals("Energía insuficiente: necesitas 2 y tienes 1.", resultado.mensaje());
        assertEquals(1, aplicacion.estado().energia().actual());
        assertEquals(10, aplicacion.estado().inventario().count(TipoArticulo.CARROT_SEED));
    }

    @Test
    void HU34_ca2_sowingPublishesTheEnergyAndMapEventsSoTheScreenUpdatesWithoutPolling() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED");

        assertTrue(aplicacion.eventos.contains(new EventoJuego.EnergiaCambiada(98, 100)), aplicacion.eventos.toString());
        assertTrue(aplicacion.eventos.contains(new EventoJuego.MapaCambiado()), aplicacion.eventos.toString());
    }

    @Test
    void HU01_aRejectedSowingPublishesNoEvents() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        aplicacion.controlador.plantar(1, 1, "CARROT_SEED");

        assertTrue(aplicacion.eventos.isEmpty());
    }

    @Test
    void HU01_withoutAGameInProgressSowingIsRejectedWithSinPartida() {
        AplicacionTest aplicacion = AplicacionTest.create();

        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED").codigo());
    }

    @Test
    void HU01_la_fabrica_metodo_crea_cada_cultivo_tipo_en_semilla_etapa() {
        for (TipoCultivo tipo : TipoCultivo.values()) {
            Cultivo cultivo = edu.ucc.farmsim.dominio.cultivo.FabricaCultivo.paraTipo(tipo).cultivoCrear();
            assertEquals(tipo, cultivo.tipo());
            assertEquals(EtapaCultivo.SEED, cultivo.etapa());
            assertEquals(0, cultivo.diasCrecidos());
        }
    }

    @Test
    void HU01_cropTypesFollowTheCatalogOfRn05() {
        assertEquals(4, TipoCultivo.CARROT.diasAMaduro());
        assertEquals(6, TipoCultivo.POTATO.diasAMaduro());
        assertEquals(7, TipoCultivo.TOMATO.diasAMaduro());
        assertEquals(8, TipoCultivo.CORN.diasAMaduro());
        assertEquals(10, TipoCultivo.PUMPKIN.diasAMaduro());
        assertEquals(java.util.Set.of(Estacion.SPRING), TipoCultivo.CARROT.estacionesValidos());
        assertEquals(java.util.Set.of(Estacion.SUMMER, Estacion.AUTUMN), TipoCultivo.CORN.estacionesValidos());
        assertEquals(TipoArticulo.CARROT_SEED, TipoArticulo.semillaDe(TipoCultivo.CARROT));
        assertEquals(TipoArticulo.PUMPKIN, TipoArticulo.producirDe(TipoCultivo.PUMPKIN));
        assertEquals(TipoCultivo.TOMATO, TipoArticulo.TOMATO_SEED.tipoCultivo());
        assertNull(TipoArticulo.EGG.tipoCultivo());
    }
}
