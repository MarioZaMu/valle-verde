package edu.ucc.farmsim.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.ConfiguracionTestSinMongo;
import edu.ucc.farmsim.configuracion.AplicacionServidor;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;
import edu.ucc.farmsim.interfaz.dibujo.PanelMapa;
import edu.ucc.farmsim.interfaz.pantallas.EnrutadorPantalla;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Las pantallas reales de Swing, manejadas por el {@link ClienteJuegoRemoto} real, contra el servidor real: lo que ve el
 * jugador después de pulsar los botones es lo mismo que con el controlador en proceso (DT-17, DT-19).
 */
@SpringBootTest(classes = AplicacionServidor.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("no-mongo")
@Import(ConfiguracionTestSinMongo.class)
class IntegracionPantallasRemotaTest {

    @LocalServerPort
    private int puerto;
    @Autowired
    private SesionJuego sesionServidor;

    @Test
    void HU33_nuevo_juego_luego_sembrar_luego_fin_dia_trabajo_por_el_servidor_y_los_eventos_refrescar_los_pantallas() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        RelojJuego.getInstance().reiniciarParaTests();
        sesionServidor.replace(null);
        BusEvento busCliente = new BusEvento();
        ClienteJuegoRemoto cliente = new ClienteJuegoRemoto(URI.create("http://127.0.0.1:" + puerto), busCliente);
        EnrutadorPantalla enrutador = new EnrutadorPantalla(cliente, busCliente, () -> AplicacionTest.SEED);

                // Cuando el jugador pulsa «Nueva partida»
        boton(enrutador, "Nueva partida").doClick();
        flush();

                // Entonces el servidor tiene la partida y la barra superior del cliente la muestra, actualizada por el evento
        // GameStarted
        assertTrue(sesionServidor.tieneJuego());
        assertTrue(etiquetas(enrutador).contains("500 m"), etiquetas(enrutador).toString());

                // Cuando el jugador elige la herramienta Sembrar y hace clic en una parcela de tierra
        boton(enrutador, "1 Sembrar").doClick();
        PanelMapa map = first(enrutador, PanelMapa.class);
        map.setSize(1024, 600);
        var celda = map.geometria().limitesCelda(5, 3);
        map.clicManejar(celda.x + 5, celda.y + 5);
        flush();

                // Entonces el cultivo existe en el servidor y el mapa y la barra de energía del cliente se actualizaron por
        // eventos
        assertTrue(sesionServidor.actual().granja().parcelaEn(5, 3).tieneCultivo());
        assertNotNull(map.vistaActual().parcelaEn(5, 3).cultivo());
        assertEquals("98/100", first(enrutador, JProgressBar.class).getString());

                // Cuando el jugador termina el día
        boton(enrutador, "Terminar día").doClick();
        flush();

                // Entonces la barra superior muestra el día 2 y la energía vuelve a estar completa
        assertTrue(etiquetas(enrutador).contains("Año 1 · Primavera · Día 2"), etiquetas(enrutador).toString());
        assertEquals("100/100", first(enrutador, JProgressBar.class).getString());
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static <T extends Component> T first(Container raiz, Class<T> tipo) {
        List<T> encontrado = new ArrayList<>();
        collect(raiz, tipo, encontrado);
        assertTrue(!encontrado.isEmpty(), "no " + tipo.getSimpleName());
        return encontrado.get(0);
    }

    private static AbstractButton boton(Container raiz, String texto) {
        List<AbstractButton> encontrado = new ArrayList<>();
        collect(raiz, AbstractButton.class, encontrado);
        AbstractButton coincidir = encontrado.stream().filter(b -> texto.equals(b.getText())).findFirst().orElse(null);
        assertNotNull(coincidir, "no button '" + texto + "'");
        return coincidir;
    }

    private static List<String> etiquetas(Container raiz) {
        List<JLabel> encontrado = new ArrayList<>();
        collect(raiz, JLabel.class, encontrado);
        return encontrado.stream().map(JLabel::getText).toList();
    }

    private static <T extends Component> void collect(Container contenedor, Class<T> tipo, List<T> encontrado) {
        for (Component hijo : contenedor.getComponents()) {
            if (tipo.isInstance(hijo)) {
                encontrado.add(tipo.cast(hijo));
            }
            if (hijo instanceof Container anidado) {
                collect(anidado, tipo, encontrado);
            }
        }
    }
}
