package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.interfaz.componentes.Atajo;
import edu.ucc.farmsim.interfaz.componentes.Herramienta;
import edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta;
import edu.ucc.farmsim.interfaz.pantallas.PresentadorPantallaJuego;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * HU-36: elegir una herramienta con las teclas 1 a 5 o con la barra y actuar sobre una parcela haciendo clic en ella.
 * El modelo de herramientas y el presentador no tienen Swing, así que todo el comportamiento se prueba sin pantalla.
 * Los atajos de mercado, inventario, logros y guardado se enlazan cuando existen sus pantallas (fases 5.3 y 5.5).
 */
class HU36HerramientasYClicsTest {

    private static final edu.ucc.farmsim.interfaz.pantallas.DialogosJuego NO_DIALOGOS = new edu.ucc.farmsim.apoyo.DialogosGrabacion();

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    private final List<String> mensajes = new ArrayList<>();
    private final ModeloHerramienta herramientas = new ModeloHerramienta();

    private PresentadorPantallaJuego presentador(AplicacionTest aplicacion) {
        return new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add, NO_DIALOGOS);
    }

    @Test
    void HU36_ca1_claves_uno_a_cinco_seleccionar_el_cinco_herramientas_y_escape_deselecciona() {
        assertNull(herramientas.actual());

        assertTrue(herramientas.claveManejar('1'));
        assertEquals(Herramienta.SOW, herramientas.actual());
        herramientas.claveManejar('2');
        assertEquals(Herramienta.WATER, herramientas.actual());
        herramientas.claveManejar('3');
        assertEquals(Herramienta.HARVEST, herramientas.actual());
        herramientas.claveManejar('4');
        assertEquals(Herramienta.BUILD, herramientas.actual());
        herramientas.claveManejar('5');
        assertEquals(Herramienta.FEED, herramientas.actual());

        herramientas.deseleccionar();
        assertNull(herramientas.actual());
        assertFalse(herramientas.claveManejar('9'), "other keys are not tool keys");
    }

    @Test
    void HU36_ca1_la_barra_y_los_claves_compartir_la_mismo_seleccion_y_notificar_oyentes() {
        List<Herramienta> visto = new ArrayList<>();
        herramientas.addChangeListener(() -> visto.add(herramientas.actual()));

        herramientas.seleccionar(Herramienta.WATER);
        herramientas.claveManejar('1');
        herramientas.deseleccionar();

        assertEquals(java.util.Arrays.asList(Herramienta.WATER, Herramienta.SOW, null), visto);
    }

    @Test
    void HU36_ca1_everyToolHasASpanishLabelAndItsKey() {
        assertEquals("Sembrar", Textos.herramienta(Herramienta.SOW));
        assertEquals("Regar", Textos.herramienta(Herramienta.WATER));
        assertEquals("Cosechar", Textos.herramienta(Herramienta.HARVEST));
        assertEquals("Construir", Textos.herramienta(Herramienta.BUILD));
        assertEquals("Alimentar", Textos.herramienta(Herramienta.FEED));
        assertEquals('1', Herramienta.SOW.clave());
        assertEquals(KeyEvent.VK_5, Herramienta.FEED.codigoClave());
    }

    @Test
    void HU36_ca2_clickingAPlotWithTheSowToolAndASeedSowsIt() {
                // Dada la herramienta Sembrar con semillas de zanahoria seleccionadas
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        herramientas.seleccionar(Herramienta.SOW);
        herramientas.semillaSeleccionar("CARROT_SEED");

                // Cuando el jugador hace clic en una parcela de tierra
        presentador(aplicacion).onParcelaClicada(COL, FILA);

                // Entonces el cultivo se sembró (HU-01) y no se muestra ningún error
        assertNotNull(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo());
        assertEquals(9, aplicacion.estado().inventario().count(TipoArticulo.CARROT_SEED));
        assertTrue(mensajes.isEmpty() || mensajes.stream().allMatch(String::isEmpty), mensajes.toString());
    }

    @Test
    void HU36_ca2_la_agua_herramienta_riega_el_clicado_cultivo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 0);
        herramientas.seleccionar(Herramienta.WATER);

        presentador(aplicacion).onParcelaClicada(COL, FILA);

        assertTrue(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo().esRegado());
    }

    @Test
    void HU36_ca3_aRejectedActionShowsTheSpanishMessageAndChangesNothing() {
                // Dada la herramienta Sembrar y sin energía
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().energia().set(0);
        herramientas.seleccionar(Herramienta.SOW);
        MementoJuego antes = aplicacion.estado().mementoCrear();

                // Cuando el jugador hace clic en una parcela de tierra
        presentador(aplicacion).onParcelaClicada(COL, FILA);

                // Entonces el estado es el mismo y el mensaje explica por qué
        assertEquals(antes, aplicacion.estado().mementoCrear());
        assertEquals(List.of("Energía insuficiente: necesitas 2 y tienes 0."), mensajes);
    }

    @Test
    void HU36_ca3_withoutAToolAClickDoesNothingAtAll() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        MementoJuego antes = aplicacion.estado().mementoCrear();

        presentador(aplicacion).onParcelaClicada(COL, FILA);

        assertEquals(antes, aplicacion.estado().mementoCrear());
        assertTrue(mensajes.isEmpty());
    }

    @Test
    void HU36_ca4_theHarvestToolHarvestsAMatureCropAndClearsAWitheredOne() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 4);
        DatosJuego.plantar(aplicacion.estado(), COL + 1, FILA, TipoCultivo.CARROT, 2, 3, false, true);
        herramientas.seleccionar(Herramienta.HARVEST);

        presentador(aplicacion).onParcelaClicada(COL, FILA);
        presentador(aplicacion).onParcelaClicada(COL + 1, FILA);

        assertEquals(1, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
        assertNull(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo());
        assertNull(aplicacion.estado().granja().parcelaEn(COL + 1, FILA).cultivo());
        assertEquals(100 - 2 - 1, aplicacion.estado().energia().actual());
                // (recoger los productos de un edificio con esta misma herramienta llega con HU-22)
    }

    @Test
    void HU36_ca5_cada_atajo_es_mapeado_a_su_clave() {
        assertEquals(KeyEvent.VK_ENTER, Atajo.END_DAY.codigoClave());
        assertEquals(KeyEvent.VK_M, Atajo.MARKET.codigoClave());
        assertEquals(KeyEvent.VK_I, Atajo.INVENTORY.codigoClave());
        assertEquals(KeyEvent.VK_L, Atajo.ACHIEVEMENTS.codigoClave());
        assertEquals(KeyEvent.VK_F5, Atajo.QUICK_SAVE.codigoClave());
        assertEquals(KeyEvent.VK_F9, Atajo.QUICK_LOAD.codigoClave());
        assertEquals(6, Atajo.values().length);
    }

    @Test
    void HU36_ca5_enterEndsTheDayAndLOpensTheAchievements() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        edu.ucc.farmsim.apoyo.DialogosGrabacion dialogos = new edu.ucc.farmsim.apoyo.DialogosGrabacion();
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add, dialogos);

        presentador.onAtajo(Atajo.END_DAY);
        presentador.onAtajo(Atajo.ACHIEVEMENTS);

        assertEquals(2, aplicacion.controlador.hud().value().dia());
        assertTrue(mensajes.stream().allMatch(String::isBlank));
        assertEquals(List.of("logros"), dialogos.abierto);
    }

    @Test
    void HU29_ca1_el_guardado_boton_siempre_abre_los_ranuras_a_elegir_donde_a_guardado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        edu.ucc.farmsim.apoyo.DialogosGrabacion dialogos = new edu.ucc.farmsim.apoyo.DialogosGrabacion();
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add, dialogos);

        presentador.onGuardar();
        presentador.ranuraUsar(2);
        presentador.onGuardar();

        assertEquals(List.of("slots:save", "slots:save"), dialogos.abierto, "even after F5 used a slot");
        assertTrue(mensajes.isEmpty(), "nothing is saved until a slot is chosen");
    }

    @Test
    void HU36_ca5_f5AndF9OpenTheSlotsTheFirstTimeAndThenUseTheSlotUsedLast() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        edu.ucc.farmsim.apoyo.DialogosGrabacion dialogos = new edu.ucc.farmsim.apoyo.DialogosGrabacion();
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add, dialogos);

        presentador.onAtajo(Atajo.QUICK_SAVE);
        presentador.onAtajo(Atajo.QUICK_LOAD);
        assertEquals(List.of("slots:save", "slots:load"), dialogos.abierto);

        presentador.ranuraUsar(2);
        presentador.onAtajo(Atajo.QUICK_SAVE);
        aplicacion.controlador.diaFin();
        presentador.onAtajo(Atajo.QUICK_LOAD);

        assertEquals(List.of("Partida guardada en la ranura 2.", "Partida cargada de la ranura 2."), mensajes);
        assertEquals(1, aplicacion.controlador.hud().value().dia());
        assertEquals(2, presentador.ranuraUltima());
    }

    @Test
    void HU36_quickSaveAndLoadShowTheRn35MessageWhenTheStorageFails() {
        edu.ucc.farmsim.apoyo.RepositorioGuardadoFalso repositorio = new edu.ucc.farmsim.apoyo.RepositorioGuardadoFalso();
        AplicacionTest aplicacion = AplicacionTest.create(new edu.ucc.farmsim.aplicacion.servicio.SesionJuego(),
                edu.ucc.farmsim.dominio.juego.SecuenciaDia.estandar(), repositorio);
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add,
                new edu.ucc.farmsim.apoyo.DialogosGrabacion());
        presentador.ranuraUsar(1);
        repositorio.inalcanzable();

        presentador.onAtajo(Atajo.QUICK_SAVE);
        presentador.onAtajo(Atajo.QUICK_LOAD);

        assertEquals(List.of("No se pudo acceder a las partidas guardadas: MongoDB no respondió.",
                "No se pudo acceder a las partidas guardadas: MongoDB no respondió."), mensajes);
    }

    @Test
    void HU36_la_seleccionado_semilla_predeterminados_a_zanahoria_y_solo_semillas_son_aceptado() {
        assertEquals("CARROT_SEED", herramientas.semillaSeleccionada());
        herramientas.semillaSeleccionar("PUMPKIN_SEED");
        assertEquals("PUMPKIN_SEED", herramientas.semillaSeleccionada());
        try {
            herramientas.semillaSeleccionar("EGG");
        } catch (IllegalArgumentException esperado) {
            return;
        }
        throw new AssertionError("A non-seed item must be rejected");
    }

    @Test
    void HU36_ca4_theHarvestToolCollectsTheProductsOfABuilding() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        var gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.casa(gallinero, edu.ucc.farmsim.dominio.animal.TipoAnimal.HEN,
                edu.ucc.farmsim.dominio.animal.CondicionAnimal.HEALTHY, 0, false, 2);
        herramientas.seleccionar(Herramienta.HARVEST);

        presentador(aplicacion).onParcelaClicada(DatosJuego.COL_HIERBA + 1, DatosJuego.FILA_HIERBA + 1);

        assertEquals(2, aplicacion.estado().inventario().count(TipoArticulo.EGG));
        assertEquals(99, aplicacion.estado().energia().actual());
        assertEquals(List.of(""), mensajes);
    }

    @Test
    void HU36_ca4_collectingFromABuildingWithNothingWaitingShowsTheNotice() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.gallinero(aplicacion.estado());
        herramientas.seleccionar(Herramienta.HARVEST);

        presentador(aplicacion).onParcelaClicada(DatosJuego.COL_HIERBA, DatosJuego.FILA_HIERBA);

        assertEquals(List.of("No hay productos que recoger."), mensajes);
    }

    @Test
    void HU36_el_alimentar_herramienta_alimenta_el_edificio_bajo_el_clic_y_dice_cuando_alli_es_ninguno() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        var gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.gallina(gallinero);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 1);
        herramientas.seleccionar(Herramienta.FEED);

        presentador(aplicacion).onParcelaClicada(COL, FILA);
        presentador(aplicacion).onParcelaClicada(DatosJuego.COL_HIERBA, DatosJuego.FILA_HIERBA);

        assertEquals(List.of(Textos.NO_AQUI_EDIFICIO, ""), mensajes);
        assertTrue(gallinero.animales().get(0).esHoyAlimentado());
        assertEquals(0, aplicacion.estado().inventario().count(TipoArticulo.HAY));
    }

    @Test
    void HU36_el_alimentar_herramienta_muestra_el_rechazo_de_el_controlador() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.gallina(DatosJuego.gallinero(aplicacion.estado()));
        herramientas.seleccionar(Herramienta.FEED);

        presentador(aplicacion).onParcelaClicada(DatosJuego.COL_HIERBA, DatosJuego.FILA_HIERBA);

        assertEquals(List.of("Heno insuficiente: necesitas 1."), mensajes);
    }

    @Test
    void HU36_theBuildToolOpensTheConstructionDialogOnTheClickedPlot() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        edu.ucc.farmsim.apoyo.DialogosGrabacion dialogos = new edu.ucc.farmsim.apoyo.DialogosGrabacion();
        herramientas.seleccionar(Herramienta.BUILD);

        new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add, dialogos).onParcelaClicada(2, 7);

        assertEquals(List.of("construction:2,7"), dialogos.abierto);
        assertTrue(aplicacion.estado().granja().edificios().isEmpty());
    }

    @Test
    void HU36_withoutAToolClickingABuildingOpensItsPanelAndClickingAPlotDoesNothing() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.gallinero(aplicacion.estado());
        edu.ucc.farmsim.apoyo.DialogosGrabacion dialogos = new edu.ucc.farmsim.apoyo.DialogosGrabacion();
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add, dialogos);

        presentador.onParcelaClicada(COL, FILA);
        presentador.onParcelaClicada(DatosJuego.COL_HIERBA + 1, DatosJuego.FILA_HIERBA);

        assertEquals(List.of("building:0"), dialogos.abierto);
        assertTrue(mensajes.isEmpty());
    }
}
