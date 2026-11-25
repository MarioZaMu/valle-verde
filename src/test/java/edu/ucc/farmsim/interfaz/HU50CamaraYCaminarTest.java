package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.dominio.granja.Direccion;
import edu.ucc.farmsim.dominio.granja.InstantaneaAvatar;
import edu.ucc.farmsim.interfaz.componentes.TeclasCaminar;
import edu.ucc.farmsim.interfaz.dibujo.DibujanteMapa;
import edu.ucc.farmsim.interfaz.dibujo.EfectosVisuales;
import edu.ucc.farmsim.interfaz.dibujo.GeometriaMapa;
import edu.ucc.farmsim.interfaz.dibujo.PanelMapa;
import edu.ucc.farmsim.interfaz.dibujo.PosCelda;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-50 en pantalla: parcelas de 48 px, una cámara que sigue al avatar, personas altas y una caminata sin pausas. */
class HU50CamaraYCaminarTest {

    private static final int COLS = 48;
    private static final int FILAS = 24;
    private static final Rectangle PANTALLA = new Rectangle(0, 0, 1920, 1080);

    private static GeometriaMapa seguir(double col, double fila) {
        return GeometriaMapa.seguir(COLS, FILAS, GeometriaMapa.CELDA_PANTALLA, PANTALLA, col, fila);
    }

    @Test
    void HU50_ca3_at1920x1080APlotIs48PxAndTheCameraCentersTheAvatarWithoutShowingOutsideTheValley() {
        assertEquals(48, GeometriaMapa.celdaPara(1920, 1080, 1.0));
        assertEquals(38, GeometriaMapa.celdaPara(1536, 864, 1.25), "48 screen pixels with Windows at 125 %");
        assertEquals(40, GeometriaMapa.celdaPara(480, 320, 1.0), "a tiny window fits 12 x 8 plots");

        GeometriaMapa medio = seguir(24, 12);
        Rectangle avatar = medio.limitesCelda(24, 12);
        assertEquals(960, avatar.x + avatar.width / 2, "centered across");
        assertEquals(540, avatar.y + avatar.height / 2, "centered down");

        GeometriaMapa oeste = seguir(2, 1);
        assertEquals(0, oeste.originX(), "never left of the valley");
        assertEquals(0, oeste.originY());
        GeometriaMapa este = seguir(46, 22);
        assertEquals(1920 - COLS * 48, este.originX(), "never right of it");
        assertEquals(1080 - FILAS * 48, este.originY());

        GeometriaMapa pequenio = GeometriaMapa.seguir(16, 10, 48, PANTALLA, 0, 0);
        assertEquals((1920 - 16 * 48) / 2, pequenio.originX(), "a valley narrower than the window is centered");
        assertEquals(new PosCelda(24, 12), medio.celdaEn(960, 540), "a click hits the plot drawn there");
        assertNull(oeste.celdaEn(-5, 10));
    }

    @Test
    void HU50_ca3_la_camara_sigue_el_paso_sin_salta_y_conserva_el_avatar_en_el_libre_parte_de_la_vista() {
        AtomicLong now = new AtomicLong(5_000);
        EfectosVisuales fx = new EfectosVisuales(now::get, new Random(1));
        fx.onMapa(campo(new VistaAvatar(24, 12, "EAST")));
        fx.onMapa(campo(new VistaAvatar(25, 12, "EAST")));
        long start = now.get();
        List<Integer> origenes = new ArrayList<>();
        for (long t = 0; t <= EfectosVisuales.MS_PASO; t += 30) {
            now.set(start + t);
            double[] at = fx.posicionAvatar();
            origenes.add(seguir(at[0], at[1]).originX());
        }
        for (int i = 1; i < origenes.size(); i++) {
            int brincar = origenes.get(i - 1) - origenes.get(i);
            assertTrue(brincar >= 0 && brincar <= 12, "smooth: " + origenes);
        }
        assertEquals(seguir(25, 12).originX(), (int) origenes.get(origenes.size() - 1));
        now.set(start + 10 * EfectosVisuales.MS_PASO);
        assertArrayEquals(new double[] {25, 12}, fx.posicionAvatar());

        Rectangle libre = new Rectangle(0, 120, 1920, 760);
        GeometriaMapa conBarras = GeometriaMapa.seguir(COLS, FILAS, 48, libre, 24, 12);
        Rectangle avatar = conBarras.limitesCelda(24, 12);
        assertTrue(libre.contains(avatar), "the avatar is never under a bar");
    }

