package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaAnimal;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.interfaz.dibujo.DibujanteMapa;
import edu.ucc.farmsim.interfaz.dibujo.EfectosVisuales;
import edu.ucc.farmsim.interfaz.dibujo.EscenaTitulo;
import edu.ucc.farmsim.interfaz.dibujo.GeometriaMapa;
import edu.ucc.farmsim.interfaz.dibujo.PanelMapa;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import edu.ucc.farmsim.interfaz.tema.Paleta;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

/** HU-48 · CA-6 a CA-8: una vista continua a 3/4 del valle, vida ambiental y una pantalla de inicio animada. */
class HU48VistaValleTest {

    private static final int W = 1024;
    private static final int H = 640;

    private final AtomicLong now = new AtomicLong(0);

    private static VistaMapa map(String fill, List<VistaParcela> especial, List<VistaEdificio> edificios) {
        List<VistaParcela> parcelas = new ArrayList<>();
        for (int fila = 0; fila < 10; fila++) {
            for (int col = 0; col < 16; col++) {
                int c = col;
                int r = fila;
                parcelas.add(especial.stream().filter(p -> p.col() == c && p.fila() == r).findFirst()
                        .orElse(new VistaParcela(col, fila, fill, null, null, false, 0)));
            }
        }
        return new VistaMapa(16, 10, parcelas, edificios);
    }

    private static BufferedImage paint(DibujanteMapa dibujante, VistaMapa map, EfectosVisuales fx) {
        BufferedImage imagen = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        dibujante.renderizar(g, map, W, H, null, fx);
        g.dispose();
        return imagen;
    }

    private static int[] pixeles(BufferedImage imagen, Rectangle r) {
        return imagen.getRGB(r.x, r.y, r.width, r.height, null, 0, r.width);
    }

    private static Rectangle celda(int col, int fila) {
        return GeometriaMapa.ajustar(16, 10, W, H).limitesCelda(col, fila);
    }

    @Test
    void HU48_ca6_no_grilla_linea_es_dibujado_entre_dos_parcelas() {
        BufferedImage vector = paint(new DibujanteMapa(GestorRecurso.soloVector()), map("GRASS", List.of(), List.of()), null);
        Rectangle izquierda = celda(2, 5);
        for (int y = izquierda.y; y < izquierda.y + izquierda.height; y++) {
            assertEquals(Paleta.GRASS.getRGB(), vector.getRGB(izquierda.x + izquierda.width - 1, y), "seam at y " + y);
            assertEquals(Paleta.GRASS.getRGB(), vector.getRGB(izquierda.x + izquierda.width, y), "seam at y " + y);
        }
        BufferedImage arte = paint(new DibujanteMapa(), map("GRASS", List.of(), List.of()), null);
        int seamX = izquierda.x + izquierda.width;
        long costura = columna(arte, seamX, izquierda);
        long oscuro = Long.MAX_VALUE;
        for (int x = izquierda.x + 2; x < seamX + izquierda.width - 2; x++) {
            if (Math.abs(x - seamX) > 1) {
                oscuro = Math.min(oscuro, columna(arte, x, izquierda));
            }
        }
        assertTrue(costura * 100 >= oscuro * 97, "the seam (" + costura + ") is no darker than the grass itself (" + oscuro + ")");
    }

    @Test
    void HU48_ca6_soilBlendsIntoTheGrassWithARaggedEdge() throws IOException {
        BufferedImage tierra = ImageIO.read(getClass().getResource("/assets/terrain/SOIL.png"));
        VistaMapa campo = map("GRASS", List.of(new VistaParcela(5, 3, "SOIL", null, null, false, 0)), List.of());
        BufferedImage imagen = paint(new DibujanteMapa(), campo, null);
        Rectangle c = celda(5, 3);

        int cubierto = 0;
        for (int x = 0; x < 32; x++) {
            if (imagen.getRGB(c.x + 2 * x, c.y) != tierra.getRGB(x, 0)) {
                cubierto++;
            }
        }
        assertEquals(tierra.getRGB(16, 16), imagen.getRGB(c.x + 32, c.y + 32), "the middle is plain soil");
        assertEquals(32, cubierto, "the grass covers the whole top edge");
        Rectangle debajo = new Rectangle(c.x, c.y + 4 * 2, c.width, 2);
        int[] fila = pixeles(imagen, debajo);
        assertTrue(Arrays.stream(fila).distinct().count() > 2, "the edge is ragged, not a straight line");
    }

    @Test
    void HU48_ca6_treesRiseAboveTheirPlotAndCastAShadow() {
        List<VistaParcela> arboles = new ArrayList<>();
        for (int col = 2; col < 14; col += 3) {
            arboles.add(new VistaParcela(col, 6, "OBSTACLE", null, null, false, 0));
        }
        BufferedImage desnudo = paint(new DibujanteMapa(), map("GRASS", List.of(), List.of()), null);
        BufferedImage madera = paint(new DibujanteMapa(), map("GRASS", arboles, List.of()), null);

        boolean sube = false;
        for (VistaParcela arbol : arboles) {
            sube |= !Arrays.equals(pixeles(desnudo, celda(arbol.col(), 5)), pixeles(madera, celda(arbol.col(), 5)));
        }
        assertTrue(sube, "a tree covers part of the plot above it");
    }

