package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.interfaz.dibujo.DibujanteMapa;
import edu.ucc.farmsim.interfaz.dibujo.GeometriaMapa;
import edu.ucc.farmsim.interfaz.dibujo.PanelMapa;
import edu.ucc.farmsim.interfaz.dibujo.PosCelda;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/**
 * HU-35: el mapa de la granja dibujado con Java2D. La geometría y el dibujo son clases separadas sin componente, así
 * que se prueban pintando sobre una imagen. Los edificios y animales se dibujan sobre las parcelas (HU-35 · CA-3).
 */
class HU35VistaMapaGranjaTest {

    private static final int ANCHO = 1024;
    private static final int ALTO = 600;

    /** Dónde pone el renderizador las parcelas de todo el valle cuando se dibuja sin cámara. */
    private static final GeometriaMapa VALLE = GeometriaMapa.ajustar(48, 24, ANCHO, ALTO);

    private static BufferedImage paint(VistaMapa map, PosCelda sobrevolar) {
        BufferedImage imagen = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        new DibujanteMapa(edu.ucc.farmsim.interfaz.recursos.GestorRecurso.soloVector()).renderizar(g, map, ANCHO, ALTO, sobrevolar);
        g.dispose();
        return imagen;
    }

    private static int[] pixelesCelda(BufferedImage imagen, GeometriaMapa geometria, int col, int fila) {
        var b = geometria.limitesCelda(col, fila);
        int[] pixeles = new int[b.width * b.height];
        imagen.getRGB(b.x, b.y, b.width, b.height, pixeles, 0, b.width);
        return pixeles;
    }

    private static int colorCentro(BufferedImage imagen, GeometriaMapa geometria, int col, int fila) {
        var b = geometria.limitesCelda(col, fila);
        return imagen.getRGB(b.x + 2, b.y + 2, 1, 1, null, 0, 1)[0];
    }

    private static VistaMapa mapaCon(VistaParcela parcela) {
        List<VistaParcela> parcelas = new ArrayList<>();
        for (int fila = 0; fila < 10; fila++) {
            for (int col = 0; col < 16; col++) {
                parcelas.add(col == 5 && fila == 3 ? parcela : new VistaParcela(col, fila, "SOIL", null, null, false, 0));
            }
        }
        return new VistaMapa(16, 10, parcelas);
    }

    @Test
    void HU35_ca1_theMapShowsThe48x24ValleyAndTheFourTerrainsLookDifferent() {
        // Given a new game
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        VistaMapa map = aplicacion.controlador.map().value();
        assertEquals(48 * 24, map.parcelas().size());
        GeometriaMapa geometria = VALLE;

                // Cuando se dibuja
        BufferedImage imagen = paint(map, null);

                // Entonces la tierra, el pasto, el agua y los obstáculos usan colores distintos (píxel de la esquina de cada
        // celda)
        Set<Integer> colores = new HashSet<>();
        colores.add(colorCentro(imagen, geometria, DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA)); // soil
        colores.add(colorCentro(imagen, geometria, 1, 1));   // grass
        colores.add(colorCentro(imagen, geometria, 13, 0));  // water
        colores.add(colorCentro(imagen, geometria, 0, 0));   // obstacle
        assertEquals(4, colores.size(), "four terrains must look different");
    }

    @Test
    void HU35_ca2_everyCropStageHasADifferentAppearanceFromTheOthersAndFromBareSoil() {
        GeometriaMapa geometria = GeometriaMapa.ajustar(16, 10, ANCHO, ALTO);
        List<int[]> mira = new ArrayList<>();
        mira.add(pixelesCelda(paint(mapaCon(new VistaParcela(5, 3, "SOIL", null, null, false, 0)), null), geometria, 5, 3));
        for (String etapa : new String[] {"SEED", "SPROUT", "GROWING", "MATURE", "WITHERED"}) {
            VistaParcela parcela = new VistaParcela(5, 3, "SOIL", "CARROT", etapa, false, 2);
            mira.add(pixelesCelda(paint(mapaCon(parcela), null), geometria, 5, 3));
        }
        for (int i = 0; i < mira.size(); i++) {
            for (int j = i + 1; j < mira.size(); j++) {
                assertFalse(Arrays.equals(mira.get(i), mira.get(j)), "appearances " + i + " and " + j + " are equal");
            }
        }
    }

    @Test
    void HU35_ca2_aWateredCropLooksDifferentFromADryOne() {
        GeometriaMapa geometria = GeometriaMapa.ajustar(16, 10, ANCHO, ALTO);
        int[] seco = pixelesCelda(paint(mapaCon(new VistaParcela(5, 3, "SOIL", "CARROT", "SPROUT", false, 3)), null), geometria, 5, 3);
        int[] mojado = pixelesCelda(paint(mapaCon(new VistaParcela(5, 3, "SOIL", "CARROT", "SPROUT", true, 3)), null), geometria, 5, 3);

        assertFalse(Arrays.equals(seco, mojado));
    }

