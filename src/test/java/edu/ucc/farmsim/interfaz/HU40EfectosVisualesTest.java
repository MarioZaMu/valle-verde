package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.interfaz.dibujo.DibujanteMapa;
import edu.ucc.farmsim.interfaz.dibujo.EfectosVisuales;
import edu.ucc.farmsim.interfaz.dibujo.GeometriaMapa;
import edu.ucc.farmsim.interfaz.dibujo.PanelMapa;
import edu.ucc.farmsim.interfaz.dibujo.Particula;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-40: la estación y el clima se ven en el mapa, y solo en el mapa (RN-01, RN-11, RN-12). */
class HU40EfectosVisualesTest {

    private static final int W = 1024;
    private static final int H = 600;

    private final AtomicLong now = new AtomicLong(0);
    private final EfectosVisuales fx = new EfectosVisuales(now::get, new Random(7));

    private static VistaMapa mapaHierba() {
        List<VistaParcela> parcelas = new ArrayList<>();
        for (int fila = 0; fila < 10; fila++) {
            for (int col = 0; col < 16; col++) {
                parcelas.add(new VistaParcela(col, fila, "GRASS", null, null, false, 0));
            }
        }
        return new VistaMapa(16, 10, parcelas, List.<VistaEdificio>of());
    }

    private static BufferedImage paint(EfectosVisuales efectos) {
        BufferedImage imagen = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        new DibujanteMapa().renderizar(g, mapaHierba(), W, H, null, efectos);
        g.dispose();
        return imagen;
    }

    private static int pixelHierba(BufferedImage imagen) {
        var celda = GeometriaMapa.ajustar(16, 10, W, H).limitesCelda(2, 5);
        return imagen.getRGB(celda.x + celda.width / 2, celda.y + celda.height / 2);
    }

    private static long brillo(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }

    @Test
    void HU40_ca1_everySeasonHasItsOwnPaletteOnTheMap() {
        Set<Integer> hierba = new HashSet<>();
        for (String estacion : List.of("SPRING", "SUMMER", "AUTUMN", "WINTER")) {
            fx.onMundo(estacion, "SUNNY");
            hierba.add(pixelHierba(paint(fx)));
        }

        assertEquals(4, hierba.size());
    }

    @Test
    void HU40_ca1_el_noche_tinte_oscurece_el_mapa_durante_el_fin_de_dia_transicion() {
        fx.onMundo("SPRING", "SUNNY");
        long dia = brillo(pixelHierba(paint(fx)));

        fx.onDiaTerminado();
        now.set(EfectosVisuales.MS_TRANSICION / 2);
        long noche = brillo(pixelHierba(paint(fx)));
        now.set(EfectosVisuales.MS_TRANSICION);
        long despues = brillo(pixelHierba(paint(fx)));

        assertTrue(noche < dia, "night " + noche + " day " + dia);
        assertEquals(dia, despues);
        assertEquals(0f, fx.alfaNoche());
    }

    @Test
    void HU40_ca2_lluvia_y_nieve_mostrar_particulas_y_otro_clima_muestra_ninguno() {
        fx.onMundo("SPRING", "RAINY");
        assertFalse(fx.particulas().isEmpty());
        assertTrue(fx.particulas().stream().allMatch(p -> p.categoria() == Particula.Categoria.RAIN));

        fx.onMundo("WINTER", "SNOWY");
        assertFalse(fx.particulas().isEmpty());
        assertTrue(fx.particulas().stream().allMatch(p -> p.categoria() == Particula.Categoria.SNOW));

        fx.onMundo("SUMMER", "SUNNY");
        assertTrue(fx.particulas().isEmpty());
        fx.onMundo("SUMMER", "DROUGHT");
        assertTrue(fx.particulas().isEmpty());
    }

