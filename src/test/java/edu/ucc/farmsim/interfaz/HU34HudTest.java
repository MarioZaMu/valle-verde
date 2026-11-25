package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import edu.ucc.farmsim.interfaz.componentes.HudPanel;
import edu.ucc.farmsim.interfaz.componentes.ModeloHud;
import edu.ucc.farmsim.interfaz.pantallas.PresentadorPantallaJuego;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

/**
 * HU-34: la barra superior muestra dinero, fecha, clima y energía y se actualiza cuando recibe eventos. El modelo
 * ({@link ModeloHud}) es un observador sin Swing, así que se prueba sin pantalla.
 */
class HU34HudTest {

    private static final edu.ucc.farmsim.interfaz.pantallas.DialogosJuego NO_DIALOGOS = new edu.ucc.farmsim.apoyo.DialogosGrabacion();

    @Test
    void HU34_ca1_aNewGameShowsMoneyDateWeatherForecastAndFullEnergy() {
                // Dado un HUD que escucha el bus de eventos
        AplicacionTest aplicacion = AplicacionTest.create();
        ModeloHud hud = new ModeloHud();
        aplicacion.bus.suscribir(hud);

                // Cuando empieza una partida nueva
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);

                // Entonces el HUD muestra 500 m, Año 1 Primavera Día 1, Sunny con un pronóstico, y energía 100/100
        assertEquals("500 m", hud.textoDinero());
        assertEquals("Año 1 · Primavera · Día 1", hud.textoFecha());
        assertEquals("Soleado", hud.textoClima());
        assertTrue(hud.textoPronostico().startsWith("Mañana: "), hud.textoPronostico());
        assertEquals("100/100", hud.textoEnergia());
        assertEquals(1.0, hud.fraccionEnergia(), 0.0001);
    }

    @Test
    void HU34_ca2_energia_cambia_son_mostrado_cuando_el_evento_llega_sin_cualquier_sondeo() {
                // Dado un HUD con una partida iniciada
        AplicacionTest aplicacion = AplicacionTest.create();
        ModeloHud hud = new ModeloHud();
        aplicacion.bus.suscribir(hud);
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);

                // Cuando una acción baja la energía a 98 y se publica el evento
        aplicacion.bus.publish(new EventoJuego.EnergiaCambiada(98, 100));

                // Entonces el HUD muestra 98/100 y su barra está al 98 %
        assertEquals("98/100", hud.textoEnergia());
        assertEquals(0.98, hud.fraccionEnergia(), 0.0001);
        assertEquals("500 m", hud.textoDinero());
    }

    @Test
    void HU34_ca2_aSaleShowsTheNewMoneyAsSoonAsItsEventArrives() {
                // Dado un HUD con una partida iniciada y 3 zanahorias para vender
        AplicacionTest aplicacion = AplicacionTest.create();
        ModeloHud hud = new ModeloHud();
        aplicacion.bus.suscribir(hud);
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.estado().inventario().add(edu.ucc.farmsim.dominio.economia.TipoArticulo.CARROT, 3);

                // Cuando el jugador las vende
        int ganado = aplicacion.controlador.vender("CARROT", 3).value();

                // Entonces el HUD muestra el nuevo saldo sin esperar al final del día
        assertEquals((500 + ganado) + " m", hud.textoDinero());
        assertEquals("100/100", hud.textoEnergia(), "the rest of the HUD stays");
    }

    @Test
    void HU34_ca3_zeroEnergyShowsAnEmptyBarAndTheText0Of100NotJustAColor() {
        AplicacionTest aplicacion = AplicacionTest.create();
        ModeloHud hud = new ModeloHud();
        aplicacion.bus.suscribir(hud);
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);

        aplicacion.bus.publish(new EventoJuego.EnergiaCambiada(0, 100));

        assertEquals(0.0, hud.fraccionEnergia(), 0.0001);
        assertEquals("0/100", hud.textoEnergia());
    }

    @Test
    void HU34_ca4_terminando_el_dia_muestra_el_siguiente_dia_y_el_nuevo_pronostico() {
        AplicacionTest aplicacion = AplicacionTest.create();
        ModeloHud hud = new ModeloHud();
        aplicacion.bus.suscribir(hud);
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);

                // Cuando termina el día
        Resultado<VistaHud> resultado = aplicacion.controlador.diaFin();

                // Entonces el HUD muestra el día 2 y el pronóstico que se sorteó
        assertEquals("Año 1 · Primavera · Día 2", hud.textoFecha());
        assertEquals(Textos.clima(resultado.value().clima()), hud.textoClima());
        assertEquals("Mañana: " + Textos.clima(resultado.value().pronostico()), hud.textoPronostico());
        assertEquals("100/100", hud.textoEnergia());
    }

    @Test
    void HU34_antes_cualquier_juego_el_hud_muestra_guiones() {
        ModeloHud hud = new ModeloHud();

        assertEquals("—", hud.textoDinero());
        assertEquals("—", hud.textoFecha());
        assertEquals("—/—", hud.textoEnergia());
        assertEquals(0.0, hud.fraccionEnergia(), 0.0001);
        hud.onEvento(new EventoJuego.EnergiaCambiada(50, 100)); // no game yet: ignored without failing
        assertEquals("—/—", hud.textoEnergia());
    }

    @Test
    void HU34_everySeasonAndWeatherHasASpanishName() {
        for (Estacion estacion : Estacion.values()) {
            assertNotNull(Textos.estacion(estacion.name()));
            assertFalse(Textos.estacion(estacion.name()).equals(estacion.name()), "untranslated " + estacion);
        }
        for (Clima clima : Clima.values()) {
            assertFalse(Textos.clima(clima.name()).equals(clima.name()), "untranslated " + clima);
        }
        assertEquals("OTHER", Textos.clima("OTHER"));
    }

    @Test
    void HU34_el_swing_panel_muestra_los_textos_de_el_modelo_cuando_un_evento_llega() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.create();
        HudPanel panel = new HudPanel(aplicacion.bus);

        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        javax.swing.SwingUtilities.invokeAndWait(() -> { }); // deja que el panel termine su actualización en el hilo de Swing

        List<String> textos = new ArrayList<>();
        etiquetasRecolectar(panel, textos);
        assertTrue(textos.contains("500 m"), textos.toString());
        assertTrue(textos.contains("Año 1 · Primavera · Día 1"), textos.toString());
    }

    @Test
    void HU10_theEndDayButtonLogicShowsAMessageOnlyWhenTheDayCouldNotEnd() {
        AplicacionTest aplicacion = AplicacionTest.create();
        List<String> mensajes = new ArrayList<>();
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, new edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta(), mensajes::add, NO_DIALOGOS);

        presentador.onTerminarDia(); // sin partida en curso
        assertEquals(List.of("No hay una partida en curso."), mensajes);

        mensajes.clear();
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        presentador.onTerminarDia();
        assertTrue(mensajes.stream().allMatch(String::isBlank), "a successful end of day shows no message");
        assertEquals(2, aplicacion.controlador.hud().value().dia());
    }

    private static void etiquetasRecolectar(Container contenedor, List<String> textos) {
        for (Component hijo : contenedor.getComponents()) {
            if (hijo instanceof JLabel etiqueta) {
                textos.add(etiqueta.getText());
            }
            if (hijo instanceof Container anidado) {
                etiquetasRecolectar(anidado, textos);
            }
        }
    }
}