    @Test
    void HU35_ca2_differentCropsHaveADifferentMatureColor() {
        GeometriaMapa geometria = GeometriaMapa.ajustar(16, 10, ANCHO, ALTO);
        int[] zanahoria = pixelesCelda(paint(mapaCon(new VistaParcela(5, 3, "SOIL", "CARROT", "MATURE", false, 0)), null), geometria, 5, 3);
        int[] tomate = pixelesCelda(paint(mapaCon(new VistaParcela(5, 3, "SOIL", "TOMATO", "MATURE", false, 0)), null), geometria, 5, 3);

        assertFalse(Arrays.equals(zanahoria, tomate));
    }

    @Test
    void HU35_el_sobrevolado_parcela_es_resaltado() {
        VistaMapa map = AplicacionTest.juegoIniciado().controlador.map().value();
        GeometriaMapa geometria = VALLE;

        int[] sencillo = pixelesCelda(paint(map, null), geometria, 5, 3);
        int[] hovered = pixelesCelda(paint(map, new PosCelda(5, 3)), geometria, 5, 3);

        assertFalse(Arrays.equals(sencillo, hovered));
    }

    @Test
    void HU35_ca4_clics_afuera_el_mapa_area_mapa_a_no_parcela() {
        GeometriaMapa geometria = GeometriaMapa.ajustar(16, 10, ANCHO, ALTO);
        var first = geometria.limitesCelda(0, 0);
        var ultimo = geometria.limitesCelda(15, 9);

        assertEquals(new PosCelda(0, 0), geometria.celdaEn(first.x + 1, first.y + 1));
        assertEquals(new PosCelda(15, 9), geometria.celdaEn(ultimo.x + ultimo.width - 1, ultimo.y + ultimo.height - 1));
        assertEquals(new PosCelda(5, 3), geometria.celdaEn(geometria.limitesCelda(5, 3).x + 3, geometria.limitesCelda(5, 3).y + 3));
        assertNull(geometria.celdaEn(-5, -5));
        assertNull(geometria.celdaEn(first.x - 1, first.y + 5));
        assertNull(geometria.celdaEn(ultimo.x + ultimo.width + 1, ultimo.y + 5));
        assertNull(geometria.celdaEn(ANCHO + 50, ALTO + 50));
    }

    @Test
    void HU35_ca4_aClickOutsideTheMapOnThePanelDoesNothing() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        List<PosCelda> clics = new ArrayList<>();
        PanelMapa panel = new PanelMapa(aplicacion.controlador, aplicacion.bus, (col, fila) -> clics.add(new PosCelda(col, fila)));
        panel.setSize(ANCHO, ALTO);

        panel.clicManejar(-10, -10);
        assertTrue(clics.isEmpty());

