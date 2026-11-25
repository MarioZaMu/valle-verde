package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DialogosGrabacion;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.granja.Direccion;
import edu.ucc.farmsim.dominio.granja.InstantaneaAvatar;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.interfaz.componentes.Herramienta;
import edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta;
import edu.ucc.farmsim.interfaz.componentes.PresentadorConversacion;
import edu.ucc.farmsim.interfaz.dibujo.DibujanteMapa;
import edu.ucc.farmsim.interfaz.dibujo.GeometriaMapa;
import edu.ucc.farmsim.interfaz.pantallas.PresentadorPantallaJuego;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-46 y HU-47 en pantalla: E o un clic abre el diálogo, que entrega el encargo; el mapa las dibuja. */
class HU46PantallaNpcTest {

    private static final int W = 1024;
    private static final int H = 640;

    private final List<String> mensajes = new ArrayList<>();
    private final DialogosGrabacion dialogos = new DialogosGrabacion();
    private final ModeloHerramienta herramientas = new ModeloHerramienta();

    private PresentadorPantallaJuego presentador(AplicacionTest aplicacion) {
        return new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add, dialogos);
    }

    @Test
    void HU46_ca1_eNextToAPersonOpensItsDialogueAndFarAwayItSaysNobodyIsNear() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PresentadorPantallaJuego presentador = presentador(aplicacion);

        presentador.onClaveHablar();
        assertEquals(Textos.NADIE_A_HABLAR, mensajes.get(mensajes.size() - 1));
        assertTrue(dialogos.abierto.isEmpty());

        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(Npc.MARTA.col() - 1, Npc.MARTA.fila(), Direccion.SOUTH));
        presentador.onClaveHablar();

        assertEquals(List.of("dialogue:MARTA"), dialogos.abierto);
        assertEquals("Marta", dialogos.conversaciones.get(0).name());
    }

    @Test
    void HU46_ca2_aClickOnAPersonTalksToItWhateverTheToolAndFromFarAwaySaysToComeCloser() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        herramientas.seleccionar(Herramienta.WATER);
        PresentadorPantallaJuego presentador = presentador(aplicacion);

        presentador.onParcelaClicada(Npc.TOMAS.col(), Npc.TOMAS.fila());
        assertEquals("Acércate a Don Tomás para hablar.", mensajes.get(mensajes.size() - 1));

        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(Npc.TOMAS.col(), Npc.TOMAS.fila() + 1, Direccion.NORTH));
        presentador.onParcelaClicada(Npc.TOMAS.col(), Npc.TOMAS.fila());
        assertEquals(List.of("dialogue:TOMAS"), dialogos.abierto);
    }

    @Test
    void HU47_ca1_la_conversacion_entrega_el_encargo_y_luego_solo_agradece() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(Npc.MARTA.col() - 1, Npc.MARTA.fila(), Direccion.EAST));
        PresentadorConversacion conversacion = new PresentadorConversacion(aplicacion.controlador, aplicacion.controlador.hablar("MARTA").value());
        assertTrue(conversacion.puedeEntregar());

        assertEquals("Te faltan 5 Zanahorias para el encargo.", conversacion.entregar());
        assertTrue(conversacion.puedeEntregar());
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 5);
        assertEquals(Textos.ENCARGO_ENTREGADO, conversacion.entregar());

        assertFalse(conversacion.puedeEntregar());
        assertEquals("¡Gracias otra vez por traerme 5 Zanahorias!", conversacion.vista().encargo());
        assertEquals(Textos.ENCARGO_ENTREGADO, conversacion.mensaje());
    }

    @Test
    void HU47_ca1_thePeopleAreDrawnWithABubbleWhileTheirErrandIsPending() {
        for (GestorRecurso recursos : List.of(GestorRecurso.desdeClasspath(), GestorRecurso.soloVector())) {
            DibujanteMapa dibujante = new DibujanteMapa(recursos);
            int[] nobody = paint(dibujante, campo(false, false), 10, 1);
            int[] esperando = paint(dibujante, campo(true, false), 10, 1);
            int[] hecho = paint(dibujante, campo(true, true), 10, 1);

            assertFalse(Arrays.equals(nobody, esperando), "the person is drawn");
            assertFalse(Arrays.equals(esperando, hecho), "the bubble goes away when the errand is done");
        }
    }

    private static VistaMapa campo(boolean conMarta, boolean hecho) {
        List<VistaParcela> parcelas = new ArrayList<>();
        for (int fila = 0; fila < 10; fila++) {
            for (int col = 0; col < 16; col++) {
                parcelas.add(new VistaParcela(col, fila, "GRASS", null, null, false, 0));
            }
        }
        List<VistaNpc> npcs = conMarta
                ? List.of(new VistaNpc("MARTA", "Marta", "Vendedora del mercado", 10, 1, "CARROT", 5, 60, hecho))
                : List.of();
        return new VistaMapa(16, 10, parcelas, List.of(), null, npcs);
    }

    private static int[] paint(DibujanteMapa dibujante, VistaMapa map, int col, int fila) {
        BufferedImage imagen = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        dibujante.renderizar(g, map, W, H, null);
        g.dispose();
        Rectangle c = GeometriaMapa.ajustar(16, 10, W, H).limitesCelda(col, fila);
        Rectangle alrededor = new Rectangle(c.x, c.y - c.height, c.width, c.height * 2);
        return imagen.getRGB(alrededor.x, alrededor.y, alrededor.width, alrededor.height, null, 0, alrededor.width);
    }
}
