package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.economia.Mercado;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.granja.Direccion;
import edu.ucc.farmsim.dominio.granja.InstantaneaAvatar;
import edu.ucc.farmsim.dominio.granja.MapaGranja;
import edu.ucc.farmsim.dominio.granja.Terreno;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-46: hablar con las personas del valle (RN-37). */
class HU46ConversacionNpcTest {

    /**
     * Pone al avatar en la parcela al oeste de la persona, mirándola (al sur de ella para Lucía, al este para Inés).
     */
    static void siguienteA(AplicacionTest aplicacion, Npc npc) {
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(npc.col() - 1, npc.fila(), Direccion.EAST));
    }

    private static String decir(AplicacionTest aplicacion, Npc npc) {
        Resultado<VistaConversacion> resultado = aplicacion.controlador.hablar(npc.name());
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        return resultado.value().texto();
    }

    @Test
    void HU46_ca1_siguiente_a_marta_ella_habla_sobre_precios_en_espaniol() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        siguienteA(aplicacion, Npc.MARTA);

        Resultado<VistaConversacion> resultado = aplicacion.controlador.hablar("MARTA");

        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        VistaConversacion conversacion = resultado.value();
        assertEquals("Marta", conversacion.name());
        assertEquals("Vendedora del mercado", conversacion.rol());
        assertTrue(conversacion.texto().startsWith("¡Bienvenido al mercado! Hoy lo mejor pagado es "), conversacion.texto());
        assertTrue(conversacion.texto().contains(" m por unidad."), conversacion.texto());
        assertEquals("Encargo: tráeme 5 Zanahorias y te pago 60 m.", conversacion.encargo());
        assertFalse(conversacion.encargoHecho());
    }

    @Test
    void HU46_ca1_cada_persona_habla_sobre_su_propio_sujeto() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        siguienteA(aplicacion, Npc.TOMAS);
        assertEquals("En Primavera conviene sembrar Zanahoria y Papa.", decir(aplicacion, Npc.TOMAS));
        siguienteA(aplicacion, Npc.LUCIA);
        assertEquals("Aún no tienes animales. Un Gallinero es un buen comienzo.", decir(aplicacion, Npc.LUCIA));
        siguienteA(aplicacion, Npc.INES);
        assertTrue(decir(aplicacion, Npc.INES).startsWith("El pronóstico para mañana es "));
        assertEquals("Dra. Lucía", aplicacion.controlador.npcs().value().get(2).name());
    }

    @Test
    void HU46_ca2_desde_lejos_lejos_el_hablar_es_rechazado_con_el_nombre() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        for (Npc npc : Npc.values()) {
            Resultado<VistaConversacion> resultado = aplicacion.controlador.hablar(npc.name());

            assertEquals(CodigoError.NPC_LEJOS, resultado.codigo());
        }
        assertEquals("Acércate a Don Tomás para hablar.", aplicacion.controlador.hablar("TOMAS").mensaje());
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(Npc.MARTA.col() - 1, Npc.MARTA.fila() + 1, Direccion.EAST));
        assertEquals(CodigoError.NPC_LEJOS, aplicacion.controlador.hablar("MARTA").codigo(), "a diagonal is not next to");
        assertEquals(CodigoError.SIN_PARTIDA, AplicacionTest.create().controlador.hablar("MARTA").codigo());
        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.hablar("NOBODY").codigo());
    }

    @Test
    void HU46_ca3_el_mismo_estado_siempre_da_los_mismo_palabras_y_ellos_seguir_el_juego() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        siguienteA(aplicacion, Npc.TOMAS);
        String primavera = decir(aplicacion, Npc.TOMAS);
        assertEquals(primavera, decir(aplicacion, Npc.TOMAS));
        DatosJuego.fechaFijar(1, Estacion.SUMMER, 1);
        assertNotEquals(primavera, decir(aplicacion, Npc.TOMAS), "the season");
        String verano = decir(aplicacion, Npc.TOMAS);
        aplicacion.estado().clima().restaurar(Clima.DROUGHT, Clima.SUNNY);
        assertEquals(verano + " Con la Sequía de hoy, regar cuesta el doble.", decir(aplicacion, Npc.TOMAS), "the weather");

        siguienteA(aplicacion, Npc.INES);
        aplicacion.estado().clima().restaurar(Clima.SUNNY, Clima.RAINY);
        assertEquals("El pronóstico para mañana es Lluvioso. No hará falta regar.", decir(aplicacion, Npc.INES));
        aplicacion.estado().clima().restaurar(Clima.SUNNY, Clima.FROST);
        assertTrue(decir(aplicacion, Npc.INES).contains("Helada"), "the forecast");

        siguienteA(aplicacion, Npc.LUCIA);
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.gallina(gallinero);
        assertEquals("1 animal aún no ha comido hoy: dale Heno.", decir(aplicacion, Npc.LUCIA));
        DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.SICK, 2, false, 0);
        assertEquals("Tienes 1 animal enfermo: cúralo con Medicina antes de que empeore.", decir(aplicacion, Npc.LUCIA),
                "the sick animals");

        siguienteA(aplicacion, Npc.MARTA);
        String precios = decir(aplicacion, Npc.MARTA);
        for (TipoArticulo articulo : Mercado.articulosVendibles()) {
            DatosJuego.demandaFijar(aplicacion.estado(), articulo, ReglasJuego.DEMANDA_MINIMA);
        }
        assertNotEquals(precios, decir(aplicacion, Npc.MARTA), "the prices");
    }

    @Test
    void HU46_ca4_nobodyCanWalkThroughAPersonNorBuildOnItAndTheyStandOnFreeGrass() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        MapaGranja granja = MapaGranja.inicial();
        for (Npc npc : Npc.values()) {
            assertEquals(Terreno.GRASS, granja.parcelaEn(npc.col(), npc.fila()).terreno(), npc.name());
            assertFalse(granja.esTransitable(npc.col(), npc.fila()));
            assertNull(granja.edificioEn(npc.col(), npc.fila()));
            assertEquals(npc, Npc.at(npc.col(), npc.fila()));
        }
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(Npc.MARTA.col() - 1, Npc.MARTA.fila(), Direccion.NORTH));

        assertEquals(CodigoError.MOVIMIENTO_BLOQUEADO, aplicacion.controlador.mover("EAST").codigo());
        assertEquals("EAST", aplicacion.controlador.mapa().value().avatar().mirada());
        DatosJuego.dineroFijar(aplicacion.estado(), 5_000);
        assertEquals(CodigoError.UBICACION_INVALIDA,
                aplicacion.controlador.construir("COOP", Npc.INES.col(), Npc.INES.fila()).codigo());
        List<VistaNpc> dibujado = aplicacion.controlador.mapa().value().npcs();
        assertEquals(4, dibujado.size());
        assertEquals(new VistaNpc("INES", "Inés", "Vecina", 37, 17, "EGG", 4, 80, false), dibujado.get(3));
    }

    @Test
    void HU46_ca5_hablando_cuesta_no_energia_ni_tiempo_y_cambia_nada() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        siguienteA(aplicacion, Npc.TOMAS);
        VistaHud antes = aplicacion.controlador.hud().value();
        var archivado = aplicacion.estado().mementoCrear();

        aplicacion.controlador.hablar("TOMAS");
        aplicacion.controlador.npcs();

        assertEquals(antes, aplicacion.controlador.hud().value());
        assertEquals(archivado, aplicacion.estado().mementoCrear());
        assertTrue(aplicacion.eventos.isEmpty());
    }
}