    @Test
    void HU50_ca3_theMapPanelUsesTheCameraOnTheRealValley() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PanelMapa panel = new PanelMapa(aplicacion.controlador, aplicacion.bus, (col, fila) -> { });
        panel.setSize(1920, 1080);

        assertEquals(48, panel.geometria().tamanioCelda());
        assertEquals(0, panel.geometria().originX(), "the avatar starts in the west of the farm");
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(40, 12, Direccion.EAST));
        panel.recargar();
        assertEquals(1920 - COLS * 48, panel.geometria().originX(), "in the village the camera went east");
    }

    @Test
    void HU50_ca4_peopleAre32x64SpritesDrawnOneByTwoPlotsWithTheirFeetOnTheirPlot() {
        GestorRecurso recursos = GestorRecurso.desdeClasspath();
        for (String name : List.of("AVATAR_S_0", "AVATAR_E_2", "MARTA", "TOMAS", "LUCIA", "INES")) {
            BufferedImage sprite = recursos.sprite("characters/" + name + ".png").orElseThrow();
            assertEquals(32, sprite.getWidth(), name);
            assertEquals(64, sprite.getHeight(), name);
        }
        for (GestorRecurso pack : List.of(recursos, GestorRecurso.soloVector())) {
            DibujanteMapa dibujante = new DibujanteMapa(pack);
            assertFalse(Arrays.equals(paint(dibujante, campo(null), 7, 6), paint(dibujante, campo(new VistaAvatar(7, 7, "SOUTH")), 7, 6)),
                    "the head rises into the plot above");
            assertTrue(Arrays.equals(paint(dibujante, campo(null), 7, 8), paint(dibujante, campo(new VistaAvatar(7, 7, "SOUTH")), 7, 8)),
                    "nothing below the feet");
        }
    }

    @Test
    void HU50_ca5_holdingAKeyStepsAtOnceThenEvery180MsWithoutPauseAndReleasingStops() {
        TeclasCaminar claves = new TeclasCaminar(EfectosVisuales.MS_PASO);

        assertEquals("EAST", claves.pulsar("EAST", 1_000), "the first step goes at once");
        assertNull(claves.pulsar("EAST", 1_030), "the key repeat of the system is ignored");
        assertNull(claves.tic(1_100));
        List<Long> pasos = new ArrayList<>();
        for (long t = 1_100; t <= 1_800; t += 10) {
            if ("EAST".equals(claves.tic(t))) {
                pasos.add(t);
            }
        }
        assertEquals(List.of(1_180L, 1_360L, 1_540L, 1_720L), pasos, "one step every 180 ms, no pause");

        assertEquals("NORTH", claves.pulsar("NORTH", 1_900), "the last key pressed leads");
        claves.liberar("NORTH");
        assertEquals("EAST", claves.tic(2_080), "back to the key still held");
        claves.liberar("EAST");
        assertFalse(claves.esCaminando());
        assertNull(claves.tic(3_000), "released: it stops");
        assertEquals("WEST", claves.pulsar("WEST", 3_050), "a new press walks again at once");
    }

    @Test
    void HU50_ca5_holdingTwoKeysLikeWAndDWalksDiagonallyAtTheSamePaceWithoutAnExtraStep() {
        TeclasCaminar claves = new TeclasCaminar(EfectosVisuales.MS_PASO);

        assertEquals("NORTH", claves.pulsar("NORTH", 1_000));
        assertNull(claves.pulsar("EAST", 1_060), "the second key waits for the step under way: no jerk");
        List<String> pasos = new ArrayList<>();
        List<Long> veces = new ArrayList<>();
        for (long t = 1_000; t <= 1_900; t += 10) {
            String paso = claves.tic(t);
            if (paso != null) {
                pasos.add(paso);
                veces.add(t);
            }
        }
        assertEquals(List.of("EAST", "NORTH", "EAST", "NORTH", "EAST"), pasos, "it zigzags: a diagonal");
        assertEquals(List.of(1_180L, 1_360L, 1_540L, 1_720L, 1_900L), veces, "still one step every 180 ms");
        assertEquals("NORTH", claves.alternativa("EAST"), "if east is blocked, north is tried instead");

        claves.liberar("NORTH");
        assertEquals("EAST", claves.tic(2_080), "with one key left it walks straight");
        assertEquals("EAST", claves.tic(2_260));
        assertNull(claves.alternativa("EAST"));
        claves.pulsar("WEST", 2_300);
        assertEquals("WEST", claves.tic(2_440), "opposite keys do not zigzag: the last one leads");
        assertEquals("WEST", claves.tic(2_620));
    }

    @Test
    void HU50_ca5_aStepMovesTheAvatarOnTheShownMapWithoutDownloadingTheWholeValleyAgain() throws Exception {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        AtomicLong llamadasMapa = new AtomicLong();
        ComandosJuego contado = (ComandosJuego) Proxy.newProxyInstance(ComandosJuego.class.getClassLoader(),
                new Class<?>[] {ComandosJuego.class}, (proxy, method, args) -> {
                    if ("map".equals(method.getName())) {
                        llamadasMapa.incrementAndGet();
                    }
                    try {
                        return method.invoke(aplicacion.controlador, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        PanelMapa panel = new PanelMapa(contado, aplicacion.bus, (col, fila) -> { });
        panel.setSize(1920, 1080);
        VistaMapa antes = panel.vistaActual();
        long llamadas = llamadasMapa.get();

        String manera = null;
        for (String direccion : List.of("EAST", "SOUTH", "WEST", "NORTH")) {
            if (aplicacion.controlador.mover(direccion).isOk()) {
                manera = direccion;
                break;
            }
        }
        javax.swing.SwingUtilities.invokeAndWait(() -> { });

        assertEquals(PanelMapa.MS_RETRASO_MARCO_CAMINAR, panel.retrasoMarco(), "while it walks the map is drawn 60 times a second");
        assertEquals(llamadas, llamadasMapa.get(), "a step does not ask for the 1152 plots again (it made the walk jerk)");
        assertEquals(aplicacion.controlador.map().value().avatar(), panel.vistaActual().avatar(), "the avatar is where it went");
        assertEquals(manera, panel.vistaActual().avatar().mirada());
        assertEquals(antes.parcelas(), panel.vistaActual().parcelas(), "the rest of the valley is the same");

        VistaAvatar at = panel.vistaActual().avatar();
        panel.cara("NORTH");
        assertEquals(new VistaAvatar(at.col(), at.fila(), "NORTH"), panel.vistaActual().avatar(),
                "a blocked step only turns the avatar, also without asking for the map");
        assertEquals(llamadas, llamadasMapa.get());
    }

    @Test
    void HU50_ca5_aMovingCameraSlidesTheGroundAlreadyPaintedInsteadOfPaintingItAgainOnEveryRow() {
        VistaMapa valle = AplicacionTest.juegoIniciado().controlador.map().value();
        DibujanteMapa dibujante = new DibujanteMapa(GestorRecurso.desdeClasspath());
        Rectangle libre = new Rectangle(0, 120, 1920, 760);
        BufferedImage imagen = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        for (double fila = 0; fila <= FILAS; fila += 0.5) {
            dibujante.renderizar(g, valle, GeometriaMapa.seguir(COLS, FILAS, 48, libre, 24, fila), 1920, 1080, null, null);
        }
        for (double col = 0; col <= COLS; col += 0.5) {
            dibujante.renderizar(g, valle, GeometriaMapa.seguir(COLS, FILAS, 48, libre, col, 12), 1920, 1080, null, null);
        }
        g.dispose();

        assertEquals(1, dibujante.pintaSuelo(),
                "walking up and down (or across) the valley never repaints the ground: repainting it made the walk jerk");
    }

    @Test
    void HU50_ca5_el_caminar_animacion_conserva_yendo_a_traves_pasos_en_lugar_de_reiniciando() {
        AtomicLong now = new AtomicLong(10_000);
        EfectosVisuales fx = new EfectosVisuales(now::get, new Random(1));
        fx.onMapa(campo(new VistaAvatar(5, 5, "EAST")));
        fx.onMapa(campo(new VistaAvatar(6, 5, "EAST")));
        now.addAndGet(EfectosVisuales.MS_PASO);
        fx.onMapa(campo(new VistaAvatar(7, 5, "EAST")));
        now.addAndGet(EfectosVisuales.MS_PASO / 3 + 1);

        assertTrue(fx.esCaminando());
        assertEquals(1, fx.marcoAvatar(), "the second step goes on with the cycle of the first");
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

    private static int[] paint(DibujanteMapa dibujante, VistaMapa map, int col, int fila) {
        BufferedImage imagen = new BufferedImage(1024, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        dibujante.renderizar(g, map, 1024, 640, null, null);
        g.dispose();
        Rectangle c = GeometriaMapa.ajustar(16, 10, 1024, 640).limitesCelda(col, fila);
        return imagen.getRGB(c.x, c.y, c.width, c.height, null, 0, c.width);
    }
}
