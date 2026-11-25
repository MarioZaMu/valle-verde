package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DialogosGrabacion;
import edu.ucc.farmsim.dominio.granja.Direccion;
import edu.ucc.farmsim.dominio.granja.InstantaneaAvatar;
import edu.ucc.farmsim.interfaz.componentes.Herramienta;
import edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta;
import edu.ucc.farmsim.interfaz.dibujo.DibujanteMapa;
import edu.ucc.farmsim.interfaz.dibujo.EfectosVisuales;
import edu.ucc.farmsim.interfaz.dibujo.GeometriaMapa;
import edu.ucc.farmsim.interfaz.pantallas.PresentadorPantallaJuego;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-45 · CA-3 y CA-5: la herramienta frente al avatar y la animación de caminata, sin pantalla. */
class HU45CaminarAvatarTest {

    private static final int W = 1024;
    private static final int H = 640;

    private final List<String> mensajes = new ArrayList<>();
    private final ModeloHerramienta herramientas = new ModeloHerramienta();
    private final AtomicLong now = new AtomicLong(10_000);

    private PresentadorPantallaJuego presentador(AplicacionTest aplicacion) {
        return new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add, new DialogosGrabacion());
    }

    private static VistaMapa campo(VistaAvatar avatar) {
        List<VistaParcela> parcelas = new ArrayList<>();
        for (int fila = 0; fila < 10; fila++) {
            for (int col = 0; col < 16; col++) {
                parcelas.add(new VistaParcela(col, fila, "GRASS", null, null, false, 0));
            }
        }
        return new VistaMapa(16, 10, parcelas, List.of(), avatar);
    }

    private static int[] celdaPintar(DibujanteMapa dibujante, VistaMapa map, EfectosVisuales fx, int col, int fila) {
        BufferedImage imagen = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        dibujante.renderizar(g, map, W, H, null, fx);
        g.dispose();
        Rectangle c = GeometriaMapa.ajustar(16, 10, W, H).limitesCelda(col, fila);
        return imagen.getRGB(c.x, c.y, c.width, c.height, null, 0, c.width);
    }

    @Test
    void HU45_ca3_spaceUsesTheToolOnThePlotInFrontAndAClickStillWorksAnywhere() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        assertTrue(aplicacion.controlador.plantar(7, 6, "CARROT_SEED").isOk());
        assertTrue(aplicacion.controlador.plantar(10, 3, "CARROT_SEED").isOk());
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(7, 7, Direccion.NORTH));
        PresentadorPantallaJuego presentador = presentador(aplicacion);
        herramientas.seleccionar(Herramienta.WATER);

        presentador.onHerramientaUsar();

        assertTrue(aplicacion.controlador.inspeccionar(7, 6).value().regado(), "the crop in front was watered");
        assertFalse(aplicacion.controlador.inspeccionar(10, 3).value().regado());
        presentador.onParcelaClicada(10, 3);
        assertTrue(aplicacion.controlador.inspeccionar(10, 3).value().regado(), "a click needs no closeness");

        presentador.onHerramientaUsar();
        assertEquals("El cultivo ya está regado hoy.", mensajes.get(mensajes.size() - 1), "the same rules as always");
    }

    @Test
    void HU45_ca3_mirando_fuera_de_la_granja_espacio_dice_alli_es_nada_en_frente() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(12, 0, Direccion.NORTH));
        herramientas.seleccionar(Herramienta.WATER);

        presentador(aplicacion).onHerramientaUsar();

        assertEquals(Textos.NADA_EN_FRENTE, mensajes.get(mensajes.size() - 1));
    }

    @Test
    void HU45_ca2_theArrowsWalkAndABlockedStepShowsItsMessage() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PresentadorPantallaJuego presentador = presentador(aplicacion);

        assertTrue(presentador.onMover("EAST"));
        assertEquals("", mensajes.get(mensajes.size() - 1));
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(7, 8, Direccion.NORTH));
        assertFalse(presentador.onMover("SOUTH"));
        assertFalse(presentador.onMover("SOUTH"));

        assertEquals("No puedes pasar por ahí.", mensajes.get(mensajes.size() - 1));
        assertEquals("SOUTH", aplicacion.controlador.mapa().value().avatar().mirada());
    }

    @Test
    void HU45_ca5_aStepPlaysThreeFramesAndAtRestOnlyOneIsShown() {
        EfectosVisuales fx = new EfectosVisuales(now::get, new Random(1));
        fx.onMapa(campo(new VistaAvatar(7, 7, "SOUTH")));
        assertFalse(fx.esCaminando());
        assertEquals(0, fx.marcoAvatar());
        assertNull(fx.avatarDesde());
        assertFalse(fx.marcosNecesita(), "nothing moves at rest");

        fx.onMapa(campo(new VistaAvatar(8, 7, "EAST")));

        assertTrue(fx.esCaminando());
        assertTrue(fx.marcosNecesita());
        assertArrayEquals(new int[] {7, 7}, fx.avatarDesde());
        Set<Integer> marcos = new HashSet<>();
        long start = now.get();
        for (long t = 0; t < EfectosVisuales.MS_PASO; t += 10) {
            now.set(start + t);
            marcos.add(fx.marcoAvatar());
        }
        assertEquals(Set.of(0, 1, 2), marcos);
        now.set(start + EfectosVisuales.MS_PASO);
        assertFalse(fx.esCaminando());
        assertEquals(0, fx.marcoAvatar());
        assertEquals(1.0, fx.progresoAvatar());
        assertFalse(fx.marcosNecesita());

        fx.onMapa(campo(new VistaAvatar(2, 2, "EAST")));
        assertFalse(fx.esCaminando(), "a jump (a loaded game) is not a walk");
        fx.onMapa(campo(new VistaAvatar(2, 3, "SOUTH")));
        fx.setEnabled(false);
        assertFalse(fx.esCaminando(), "with the effects off nothing moves");
        assertEquals(0, fx.marcoAvatar());
    }

    @Test
    void HU45_ca5_el_avatar_es_dibujado_y_su_caminar_cambia_el_imagen_pero_no_el_juego() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        VistaAvatar antes = aplicacion.controlador.mapa().value().avatar();
        for (GestorRecurso recursos : List.of(GestorRecurso.desdeClasspath(), GestorRecurso.soloVector())) {
            DibujanteMapa dibujante = new DibujanteMapa(recursos);
            assertFalse(Arrays.equals(celdaPintar(dibujante, campo(null), null, 7, 7),
                    celdaPintar(dibujante, campo(new VistaAvatar(7, 7, "SOUTH")), null, 7, 7)), "the avatar is drawn");
        }
        DibujanteMapa dibujante = new DibujanteMapa();
        assertFalse(Arrays.equals(celdaPintar(dibujante, campo(new VistaAvatar(7, 7, "SOUTH")), null, 7, 7),
                celdaPintar(dibujante, campo(new VistaAvatar(7, 7, "EAST")), null, 7, 7)), "it looks where it faces");

        EfectosVisuales fx = new EfectosVisuales(now::get, new Random(1));
        fx.onMundo("SPRING", "SUNNY");
        fx.onMapa(campo(new VistaAvatar(7, 7, "EAST")));
        fx.onMapa(campo(new VistaAvatar(8, 7, "EAST")));
        long start = now.get();
        int[] leaving = celdaPintar(dibujante, campo(new VistaAvatar(8, 7, "EAST")), fx, 8, 7);
        now.set(start + EfectosVisuales.MS_PASO * 2 / 3);
        int[] arriving = celdaPintar(dibujante, campo(new VistaAvatar(8, 7, "EAST")), fx, 8, 7);
        now.set(start + EfectosVisuales.MS_PASO * 5);
        int[] resto = celdaPintar(dibujante, campo(new VistaAvatar(8, 7, "EAST")), fx, 8, 7);
        now.set(start + EfectosVisuales.MS_PASO * 9);

        assertFalse(Arrays.equals(leaving, arriving), "it moves across the plot");
        assertTrue(Arrays.equals(resto, celdaPintar(dibujante, campo(new VistaAvatar(8, 7, "EAST")), fx, 8, 7)),
                "at rest the picture stays still");
        assertEquals(antes, aplicacion.controlador.mapa().value().avatar(), "drawing never touches the game");
    }
}
