package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.interfaz.componentes.CajaTutorial;
import edu.ucc.farmsim.interfaz.componentes.CategoriaAviso;
import edu.ucc.farmsim.interfaz.componentes.ColaAviso;
import edu.ucc.farmsim.interfaz.componentes.PresentadorAviso;
import edu.ucc.farmsim.interfaz.componentes.PresentadorTutorial;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-49 en pantalla: el cuadro del avatar con el paso, qué hacer y el botón para saltarlo. */
class HU49CajaTutorialTest {

    private static AplicacionTest juegoNuevo() {
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        return aplicacion;
    }

    private static CajaTutorial caja(AplicacionTest aplicacion) {
        CajaTutorial caja = new CajaTutorial(new PresentadorTutorial(aplicacion.controlador));
        aplicacion.bus.suscribir(caja);
        caja.refrescar();
        return caja;
    }

    @Test
    void HU49_ca1_aNewGameShowsTheAvatarsBoxWithStepOneAndTheSkipButton() {
        AplicacionTest aplicacion = juegoNuevo();

        CajaTutorial caja = caja(aplicacion);

        assertTrue(caja.isVisible());
        assertEquals("Paso 1 de 5", caja.textoPaso());
        assertTrue(caja.textoMensaje().contains("flechas"));
        assertEquals(Textos.TUTORIAL_SALTAR, caja.botonSaltar().getText());
        assertTrue(caja.tieneRetrato(), "the avatar is the one who speaks");
        Dimension size = caja.getPreferredSize();
        assertTrue(size.width <= CajaTutorial.ANCHO_MAXIMO && size.height <= CajaTutorial.ALTO_MAXIMO,
                "it fits next to the top bar at 1024 x 768: " + size);
    }

    @Test
    void HU49_ca2_theBoxFollowsTheStepsAndDisappearsWithANoticeAtTheEnd() {
        AplicacionTest aplicacion = juegoNuevo();
        CajaTutorial caja = caja(aplicacion);
        ColaAviso cola = new ColaAviso(new AtomicLong(0)::get);
        aplicacion.bus.suscribir(new PresentadorAviso(cola));

        aplicacion.controlador.mover("EAST");
        assertEquals("Paso 2 de 5", caja.textoPaso());
        assertTrue(caja.textoMensaje().startsWith("Elige Sembrar"));
        aplicacion.controlador.plantar(8, 6, "CARROT_SEED");
        aplicacion.controlador.agua(8, 6);
        assertEquals("Paso 4 de 5", caja.textoPaso());
        aplicacion.estado().avatar().restaurarDesde(new edu.ucc.farmsim.dominio.granja.InstantaneaAvatar(28, 11,
                edu.ucc.farmsim.dominio.granja.Direccion.EAST));
        aplicacion.controlador.hablar("MARTA");
        assertTrue(cola.visible().isEmpty(), "no notice before the end");
        aplicacion.controlador.diaFin();

        assertFalse(caja.isVisible());
        assertEquals(1, cola.visible().stream().filter(t -> t.categoria() == CategoriaAviso.INFO
                && t.texto().equals("¡Listo! Ya sabes lo básico. ¡Buena suerte en el valle!")).count());
    }

    @Test
    void HU49_ca4_theSkipButtonHidesTheBoxWithoutANoticeAndANewGameShowsItAgain() {
        AplicacionTest aplicacion = juegoNuevo();
        CajaTutorial caja = caja(aplicacion);
        ColaAviso cola = new ColaAviso(new AtomicLong(0)::get);
        aplicacion.bus.suscribir(new PresentadorAviso(cola));

        caja.botonSaltar().doClick();

        assertFalse(caja.isVisible());
        assertTrue(aplicacion.controlador.tutorial().value().concluido());
        assertTrue(cola.visible().isEmpty(), "skipping is not celebrated");
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        assertTrue(caja.isVisible());
        assertEquals("Paso 1 de 5", caja.textoPaso());
    }

    @Test
    void HU49_ca5_aLoadedGameWithTheTutorialFinishedOrNoGameAtAllShowNoBox() {
        AplicacionTest ninguno = AplicacionTest.create();
        assertFalse(caja(ninguno).isVisible(), "without a game there is nothing to explain");

        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        CajaTutorial caja = caja(aplicacion);
        assertFalse(caja.isVisible());
        caja.onEvento(new EventoJuego.MapaCambiado());
        assertFalse(caja.isVisible());
    }
}