        var dentro = panel.geometria().limitesCelda(5, 3);
        panel.clicManejar(dentro.x + 4, dentro.y + 4);
        assertEquals(List.of(new PosCelda(5, 3)), clics);
    }

    @Test
    void HU35_ca5_theMapIsLegibleFrom1024x768WithCellsOfAtLeast40Pixels() {
                // El área del mapa de una ventana de 1024 x 768 es de unos 1024 x 600 una vez quitadas la barra superior y los
        // botones
        GeometriaMapa geometria = GeometriaMapa.ajustar(16, 10, 1024, 600);

        assertTrue(geometria.tamanioCelda() >= 40, "cell size " + geometria.tamanioCelda());
        var ultimo = geometria.limitesCelda(15, 9);
        assertTrue(ultimo.x + ultimo.width <= 1024 && ultimo.y + ultimo.height <= 600, "the whole map fits without clipping");
    }

    @Test
    void HU35_ca5_thePanelRedrawsWhenAMapEventArrivesAndNeverPollsTheGame() throws Exception {
                // Dado un panel que muestra el mapa de una partida nueva, sobre un controlador que cuenta las peticiones del
        // mapa
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        int[] mapRequests = {0};
        edu.ucc.farmsim.aplicacion.controlador.ComandosJuego contando =
                (edu.ucc.farmsim.aplicacion.controlador.ComandosJuego) java.lang.reflect.Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[] {edu.ucc.farmsim.aplicacion.controlador.ComandosJuego.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("map")) {
                                mapRequests[0]++;
                            }
                            return method.invoke(aplicacion.controlador, args);
                        });
        PanelMapa panel = new PanelMapa(contando, aplicacion.bus, (col, fila) -> { });
        SwingUtilities.invokeAndWait(() -> { });
        assertNull(panel.vistaActual().parcelaEn(5, 3).cultivo());

                // Cuando se siembra un cultivo (el controlador publica MapChanged)
        aplicacion.controlador.plantar(5, 3, "CARROT_SEED");
        SwingUtilities.invokeAndWait(() -> { });

                // Entonces el panel ya tiene la vista nueva, y sin nada en movimiento no tiene ningún temporizador corriendo
        assertNotNull(panel.vistaActual().parcelaEn(5, 3).cultivo());
        assertFalse(panel.esAnimando());
        int peticiones = mapRequests[0];
        Thread.sleep(150);
        SwingUtilities.invokeAndWait(() -> { });
        assertEquals(peticiones, mapRequests[0], "the panel never asks the game for the map on its own");
    }

    @Test
    void HU07_ca1_theTooltipAppearsAfter300Milliseconds() {
        assertEquals(300, PanelMapa.MS_RETRASO_AYUDA);
    }

    @Test
    void HU35_geometria_rechaza_invalido_tamanios() {
        assertEquals(1, GeometriaMapa.ajustar(16, 10, 0, 0).tamanioCelda());
    }

    @Test
    void HU35_ca3_aCoopWithTwoHensIsDrawnInItsPositionAndTheHensChangeWhatIsSeen() {
                // Dado un gallinero construido en (2,7), que cubre de (2,7) a (3,8)
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        var gallinero = DatosJuego.gallinero(aplicacion.estado());
        GeometriaMapa geometria = VALLE;
        VistaMapa empty = aplicacion.controlador.map().value();
        VistaMapa noEdificios = new VistaMapa(empty.cols(), empty.filas(), empty.parcelas());
        int[] hierba = pixelesCelda(paint(noEdificios, null), geometria, 2, 8);
        int[] coopEmpty = pixelesCelda(paint(empty, null), geometria, 2, 8);

                // Cuando se mudan dos gallinas
        DatosJuego.gallina(gallinero);
        DatosJuego.gallina(gallinero);
        VistaMapa conGallinas = aplicacion.controlador.map().value();
        int[] gallineroConGallinas = pixelesCelda(paint(conGallinas, null), geometria, 2, 8);

                // Entonces el edificio reemplaza al pasto, las gallinas cambian su aspecto y la parcela de al lado queda
        // intacta
        assertEquals(1, empty.edificios().size());
        assertEquals(2, conGallinas.edificioEn(3, 8).animales().size());
        assertNotEquals(Arrays.hashCode(hierba), Arrays.hashCode(coopEmpty));
        assertNotEquals(Arrays.hashCode(coopEmpty), Arrays.hashCode(gallineroConGallinas));
        assertEquals(Arrays.hashCode(pixelesCelda(paint(noEdificios, null), geometria, 4, 8)),
                Arrays.hashCode(pixelesCelda(paint(conGallinas, null), geometria, 4, 8)));
    }

    @Test
    void HU35_ca3_aSickHenAndTheProductsWaitingLookDifferentFromAHealthyOne() {
        GeometriaMapa geometria = VALLE;
        int[] sano = celdaGallinero(edu.ucc.farmsim.dominio.animal.CondicionAnimal.HEALTHY, 0, geometria, 2, 8);
        int[] enfermo = celdaGallinero(edu.ucc.farmsim.dominio.animal.CondicionAnimal.SICK, 0, geometria, 2, 8);
        int[] hambriento = celdaGallinero(edu.ucc.farmsim.dominio.animal.CondicionAnimal.HUNGRY, 0, geometria, 2, 8);
        int[] noProducts = celdaGallinero(edu.ucc.farmsim.dominio.animal.CondicionAnimal.HEALTHY, 0, geometria, 3, 7);
        int[] productos = celdaGallinero(edu.ucc.farmsim.dominio.animal.CondicionAnimal.HEALTHY, 1, geometria, 3, 7);

        assertNotEquals(Arrays.hashCode(sano), Arrays.hashCode(enfermo));
        assertNotEquals(Arrays.hashCode(sano), Arrays.hashCode(hambriento));
        assertNotEquals(Arrays.hashCode(noProducts), Arrays.hashCode(productos));
    }

    private static int[] celdaGallinero(edu.ucc.farmsim.dominio.animal.CondicionAnimal condicion, int pendiente,
                                  GeometriaMapa geometria, int col, int fila) {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.casa(DatosJuego.gallinero(aplicacion.estado()), edu.ucc.farmsim.dominio.animal.TipoAnimal.HEN, condicion, 0,
                false, pendiente);
        return pixelesCelda(paint(aplicacion.controlador.map().value(), null), geometria, col, fila);
    }

    @Test
    void HU35_cada_categoria_de_edificio_tiene_su_propio_mirar() {
        GeometriaMapa geometria = VALLE;
        Set<Integer> mira = new HashSet<>();
        for (var tipo : edu.ucc.farmsim.dominio.edificio.TipoEdificio.values()) {
            AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
            DatosJuego.build(aplicacion.estado(), tipo, 2, 7);
            mira.add(Arrays.hashCode(pixelesCelda(paint(aplicacion.controlador.map().value(), null), geometria, 2, 7)));
        }
        assertEquals(4, mira.size());
    }

    @Test
    void HU35_theTooltipOfABuildingTellsItsAnimalsAndWhatWaits() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        var gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.casa(gallinero, edu.ucc.farmsim.dominio.animal.TipoAnimal.HEN,
                edu.ucc.farmsim.dominio.animal.CondicionAnimal.HEALTHY, 0, false, 1);

        String texto = edu.ucc.farmsim.interfaz.componentes.TextoParcela.describir(aplicacion.controlador.edificios().value().edificios().get(0));

        assertEquals("Gallinero 1 · 1/4 animales · 1 por recolectar", texto);
    }
}
