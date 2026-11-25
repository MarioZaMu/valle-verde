package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.aplicacion.controlador.ControladorJuego;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.RepositorioGuardadoFalso;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.interfaz.pantallas.NavegadorPantalla;
import edu.ucc.farmsim.interfaz.pantallas.PantallaInicio;
import edu.ucc.farmsim.interfaz.pantallas.PresentadorPantallaInicio;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import org.junit.jupiter.api.Test;

/**
 * HU-33: pantalla de inicio con «Nueva partida», «Cargar» y «Salir». La lógica de la pantalla vive en {@link
 * PresentadorPantallaInicio}, que no tiene Swing, así que se prueba sin pantalla. También contiene el lado de UI de HU-28 ·
 * CA-3 (confirmación antes de descartar una partida en curso).
 */
class HU33PantallaInicioTest {

    /** Registra lo que el presentador le pide hacer a la navegación. */
    private static class NavegadorFalso implements NavegadorPantalla {
        final List<String> llamadas = new ArrayList<>();
        boolean respuestaAConfirmacion = true;
        int ranuraElegida;
        List<String> ofrecido = List.of();

        @Override public void inicioMostrar() { llamadas.add("start"); }
        @Override public void juegoMostrar() { llamadas.add("game"); }
        @Override public void mensajeMostrar(String texto) { llamadas.add("message:" + texto); }
        @Override public boolean confirmar(String texto) { llamadas.add("confirm:" + texto); return respuestaAConfirmacion; }
        @Override public int elegir(String titulo, List<String> opciones) {
            llamadas.add("choose:" + titulo);
            ofrecido = opciones;
            return ranuraElegida;
        }
        @Override public void exit() { llamadas.add("exit"); }
    }

    @Test
    void HU33_ca1_inicio_pantalla_ofrece_el_tres_botones_en_espaniol() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.create();
        PantallaInicio pantalla = new PantallaInicio(new PresentadorPantallaInicio(aplicacion.controlador, new NavegadorFalso(), () -> 1L));

        List<String> etiquetas = new ArrayList<>();
        botonesRecolectar(pantalla, etiquetas);