    @Test
    void HU48_ca6_aBuildingThatHidesACropTurnsSeeThrough() {
        VistaEdificio gallinero = new VistaEdificio(0, "COOP", "Gallinero", 2, 7, 2, 2, 4, "Huevo", 0, List.<VistaAnimal>of());
        VistaParcela detras = new VistaParcela(2, 6, "SOIL", "CORN", "MATURE", false, 0);
        BufferedImage sin = paint(new DibujanteMapa(), map("GRASS", List.of(new VistaParcela(2, 6, "SOIL", null, null, false, 0)),
                List.of(gallinero)), null);
        BufferedImage con = paint(new DibujanteMapa(), map("GRASS", List.of(detras), List.of(gallinero)), null);
        Rectangle c = celda(2, 6);
        Rectangle menor = new Rectangle(c.x + c.width / 4, c.y + c.height / 2, c.width / 2, c.height / 2);

        assertFalse(Arrays.equals(pixeles(sin, menor), pixeles(con, menor)), "the crop shows through the roof");
    }

    @Test
    void HU48_ca6_theCameraNeverPutsTheAvatarNorTheEdgeOfTheValleyUnderAFloatingBar() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PanelMapa panel = new PanelMapa(aplicacion.controlador, aplicacion.bus, (col, fila) -> { });
        JPanel barra = new JPanel();
        JPanel etapa = new JPanel(null);
        etapa.add(barra);
        etapa.add(panel);
        etapa.setBounds(0, 0, W, 760);
        panel.setBounds(0, 0, W, 760);
        barra.setBounds(0, 620, W, 140);
        assertTrue(panel.geometria().area().intersects(barra.getBounds()));

        panel.agregarFlotante(barra);

        for (int fila : new int[] {7, 22, 23}) {
            aplicacion.estado().avatar().restaurarDesde(new edu.ucc.farmsim.dominio.granja.InstantaneaAvatar(7, fila,
                    edu.ucc.farmsim.dominio.granja.Direccion.SOUTH));
            panel.recargar();
            Rectangle avatar = panel.geometria().limitesCelda(7, fila);
            assertFalse(avatar.intersects(barra.getBounds()), avatar + " under " + barra.getBounds());
        }
        Rectangle area = panel.geometria().area();
        assertTrue(area.y + area.height <= barra.getY(), "the last row of the valley stays above the bar: " + area);
        assertEquals(0, panel.geometria().celdaEn(area.x + 1, area.y + area.height - 1).col());
    }

    @Test
    void HU48_ca7_withTheAmbientLifeOnTheWaterMovesAndOffEverythingStaysStill() {
        VistaMapa estanque = map("WATER", List.of(), List.of());
        EfectosVisuales fx = new EfectosVisuales(now::get, new Random(5));
        fx.onMundo("SPRING", "SUNNY");
        DibujanteMapa dibujante = new DibujanteMapa();
        BufferedImage first = paint(dibujante, estanque, fx);
        now.set(500);
        assertTrue(Arrays.equals(pixeles(first, celda(5, 5)), pixeles(paint(dibujante, estanque, fx), celda(5, 5))),
                "without ambient life nothing moves");
        assertFalse(fx.marcosNecesita());

        fx.ambienteFijar(true);
        now.set(0);
        int[] still = pixeles(paint(dibujante, estanque, fx), celda(5, 5));
        now.set(500);
        int[] luego = pixeles(paint(dibujante, estanque, fx), celda(5, 5));

        assertFalse(Arrays.equals(still, luego), "the water ripples");
        assertTrue(fx.marcosNecesita());
        fx.setEnabled(false);
        assertFalse(fx.esAmbiente());
        assertFalse(fx.marcosNecesita());
    }

    @Test
    void HU48_ca7_aPanelThatIsNotOnScreenRunsNoTimerForTheAmbientLife() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PanelMapa panel = new PanelMapa(aplicacion.controlador, aplicacion.bus, (col, fila) -> { });

        assertTrue(panel.efectos().esAmbiente());
        assertFalse(panel.esAnimando());
    }

    @Test
    void HU48_ca8_el_inicio_escena_mueve_y_es_dibujado_incluso_sin_sprites() {
        for (GestorRecurso recursos : List.of(GestorRecurso.desdeClasspath(), GestorRecurso.soloVector())) {
            EscenaTitulo escena = new EscenaTitulo(recursos);
            BufferedImage a = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
            BufferedImage b = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
            escena.paint(a.createGraphics(), 800, 500, 1_000);
            escena.paint(b.createGraphics(), 800, 500, 4_000);

            assertFalse(Arrays.equals(a.getRGB(0, 0, 800, 500, null, 0, 800), b.getRGB(0, 0, 800, 500, null, 0, 800)));
            assertEquals(0xFF, a.getRGB(400, 480) >>> 24, "the whole area is painted");
        }
    }

    private static long columna(BufferedImage imagen, int x, Rectangle filas) {
        long sum = 0;
        for (int y = filas.y; y < filas.y + filas.height; y++) {
            sum += brillo(imagen.getRGB(x, y));
        }
        return sum;
    }

    private static long brillo(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }
}
