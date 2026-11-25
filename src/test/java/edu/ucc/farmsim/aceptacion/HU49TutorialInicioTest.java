package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucc.farmsim.aplicacion.dto.VistaTutorial;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.granja.Direccion;
import edu.ucc.farmsim.dominio.granja.InstantaneaAvatar;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.infraestructura.persistencia.Checksum;
import edu.ucc.farmsim.infraestructura.persistencia.CodecGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-49: el tutorial de inicio en el que el avatar guía al jugador (RN-39). */
class HU49TutorialInicioTest {

    /** Una parcela de tierra al norte del inicio del avatar, a dos parcelas de él. */
    private static final int COL_TIERRA = 8;
    private static final int FILA_TIERRA = 6;

    /** Una partida nueva con su tutorial, como la recibe el jugador. */
    private static AplicacionTest juegoNuevo() {
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.eventos.clear();
        return aplicacion;
    }

    private static VistaTutorial tutorial(AplicacionTest aplicacion) {
        Resultado<VistaTutorial> resultado = aplicacion.controlador.tutorial();
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        return resultado.value();
    }

    private static List<VistaTutorial> avanza(AplicacionTest aplicacion) {
        return aplicacion.eventos.stream().filter(EventoJuego.TutorialAvanzado.class::isInstance)
                .map(e -> ((EventoJuego.TutorialAvanzado) e).tutorial()).toList();
    }

    /** Las cinco acciones del tutorial, en orden; cada una debe ser aceptada. */
    private static void jugar(AplicacionTest aplicacion, int acciones) {
        Runnable[] pasos = {
            () -> assertTrue(aplicacion.controlador.mover("EAST").isOk()),
            () -> assertTrue(aplicacion.controlador.plantar(COL_TIERRA, FILA_TIERRA, "CARROT_SEED").isOk()),
            () -> assertTrue(aplicacion.controlador.agua(COL_TIERRA, FILA_TIERRA).isOk()),
            () -> {
                HU46ConversacionNpcTest.siguienteA(aplicacion, Npc.MARTA);
                assertTrue(aplicacion.controlador.hablar("MARTA").isOk());
            },
            () -> assertTrue(aplicacion.controlador.diaFin().isOk()),
        };
        for (int i = 0; i < acciones; i++) {
            pasos[i].run();
        }
    }

    @Test
    void HU49_ca1_aNewGameStartsAtStepOneOfFiveAskingToWalk() {
        AplicacionTest aplicacion = juegoNuevo();

        VistaTutorial vista = tutorial(aplicacion);

        assertEquals("WALK", vista.paso());
        assertEquals(1, vista.numero());
        assertEquals(5, vista.total());
        assertFalse(vista.concluido());
        assertEquals("¡Hola! Soy tu granjero. Camina por la granja con las flechas o con W A S D.", vista.texto());
        assertEquals(CodigoError.SIN_PARTIDA, AplicacionTest.create().controlador.tutorial().codigo());
    }

    @Test
    void HU49_ca2_caminando_siembra_riego_hablando_y_terminando_el_dia_ir_por_el_cinco_pasos_en_orden() {
        AplicacionTest aplicacion = juegoNuevo();

        jugar(aplicacion, 5);

        List<VistaTutorial> visto = avanza(aplicacion);
        assertEquals(List.of("SOW", "WATER", "TALK", "END_DAY", "DONE"), visto.stream().map(VistaTutorial::paso).toList());
        assertEquals(List.of(2, 3, 4, 5, 5), visto.stream().map(VistaTutorial::numero).toList());
        assertEquals("Elige Sembrar con la tecla 1, ponte frente a la tierra y pulsa Espacio "
                + "(o haz clic en una parcela de tierra).", visto.get(0).texto());
        assertEquals("Ahora elige Regar con la tecla 2 y riega lo que sembraste.", visto.get(1).texto());
        assertEquals("Los vecinos viven en la aldea, al este por el camino. Acércate a uno y pulsa E.", visto.get(2).texto());
        assertEquals("Cuando acabes, pulsa Enter o «Terminar día» para que crezcan tus cultivos.", visto.get(3).texto());
        VistaTutorial hecho = tutorial(aplicacion);
        assertTrue(hecho.concluido());
        assertEquals("¡Listo! Ya sabes lo básico. ¡Buena suerte en el valle!", hecho.texto());
        assertEquals(visto.get(4), hecho);
    }

    @Test
    void HU49_ca2_el_paso_cambia_derecha_despues_los_eventos_de_la_accion_que_completado_eso() {
        AplicacionTest aplicacion = juegoNuevo();

        assertTrue(aplicacion.controlador.mover("EAST").isOk());

        assertEquals(2, aplicacion.eventos.size());
        assertTrue(aplicacion.eventos.get(0) instanceof EventoJuego.AvatarMovido);
        assertEquals("SOW", ((EventoJuego.TutorialAvanzado) aplicacion.eventos.get(1)).tutorial().paso());
    }