    @Test
    void HU40_ca2_particulas_mover_y_quedar_dentro_el_mapa() {
        fx.onMundo("SPRING", "RAINY");
        double antes = fx.particulas().get(0).y();

        for (int i = 0; i < 200; i++) {
            now.addAndGet(33);
            fx.paso();
        }

        assertNotEquals(antes, fx.particulas().get(0).y());
        assertTrue(fx.particulas().stream().allMatch(p -> p.x() >= 0 && p.x() <= 1 && p.y() >= 0 && p.y() <= 1));
    }

    @Test
    void HU40_ca2_rainFallsOnTheImageAndDroughtAndFrostHaveTheirOwnIndicator() {
        fx.onMundo("SPRING", "SUNNY");
        int[] soleado = pixeles(paint(fx));

        fx.onMundo("SPRING", "RAINY");
        now.addAndGet(100);
        fx.paso();
        int[] rain = pixeles(paint(fx));
        fx.onMundo("SPRING", "DROUGHT");
        int[] drought = pixeles(paint(fx));
        fx.onMundo("SPRING", "FROST");
        int[] frost = pixeles(paint(fx));

        assertFalse(java.util.Arrays.equals(soleado, rain));
        assertFalse(java.util.Arrays.equals(soleado, drought));
        assertFalse(java.util.Arrays.equals(soleado, frost));
        assertFalse(java.util.Arrays.equals(drought, frost));
    }

    @Test
    void HU40_ca3_theGameStateIsTheSameWithTheEffectsOnOrOff() {
        edu.ucc.farmsim.dominio.juego.MementoJuego con = jugar(true);
        edu.ucc.farmsim.dominio.juego.MementoJuego sin = jugar(false);

        assertEquals(sin, con);
    }

    private static edu.ucc.farmsim.dominio.juego.MementoJuego jugar(boolean efectos) {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PanelMapa panel = new PanelMapa(aplicacion.controlador, aplicacion.bus, (col, fila) -> { });
        panel.efectosFijarHabilitados(efectos);
        aplicacion.controlador.plantar(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA, "CARROT_SEED");
        aplicacion.controlador.agua(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA);
        for (int i = 0; i < 4; i++) {
            aplicacion.controlador.diaFin();
            aplicacion.controlador.agua(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA);
        }
        return aplicacion.estado().mementoCrear();
    }

    @Test
    void HU40_ca4_withParticlesActiveAFrameTakesLessThan33Milliseconds() {
        fx.onMundo("WINTER", "SNOWY");
        VistaMapa map = mapaHierba();
        BufferedImage imagen = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        DibujanteMapa dibujante = new DibujanteMapa();
        dibujante.renderizar(g, map, W, H, null, fx);

        long start = System.nanoTime();
        for (int i = 0; i < 30; i++) {
            now.addAndGet(33);
            fx.paso();
            dibujante.renderizar(g, map, W, H, null, fx);
        }
        double promedio = (System.nanoTime() - start) / 30 / 1_000_000.0;
        g.dispose();

        assertTrue(promedio < 33.0, "average frame: " + promedio + " ms");
        assertTrue(fx.particulas().size() <= EfectosVisuales.PARTICULAS_MAXIMOS);
    }

    @Test
    void HU40_ca4_los_efectos_puede_ser_cambiado_apagado_y_el_mapa_va_atras_a_su_sencillo_mirar() {
        BufferedImage sencillo = paint(null);
        fx.onMundo("WINTER", "SNOWY");
        fx.onDiaTerminado();
        assertNotEquals(pixelHierba(sencillo), pixelHierba(paint(fx)));

        fx.setEnabled(false);

        assertFalse(fx.isEnabled());
        assertTrue(fx.particulas().isEmpty());
        assertEquals(0f, fx.alfaNoche());
        assertEquals(pixelHierba(sencillo), pixelHierba(paint(fx)));
        assertFalse(fx.marcosNecesita());
    }

    private static int[] pixeles(BufferedImage imagen) {
        return imagen.getRGB(0, 0, W, H, null, 0, W);
    }
}
