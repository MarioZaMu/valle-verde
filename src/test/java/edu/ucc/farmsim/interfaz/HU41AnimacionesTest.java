package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaAnimal;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.interfaz.dibujo.DibujanteMapa;
import edu.ucc.farmsim.interfaz.dibujo.EfectosVisuales;
import edu.ucc.farmsim.interfaz.dibujo.GeometriaMapa;
import edu.ucc.farmsim.interfaz.dibujo.PanelMapa;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;

/** HU-41: los cultivos y los animales se mueven con suavidad, y el movimiento nunca toca el juego (RN-08, RN-25). */
class HU41AnimacionesTest {

    private static final int W = 1024;
    private static final int H = 600;

    private final AtomicLong now = new AtomicLong(0);
    private final EfectosVisuales fx = new EfectosVisuales(now::get, new Random(3));

    private static VistaMapa granja(String etapa, boolean conGallina) {
        List<VistaParcela> parcelas = new ArrayList<>();
        for (int fila = 0; fila < 10; fila++) {
            for (int col = 0; col < 16; col++) {
                parcelas.add(col == 5 && fila == 3
                        ? new VistaParcela(col, fila, "SOIL", "CARROT", etapa, true, 2)
                        : new VistaParcela(col, fila, "SOIL", null, null, false, 0));
            }
        }
        List<VistaEdificio> edificios = conGallina
                ? List.of(new VistaEdificio(0, "COOP", "Gallinero", 2, 7, 2, 2, 4, "Huevo", 0,
                        List.of(new VistaAnimal(0, "HEN", "Gallina", "HEALTHY", "Sana", 0, true, 0),
                                new VistaAnimal(1, "HEN", "Gallina", "HEALTHY", "Sana", 0, true, 0))))
                : List.of();
        return new VistaMapa(16, 10, parcelas, edificios);
    }

    private static int[] paint(VistaMapa map, EfectosVisuales efectos) {
        BufferedImage imagen = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        new DibujanteMapa().renderizar(g, map, W, H, null, efectos);
        g.dispose();
        var area = GeometriaMapa.ajustar(16, 10, W, H).limitesCelda(2, 7);
        return imagen.getRGB(area.x, area.y, area.width * 2, area.height * 2, null, 0, area.width * 2);
    }

    @Test
    void HU41_ca1_cada_animal_ciclos_por_dos_o_tres_marcos() {
        Set<Integer> marcos = new HashSet<>();
        for (int t = 0; t < 2_000; t += 50) {
            now.set(t);
            marcos.add(fx.marcoAnimal(0));
        }

        assertTrue(marcos.size() >= 2 && marcos.size() <= 3, "frames: " + marcos);
        assertEquals(EfectosVisuales.MARCOS_ANIMAL, 3);
        assertTrue(marcos.stream().allMatch(f -> f >= 0 && f < EfectosVisuales.MARCOS_ANIMAL));
    }

    @Test
    void HU41_ca1_theAnimalIsDrawnDifferentlyOnEachFrame() {
        VistaMapa map = granja("MATURE", true);
        fx.onMapa(map);
        now.set(0);
        int[] first = paint(map, fx);
        now.set(EfectosVisuales.MS_MARCO);
        int[] segundo = paint(map, fx);

        assertFalse(Arrays.equals(first, segundo));
    }

    @Test
    void HU41_ca1_dos_animales_hacer_no_mover_en_paso() {
        for (int t = 0; t < 1_000; t += 25) {
            now.set(t);
            if (fx.marcoAnimal(0) != fx.marcoAnimal(1)) {
                return;
            }
        }
        throw new AssertionError("both animals always show the same frame");
    }

    @Test
    void HU41_ca2_aCropThatChangesStageGrowsInAtMost500Milliseconds() {
        fx.onMapa(granja("SPROUT", false));
        assertEquals(1f, fx.escalaCultivo(5, 3));

        now.set(1_000);
        fx.onMapa(granja("GROWING", false));

        assertTrue(fx.escalaCultivo(5, 3) < 1f);
        assertTrue(EfectosVisuales.MS_CRECIMIENTO <= 500);
        now.set(1_000 + EfectosVisuales.MS_CRECIMIENTO);
        assertEquals(1f, fx.escalaCultivo(5, 3));
        assertEquals(1f, fx.escalaCultivo(0, 0));
    }

    @Test
    void HU41_ca2_el_primero_mapa_y_un_sin_cambios_mapa_animar_nada() {
        fx.onMapa(granja("SPROUT", false));
        fx.onMapa(granja("SPROUT", false));

        assertEquals(1f, fx.escalaCultivo(5, 3));
        assertFalse(fx.marcosNecesita());
    }

    @Test
    void HU41_ca3_el_estado_ya_avanzado_cuando_la_transicion_inicia_y_pasos_nunca_cambiar_eso() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        List<Integer> cuandoDiaAnunciado = new ArrayList<>();
        aplicacion.bus.suscribir(evento -> {
            if (evento instanceof EventoJuego.DiaTerminado terminado) {
                cuandoDiaAnunciado.add(aplicacion.controlador.hud().value().dia());
                assertEquals(terminado.hud().dia(), aplicacion.controlador.hud().value().dia());
                fx.onDiaTerminado();
            }
        });

        aplicacion.controlador.diaFin();
        var avanzado = aplicacion.estado().mementoCrear();
        for (int i = 0; i < 40; i++) {
            now.addAndGet(25);
            fx.paso();
        }

        assertEquals(List.of(2), cuandoDiaAnunciado);
        assertEquals(avanzado, aplicacion.estado().mementoCrear());
    }

    @Test
    void HU41_ca4_theViewMovesThemWithASwingTimerAndSwitchingThemOffStopsIt() throws Exception {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PanelMapa panel = new PanelMapa(aplicacion.controlador, aplicacion.bus, (col, fila) -> { });
        Field campoTemporizador = null;
        for (Field campo : PanelMapa.class.getDeclaredFields()) {
            if (campo.getType() == Timer.class) {
                campoTemporizador = campo;
            }
        }
        assertTrue(campoTemporizador != null, "MapPanel owns a javax.swing.Timer");
        campoTemporizador.setAccessible(true);
        aplicacion.controlador.diaFin();
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
        var antes = aplicacion.estado().mementoCrear();

        panel.efectosFijarHabilitados(false);

        assertFalse(panel.esAnimando());
        assertFalse(((Timer) campoTemporizador.get(panel)).isRunning());
        assertEquals(antes, aplicacion.estado().mementoCrear());
        panel.efectosFijarHabilitados(true);
        assertTrue(panel.efectos().isEnabled());
        aplicacion.controlador.diaFin();
        assertEquals(3, aplicacion.controlador.hud().value().dia());
        assertEquals(DatosJuego.FILA_TIERRA, 3);
    }
}