    @Test
    void HU49_ca3_actionsOfALaterStepAndRejectedActionsDoNotMoveTheTutorial() {
        AplicacionTest aplicacion = juegoNuevo();

        assertTrue(aplicacion.controlador.diaFin().isOk());
        assertTrue(aplicacion.controlador.plantar(COL_TIERRA, FILA_TIERRA, "CARROT_SEED").isOk());
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(7, 8, Direccion.NORTH));
        assertEquals(CodigoError.MOVIMIENTO_BLOQUEADO, aplicacion.controlador.mover("SOUTH").codigo(), "a tree at (7, 9)");

        assertEquals("WALK", tutorial(aplicacion).paso());
        assertTrue(avanza(aplicacion).isEmpty());

        assertTrue(aplicacion.controlador.mover("NORTH").isOk());
        assertFalse(aplicacion.controlador.plantar(0, 0, "CARROT_SEED").isOk(), "an obstacle");
        assertFalse(aplicacion.controlador.plantar(DatosJuego.COL_HIERBA, DatosJuego.FILA_HIERBA, "CARROT_SEED").isOk());
        assertEquals("SOW", tutorial(aplicacion).paso(), "a rejected sowing does not count");
    }

    @Test
    void HU49_ca4_omitiendo_termina_el_tutorial_en_cualquier_paso_y_eso_nunca_viene_atras() {
        AplicacionTest aplicacion = juegoNuevo();
        jugar(aplicacion, 2);

        Resultado<VistaTutorial> omitido = aplicacion.controlador.tutorialSaltar();

        assertTrue(omitido.isOk());
        assertTrue(omitido.value().concluido());
        assertEquals("SKIPPED", omitido.value().paso());
        assertEquals(omitido.value(), avanza(aplicacion).get(avanza(aplicacion).size() - 1));
        aplicacion.eventos.clear();
        play2to5(aplicacion);
        assertTrue(tutorial(aplicacion).concluido());
        assertTrue(avanza(aplicacion).isEmpty(), "nothing moves a finished tutorial");
        assertTrue(aplicacion.controlador.tutorialSaltar().isOk(), "skipping twice is harmless");
        assertTrue(avanza(aplicacion).isEmpty());
        assertEquals(CodigoError.SIN_PARTIDA, AplicacionTest.create().controlador.tutorialSaltar().codigo());
    }

    @Test
    void HU49_ca4_el_tutorial_nunca_cambia_energia_dinero_inventario_ni_el_dia() {
        AplicacionTest guiado = juegoNuevo();
        AplicacionTest omitido = juegoNuevo();
        omitido.controlador.tutorialSaltar();

        jugar(guiado, 5);
        jugar(omitido, 5);

        assertEquals(omitido.controlador.hud().value(), guiado.controlador.hud().value());
        assertEquals(omitido.controlador.inventario().value(), guiado.controlador.inventario().value());
        assertEquals(omitido.controlador.map().value(), guiado.controlador.map().value());
    }

    @Test
    void HU49_ca5_el_paso_sobrevive_guardando_y_cargando_y_un_mayor_guardado_carga_con_el_tutorial_concluido() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        jugar(aplicacion, 2);
        assertEquals("WATER", tutorial(aplicacion).paso());
        assertTrue(aplicacion.controlador.save(1).isOk());
        aplicacion.controlador.save(2);
        mongo.manipular(2, HU49TutorialInicioTest::sinTutorial);
        assertEquals(CodecGuardado.VERSION_ESQUEMA, mongo.documento(2).versionEsquema(), "the version did not change");

        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED + 1);
        assertEquals("WALK", tutorial(aplicacion).paso());
        assertTrue(aplicacion.controlador.cargar(1).isOk());
        assertEquals("WATER", tutorial(aplicacion).paso());
        assertEquals(3, tutorial(aplicacion).numero());

        Resultado<?> mayor = aplicacion.controlador.cargar(2);
        assertTrue(mayor.isOk(), String.valueOf(mayor.mensaje()));
        assertTrue(tutorial(aplicacion).concluido(), "a game that was already being played does not repeat it");
    }

    private static void play2to5(AplicacionTest aplicacion) {
        assertTrue(aplicacion.controlador.agua(COL_TIERRA, FILA_TIERRA).isOk());
        HU46ConversacionNpcTest.siguienteA(aplicacion, Npc.MARTA);
        assertTrue(aplicacion.controlador.hablar("MARTA").isOk());
        assertTrue(aplicacion.controlador.diaFin().isOk());
    }

    /** El mismo documento que una partida escrita antes de que existiera el tutorial: sin tutorial en el estado. */
    private static DocumentoGuardado sinTutorial(DocumentoGuardado documento) {
        try {
            ObjectMapper json = new ObjectMapper();
            ObjectNode estado = (ObjectNode) json.readTree(documento.estado());
            assertTrue(estado.has("tutorial"));
            estado.remove("tutorial");
            String texto = json.writeValueAsString(estado);
            return new DocumentoGuardado(documento.id(), documento.versionEsquema(), documento.archivadoEn(), texto,
                    Checksum.of(texto));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
