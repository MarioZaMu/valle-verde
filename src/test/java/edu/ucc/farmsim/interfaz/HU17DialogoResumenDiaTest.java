package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DialogosGrabacion;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.interfaz.componentes.DialogoResumenDia;
import edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta;
import edu.ucc.farmsim.interfaz.componentes.TextoResumenDia;
import edu.ucc.farmsim.interfaz.pantallas.PresentadorPantallaJuego;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

/** El resumen que se muestra cuando termina el día (HU-17). */
class HU17DialogoResumenDiaTest {

    private static final VistaResumenDia RESUMEN = new VistaResumenDia(1, "SPRING", 1, 79, 150, 500, 429,
            List.of("Se marchitó 1 cultivo."));

    @Test
    void HU17_ca1_los_lineas_mostrar_ingresos_gastos_y_ambos_saldos() {
        assertEquals(List.of("Año 1 · Primavera · Día 1", "Ingresos: 79 m", "Gastos: 150 m", "Saldo anterior: 500 m",
                "Saldo nuevo: 429 m", "Se marchitó 1 cultivo."), TextoResumenDia.lineas(RESUMEN));
    }

    @Test
    void HU17_ca3_aQuietDayShowsZeroesAndSinNovedades() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DialogosGrabacion dialogos = new DialogosGrabacion();
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, new ModeloHerramienta(), m -> { }, dialogos);
        aplicacion.bus.suscribir(evento -> {
            if (evento instanceof edu.ucc.farmsim.aplicacion.evento.EventoJuego.ResumenDiaListo listo) {
                presentador.onResumen(listo.resumen());
            }
        });

        presentador.onTerminarDia();

        assertEquals(1, dialogos.resumenes.size());
        List<String> lineas = TextoResumenDia.lineas(dialogos.resumenes.get(0));
        assertTrue(lineas.contains("Ingresos: 0 m"));
        assertTrue(lineas.contains("Gastos: 0 m"));
        assertEquals("Sin novedades", lineas.get(lineas.size() - 1));
    }

    @Test
    void HU17_ca1_aSaleAndAPurchaseReachTheDialogThroughTheEventBus() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);
        aplicacion.controlador.comprar("CORN_SEED", 5);
        aplicacion.controlador.vender("CARROT", 3);
        DialogosGrabacion dialogos = new DialogosGrabacion();
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, new ModeloHerramienta(), m -> { }, dialogos);
        aplicacion.bus.suscribir(evento -> {
            if (evento instanceof edu.ucc.farmsim.aplicacion.evento.EventoJuego.ResumenDiaListo listo) {
                presentador.onResumen(listo.resumen());
            }
        });

        presentador.onTerminarDia();

        List<String> lineas = TextoResumenDia.lineas(dialogos.resumenes.get(0));
        assertTrue(lineas.contains("Ingresos: 79 m"));
        assertTrue(lineas.contains("Gastos: 150 m"));
        assertTrue(lineas.contains("Saldo anterior: 500 m"));
        assertTrue(lineas.contains("Saldo nuevo: 429 m"));
    }

    @Test
    void HU17_el_dialogo_hace_no_bloquear_el_juego_y_cierra() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        DialogoResumenDia dialogo = new DialogoResumenDia(null, RESUMEN);

        assertFalse(dialogo.isModal());
        List<String> mostrado = new ArrayList<>();
        etiquetas(dialogo, mostrado);
        assertEquals(TextoResumenDia.lineas(RESUMEN), mostrado);
        dialogo.dispose();
    }

    private static void etiquetas(Container contenedor, List<String> textos) {
        for (Component hijo : contenedor.getComponents()) {
            if (hijo instanceof JLabel etiqueta) {
                textos.add(etiqueta.getText());
            }
            if (hijo instanceof Container interior) {
                etiquetas(interior, textos);
            }
        }
    }
}
