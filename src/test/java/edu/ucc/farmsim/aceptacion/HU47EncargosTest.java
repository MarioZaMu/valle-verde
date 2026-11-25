package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.dominio.juego.ValidadorEstadoJuego;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.infraestructura.persistencia.Checksum;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-47: los encargos de las personas del valle (RN-38). */
class HU47EncargosTest {

    private static AplicacionTest siguienteAMarta(int zanahorias) {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        HU46ConversacionNpcTest.siguienteA(aplicacion, Npc.MARTA);
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, zanahorias);
        aplicacion.eventos.clear();
        return aplicacion;
    }

    @Test
    void HU47_ca1_entregando_cinco_zanahorias_a_marta_paga_sesenta_y_hace_el_encargo() {
        AplicacionTest aplicacion = siguienteAMarta(5);

        Resultado<VistaConversacion> resultado = aplicacion.controlador.entregar("MARTA");

        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(0, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
        assertEquals(560, aplicacion.estado().dinero());
        assertTrue(aplicacion.estado().encargos().isDone(Npc.MARTA));
        assertTrue(resultado.value().encargoHecho());
        assertEquals("¡Gracias otra vez por traerme 5 Zanahorias!", resultado.value().encargo());
        assertTrue(aplicacion.eventos.containsAll(List.of(new EventoJuego.InventarioCambiado(), new EventoJuego.DineroCambiado(560),
                new EventoJuego.EncargoCompletado("MARTA", "Marta", 60))), aplicacion.eventos.toString());
        assertTrue(aplicacion.controlador.npcs().value().get(0).encargoHecho());
    }

    @Test
    void HU47_ca1_los_otro_encargos_pedir_para_su_propio_articulos() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        int dinero = aplicacion.estado().dinero();
        for (Npc npc : List.of(Npc.TOMAS, Npc.LUCIA, Npc.INES)) {
            HU46ConversacionNpcTest.siguienteA(aplicacion, npc);
            aplicacion.estado().inventario().add(npc.articuloEncargo(), npc.montoEncargo());
            assertTrue(aplicacion.controlador.entregar(npc.name()).isOk(), npc.name());
            dinero += npc.recompensaEncargo();
        }
        assertEquals(500 + 50 + 30 + 80, dinero);
        assertEquals(dinero, aplicacion.estado().dinero());
    }

    @Test
    void HU47_ca2_con_solo_tres_zanahorias_la_entrega_es_rechazado_y_nada_cambia() {
        AplicacionTest aplicacion = siguienteAMarta(3);
        MementoJuego antes = aplicacion.estado().mementoCrear();

        Resultado<VistaConversacion> resultado = aplicacion.controlador.entregar("MARTA");

        assertEquals(CodigoError.ENCARGO_INCOMPLETO, resultado.codigo());
        assertEquals("Te faltan 2 Zanahorias para el encargo.", resultado.mensaje());
        assertEquals(antes, aplicacion.estado().mementoCrear());
        assertTrue(aplicacion.eventos.isEmpty());
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 1);
        assertEquals("Te faltan 1 Zanahoria para el encargo.", aplicacion.controlador.entregar("MARTA").mensaje());
        aplicacion.estado().avatar().restaurarDesde(new edu.ucc.farmsim.dominio.granja.InstantaneaAvatar(7, 7,
                edu.ucc.farmsim.dominio.granja.Direccion.SOUTH));
        assertEquals(CodigoError.NPC_LEJOS, aplicacion.controlador.entregar("MARTA").codigo());
    }

    @Test
    void HU47_ca3_aDoneErrandIsThankedAndNeverPaidAgainEvenAfterSavingAndLoading() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        HU46ConversacionNpcTest.siguienteA(aplicacion, Npc.MARTA);
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 10);
        assertTrue(aplicacion.controlador.entregar("MARTA").isOk());

        Resultado<VistaConversacion> otraVez = aplicacion.controlador.entregar("MARTA");

        assertTrue(otraVez.isOk());
        assertEquals(560, aplicacion.estado().dinero(), "not paid twice");
        assertEquals(5, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
        assertEquals("¡Gracias otra vez por traerme 5 Zanahorias!", aplicacion.controlador.hablar("MARTA").value().encargo());

        assertTrue(aplicacion.controlador.guardar(1).isOk());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        assertFalse(aplicacion.estado().encargos().isDone(Npc.MARTA));
        assertTrue(aplicacion.controlador.cargar(1).isOk());
        assertTrue(aplicacion.estado().encargos().isDone(Npc.MARTA));
    }

    @Test
    void HU47_ca3_aSaveWrittenBeforeTheErrandsLoadsWithNoneDoneAndABrokenListIsRejected() throws Exception {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.estado().encargos().complete(Npc.INES);
        aplicacion.controlador.guardar(2);
        mongo.manipular(2, d -> {
            try {
                ObjectMapper json = new ObjectMapper();
                ObjectNode estado = (ObjectNode) json.readTree(d.estado());
                estado.remove("encargosHechos");
                String texto = json.writeValueAsString(estado);
                return new DocumentoGuardado(d.id(), d.versionEsquema(), d.archivadoEn(), texto, Checksum.of(texto));
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });

        assertTrue(aplicacion.controlador.cargar(2).isOk());
        assertFalse(aplicacion.estado().encargos().isDone(Npc.INES));

        MementoJuego bueno = aplicacion.estado().mementoCrear();
        MementoJuego dosVeces = new MementoJuego(bueno.fecha(), bueno.dinero(), bueno.energia(), bueno.inventario(), bueno.granja(),
                bueno.hoy(), bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(), bueno.progreso(),
                bueno.avatar(), List.of(Npc.MARTA, Npc.MARTA));
        MementoJuego empty = new MementoJuego(bueno.fecha(), bueno.dinero(), bueno.energia(), bueno.inventario(), bueno.granja(),
                bueno.hoy(), bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(), bueno.progreso(),
                bueno.avatar(), Arrays.asList((Npc) null));
        assertFalse(ValidadorEstadoJuego.violaciones(dosVeces).isEmpty());
        assertFalse(ValidadorEstadoJuego.violaciones(empty).isEmpty());
    }

    @Test
    void HU47_ca4_la_recompensa_es_ingresos_en_el_resumen_de_el_dia() {
        AplicacionTest aplicacion = siguienteAMarta(5);
        aplicacion.controlador.entregar("MARTA");

        aplicacion.controlador.diaFin();

        VistaResumenDia resumen = aplicacion.eventos.stream().filter(EventoJuego.ResumenDiaListo.class::isInstance)
                .map(e -> ((EventoJuego.ResumenDiaListo) e).resumen()).findFirst().orElseThrow();
        assertEquals(60, resumen.ingresos());
        assertEquals(500, resumen.antesSaldo());
        assertEquals(560, resumen.despuesSaldo());
    }
}
