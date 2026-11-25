package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.DialogosGrabacion;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import edu.ucc.farmsim.interfaz.componentes.CapaAviso;
import edu.ucc.farmsim.interfaz.componentes.CategoriaAviso;
import edu.ucc.farmsim.interfaz.componentes.ColaAviso;
import edu.ucc.farmsim.interfaz.componentes.Herramienta;
import edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta;
import edu.ucc.farmsim.interfaz.componentes.PresentadorAviso;
import edu.ucc.farmsim.interfaz.componentes.VistaAviso;
import edu.ucc.farmsim.interfaz.pantallas.PresentadorPantallaJuego;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-39: avisos cortos que aparecen y se desvanecen sin interrumpir el juego. */
class HU39AvisosTest {

    private final AtomicLong now = new AtomicLong(1_000);
    private final ColaAviso cola = new ColaAviso(now::get);

    private static VistaHud hud(String estacion) {
        return new VistaHud(500, 1, estacion, 1, "SUNNY", "SUNNY", 100, 100);
    }

    @Test
    void HU39_ca1_aSuccessToastStaysThreeSecondsAndFadesInFourTenths() {
        cola.post(CategoriaAviso.SUCCESS, "Listo");

        assertEquals(1.0f, cola.visible().get(0).opacidad());
        now.addAndGet(3_000);
        assertEquals(1, cola.visible().size());
        assertEquals(1.0f, cola.visible().get(0).opacidad());
        now.addAndGet(200);
        float aMedias = cola.visible().get(0).opacidad();
        assertTrue(aMedias > 0.4f && aMedias < 0.6f, "fading: " + aMedias);
        now.addAndGet(200);
        assertTrue(cola.visible().isEmpty());
    }

    @Test
    void HU39_ca1_un_logro_aviso_dura_cinco_segundos() {
        cola.post(CategoriaAviso.ACHIEVEMENT, "Logro");

        now.addAndGet(4_999);
        assertEquals(1.0f, cola.visible().get(0).opacidad());
        now.addAndGet(401);
        assertTrue(cola.visible().isEmpty());
        assertEquals(5_000, CategoriaAviso.ACHIEVEMENT.msDuracion());
        assertEquals(3_000, CategoriaAviso.INFO.msDuracion());
        assertEquals(3_000, CategoriaAviso.ERROR.msDuracion());
    }

    @Test
    void HU39_ca2_aRejectionBecomesAnErrorToastWithTheSpanishMessage() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PresentadorAviso avisos = new PresentadorAviso(cola);
        ModeloHerramienta herramientas = new ModeloHerramienta();
        herramientas.seleccionar(Herramienta.SOW);
        herramientas.semillaSeleccionar("PUMPKIN_SEED");
        aplicacion.estado().inventario().add(edu.ucc.farmsim.dominio.economia.TipoArticulo.PUMPKIN_SEED, 2);
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, herramientas, texto -> { },
                new DialogosGrabacion(), avisos);

        presentador.onParcelaClicada(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA);

        List<VistaAviso> mostrado = cola.visible();
        assertEquals(1, mostrado.size());
        assertEquals(CategoriaAviso.ERROR, mostrado.get(0).categoria());
        assertEquals("No se puede sembrar Calabaza en Primavera: solo en Otoño.", mostrado.get(0).texto());
    }

    @Test
    void HU39_ca3_solo_cuatro_son_visible_y_las_quinto_espera_hasta_uno_desaparece() {
        for (int i = 1; i <= 5; i++) {
            cola.post(CategoriaAviso.INFO, "Aviso " + i);
        }

        assertEquals(4, cola.visible().size());
        assertEquals(1, cola.esperando());
        assertEquals("Aviso 1", cola.visible().get(0).texto());

        now.addAndGet(3_400);
        List<VistaAviso> luego = cola.visible();
        assertEquals(1, luego.size());
        assertEquals("Aviso 5", luego.get(0).texto());
        assertEquals(0, cola.esperando());
    }

    @Test
    void HU39_ca3_las_quinto_inicia_su_propio_conteo_cuando_eso_pasa_visible() {
        for (int i = 1; i <= 5; i++) {
            cola.post(CategoriaAviso.INFO, "Aviso " + i);
        }
        now.addAndGet(3_400);
        cola.visible();

        now.addAndGet(2_900);

        assertEquals(1, cola.visible().size());
        assertFalse(cola.esOcioso());
    }

    @Test
    void HU39_ca4_theStartOfASeasonShowsAnInformativeToast() {
        PresentadorAviso avisos = new PresentadorAviso(cola);
        avisos.onEvento(new EventoJuego.JuegoIniciado(hud("SPRING")));
        avisos.onEvento(new EventoJuego.DiaTerminado(hud("SPRING")));
        assertTrue(cola.visible().isEmpty());

        avisos.onEvento(new EventoJuego.DiaTerminado(hud("SUMMER")));

        List<VistaAviso> mostrado = cola.visible();
        assertEquals(1, mostrado.size());
        assertEquals(CategoriaAviso.INFO, mostrado.get(0).categoria());
        assertEquals("Comienza el Verano.", mostrado.get(0).texto());
    }

    @Test
    void HU39_ca4_aRealSeasonChangeReachesTheToastsThroughTheBus() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PresentadorAviso avisos = new PresentadorAviso(cola);
        aplicacion.bus.suscribir(avisos);
        aplicacion.controlador.hud();
        avisos.onEvento(new EventoJuego.JuegoIniciado(hud("SPRING")));
        DatosJuego.fechaFijar(1, Estacion.SPRING, 28);

        aplicacion.controlador.diaFin();

        assertEquals("Comienza el Verano.", cola.visible().get(0).texto());
    }

    @Test
    void HU39_ca1_anUnlockedAchievementShowsAFiveSecondToast() {
        PresentadorAviso avisos = new PresentadorAviso(cola);

        avisos.onEvento(new EventoJuego.LogroDesbloqueado("LG-01", "Primera cosecha", 100));

        VistaAviso aviso = cola.visible().get(0);
        assertEquals(CategoriaAviso.ACHIEVEMENT, aviso.categoria());
        assertEquals("¡Logro desbloqueado: Primera cosecha! +100 m", aviso.texto());
    }

    @Test
    void HU39_la_capa_pinta_los_avisos_y_nunca_atrapa_el_raton() {
        cola.post(CategoriaAviso.ERROR, "Algo falló");
        CapaAviso layer = new CapaAviso(cola);
        layer.setSize(600, 400);
        BufferedImage imagen = new BufferedImage(600, 400, BufferedImage.TYPE_INT_ARGB);
        var g = imagen.createGraphics();

        layer.paint(g);
        g.dispose();

        boolean pintado = false;
        for (int y = 0; y < 400 && !pintado; y++) {
            for (int x = 0; x < 600; x++) {
                if ((imagen.getRGB(x, y) >>> 24) != 0) {
                    pintado = true;
                    break;
                }
            }
        }
        assertTrue(pintado);
        assertFalse(layer.contains(300, 200));
    }

    @Test
    void HU39_publica_desde_cualquier_hilo_son_conservado() throws InterruptedException {
        List<Thread> hilos = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Thread hilo = new Thread(() -> cola.post(CategoriaAviso.INFO, "x"));
            hilos.add(hilo);
            hilo.start();
        }
        for (Thread hilo : hilos) {
            hilo.join();
        }

        assertEquals(4, cola.visible().size());
        assertEquals(4, cola.esperando());
    }
}