        assertEquals(List.of("Nueva partida", "Cargar", "Salir"), etiquetas);
        assertEquals("Nueva partida", Textos.JUEGO_INICIO_NUEVO);
        assertEquals("Cargar", Textos.CARGAR_INICIO);
        assertEquals("Salir", Textos.SALIR_INICIO);
    }

    /** El controlador, escribiendo el nombre de cada orden en {@code llamadas}. */
    private static edu.ucc.farmsim.aplicacion.controlador.ComandosJuego grabacion(AplicacionTest aplicacion, List<String> llamadas) {
        return (edu.ucc.farmsim.aplicacion.controlador.ComandosJuego) java.lang.reflect.Proxy.newProxyInstance(
                HU33PantallaInicioTest.class.getClassLoader(),
                new Class<?>[] {edu.ucc.farmsim.aplicacion.controlador.ComandosJuego.class},
                (proxy, method, args) -> {
                    llamadas.add(method.getName());
                    try {
                        return method.invoke(aplicacion.controlador, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @Test
    void RNF02_nuevo_juego_y_cargar_preparar_el_juego_pantalla_antes_su_primero_orden() {
        AplicacionTest aplicacion = AplicacionTest.create();
        List<String> orden = new ArrayList<>();
        NavegadorFalso navegador = new NavegadorFalso() {
            @Override
            public void juegoPreparar() {
                orden.add("preparar");
            }
        };
        PresentadorPantallaInicio presentador = new PresentadorPantallaInicio(grabacion(aplicacion, orden), navegador, () -> AplicacionTest.SEED);

        presentador.onJuegoNuevo();
        assertEquals("preparar", orden.get(0), String.valueOf(orden));
        assertTrue(orden.contains("juegoNuevo"));
        orden.clear();
        presentador.onCargar();
        assertEquals("preparar", orden.get(0), String.valueOf(orden));
    }

    @Test
    void HU33_ca2_newGameStartsAGameAndShowsTheGameScreen() {
        AplicacionTest aplicacion = AplicacionTest.create();
        NavegadorFalso navegador = new NavegadorFalso();
        PresentadorPantallaInicio presentador = new PresentadorPantallaInicio(aplicacion.controlador, navegador, () -> AplicacionTest.SEED);

        presentador.onJuegoNuevo();

        assertTrue(aplicacion.controlador.tieneJuegoEnProgreso());
        assertEquals(List.of("game"), navegador.llamadas);
    }

    @Test
    void HU33_ca3_loadWithAllSlotsEmptyShowsAMessageAndStaysOnTheStartScreen() {
        AplicacionTest aplicacion = AplicacionTest.create();
        NavegadorFalso navegador = new NavegadorFalso();
        PresentadorPantallaInicio presentador = new PresentadorPantallaInicio(aplicacion.controlador, navegador, () -> 1L);

        presentador.onCargar();

        assertEquals(List.of("message:No hay partidas guardadas."), navegador.llamadas);
        assertFalse(navegador.llamadas.contains("game"));
    }

    @Test
    void HU33_ca3_loadWithASavedGameOffersTheSlotsAndCancellingChangesNothing() {
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), new RepositorioGuardadoFalso().conRanura(2));
        NavegadorFalso navegador = new NavegadorFalso();

        new PresentadorPantallaInicio(aplicacion.controlador, navegador, () -> 1L).onCargar();

        assertEquals(List.of("choose:" + Textos.RANURA_ELEGIR), navegador.llamadas);
        assertEquals(3, navegador.ofrecido.size());
        assertFalse(aplicacion.controlador.tieneJuegoEnProgreso());
    }

    @Test
    void HU30_ca3_choosingASavedSlotLoadsItAndShowsTheGameScreen() {
        RepositorioGuardadoFalso repositorio = new RepositorioGuardadoFalso();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), repositorio);
        aplicacion.controlador.juegoNuevo(1L);
        aplicacion.controlador.diaFin();
        aplicacion.controlador.guardar(3);
        aplicacion.controlador.juegoNuevo(2L);
        NavegadorFalso navegador = new NavegadorFalso();
        navegador.ranuraElegida = 3;

        new PresentadorPantallaInicio(aplicacion.controlador, navegador, () -> 1L).onCargar();

        assertEquals(List.of("choose:" + Textos.RANURA_ELEGIR, "game"), navegador.llamadas);
        assertEquals(2, aplicacion.controlador.hud().value().dia());
    }

    @Test
    void HU31_choosingAnEmptySlotShowsTheRn35MessageAndStaysOnTheStartScreen() {
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), new RepositorioGuardadoFalso().conRanura(1));
        NavegadorFalso navegador = new NavegadorFalso();
        navegador.ranuraElegida = 3;

        new PresentadorPantallaInicio(aplicacion.controlador, navegador, () -> 1L).onCargar();

        assertEquals(List.of("choose:" + Textos.RANURA_ELEGIR, "message:No existe una partida en la ranura 3."),
                navegador.llamadas);
    }

    @Test
    void HU33_ca4_salir_pide_la_navegacion_a_cerrar_la_aplicacion() {
        AplicacionTest aplicacion = AplicacionTest.create();
        NavegadorFalso navegador = new NavegadorFalso();

        new PresentadorPantallaInicio(aplicacion.controlador, navegador, () -> 1L).onSalir();

        assertEquals(List.of("exit"), navegador.llamadas);
    }

    @Test
    void HU28_ca3_cancelando_la_confirmacion_conserva_el_juego_en_progreso() {
                // Dada una partida en curso donde se gastó energía
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.sesion.actual().energia().gastarIntentar(10);
        NavegadorFalso navegador = new NavegadorFalso();
        navegador.respuestaAConfirmacion = false;

                // Cuando el jugador elige «Nueva partida» y cancela la confirmación
        new PresentadorPantallaInicio(aplicacion.controlador, navegador, () -> AplicacionTest.SEED).onJuegoNuevo();

                // Entonces se preguntó la confirmación, no se mostró ninguna pantalla de juego y la partida no se tocó
        assertEquals(List.of("confirm:" + Textos.JUEGO_DESCARTAR_CONFIRMAR), navegador.llamadas);
        assertEquals(90, aplicacion.sesion.actual().energia().actual());
    }

    @Test
    void HU28_ca3_acceptingTheConfirmationStartsAFreshGame() {
        AplicacionTest aplicacion = AplicacionTest.create();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.sesion.actual().energia().gastarIntentar(10);
        NavegadorFalso navegador = new NavegadorFalso();

        new PresentadorPantallaInicio(aplicacion.controlador, navegador, () -> AplicacionTest.SEED).onJuegoNuevo();

        assertEquals(List.of("confirm:" + Textos.JUEGO_DESCARTAR_CONFIRMAR, "game"), navegador.llamadas);
        assertEquals(100, aplicacion.sesion.actual().energia().actual());
    }

    @Test
    void HU33_aFailedNewGameShowsTheErrorMessageInsteadOfTheGameScreen() {
        SesionJuego defectuoso = new SesionJuego() {
            @Override
            public void replace(edu.ucc.farmsim.dominio.juego.EstadoJuego estado) {
                throw new IllegalStateException("cannot start");
            }
        };
        ControladorJuego controlador = AplicacionTest.create(defectuoso, SecuenciaDia.estandar()).controlador;
        NavegadorFalso navegador = new NavegadorFalso();

        new PresentadorPantallaInicio(controlador, navegador, () -> 1L).onJuegoNuevo();

        assertEquals(List.of("message:Ocurrió un error inesperado. Tu partida no cambió."), navegador.llamadas);
    }

    private static void botonesRecolectar(Container contenedor, List<String> etiquetas) {
        for (Component hijo : contenedor.getComponents()) {
            if (hijo instanceof JButton boton) {
                etiquetas.add(boton.getText());
            }
            if (hijo instanceof Container anidado) {
                botonesRecolectar(anidado, etiquetas);
            }
        }
    }
}
