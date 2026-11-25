package edu.ucc.farmsim;

import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.cliente.ClienteJuegoRemoto;
import edu.ucc.farmsim.configuracion.AplicacionServidor;
import edu.ucc.farmsim.interfaz.pantallas.EnrutadorPantalla;
import edu.ucc.farmsim.interfaz.pantallas.VentanaPrincipal;
import edu.ucc.farmsim.interfaz.tema.UiTheme;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingUtilities;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Raíz de composición: el único lugar que arranca el servidor y la ventana y los une (DT-22). Sin argumentos arranca el
 * servidor Spring Boot en esta máquina y una ventana que le habla por HTTP; {@code --servidor} arranca solo el servidor y
 * {@code --cliente --url=...} solo la ventana. Con ambos, el servidor arranca en un hilo propio mientras se abre la
 * ventana, de modo que la pantalla de inicio aparece sin esperar a Spring (RNF-02); una orden enviada antes de que el
 * servidor esté listo lo espera.
 *
 * <p>Stories: HU-33, HU-43.
 */
public final class Main {

    private static final int SALIR_NO_MOSTRAR = 1;
    private static final int ARGUMENTOS_SALIR_MALOS = 2;
    private static final int SERVIDOR_SALIR_FALLIDO = 3;
    private static final Duration ESPERAR_INICIO_SERVIDOR = Duration.ofSeconds(30);

    private Main() {
                // solo punto de entrada
    }

    /**
     * Arranca la aplicación.
     *
     * @param args véase {@link OpcionesLanzamiento}
     */
    public static void main(String[] args) {
        OpcionesLanzamiento opciones;
        try {
            opciones = OpcionesLanzamiento.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(InfoJuego.NOMBRE + ": " + e.getMessage());
            System.exit(ARGUMENTOS_SALIR_MALOS);
            return;
        }
        if (opciones.ventanaNecesita() && GraphicsEnvironment.isHeadless()) {
            System.err.println(InfoJuego.NOMBRE + " necesita una pantalla para ejecutarse.");
            System.exit(SALIR_NO_MOSTRAR);
            return;
        }
        if (!opciones.ventanaNecesita()) {
            iniciarServidor(opciones);
            return;
        }
        if (!opciones.servidorNecesita()) {
            String url = opciones.url() != null ? opciones.url() : OpcionesLanzamiento.URL_PREDETERMINADO;
            ventanaAbrir(CompletableFuture.completedFuture(URI.create(url)));
            return;
        }
        CompletableFuture<URI> servidor = new CompletableFuture<>();
        Thread arranque = new Thread(() -> {
            try {
                servidor.complete(URI.create("http://127.0.0.1:" + iniciarServidor(opciones)));
            } catch (RuntimeException e) {
                servidor.completeExceptionally(e);
                System.err.println(InfoJuego.NOMBRE + ": el servidor no pudo arrancar: " + e.getMessage());
                System.exit(SERVIDOR_SALIR_FALLIDO);
            }
        }, "server-start");
        arranque.start();
        ventanaAbrir(servidor);
    }

    /** Arranca el servidor Spring Boot y devuelve el puerto en el que escucha. */
    private static int iniciarServidor(OpcionesLanzamiento opciones) {
        ConfigurableApplicationContext contexto = new SpringApplicationBuilder(AplicacionServidor.class)
                .headless(false)
                .run(opciones.argumentosServidor());
        return ((WebServerApplicationContext) contexto).getWebServer().getPort();
    }

    private static void ventanaAbrir(CompletableFuture<URI> servidor) {
        BusEvento bus = new BusEvento();
        ClienteJuegoRemoto cliente = new ClienteJuegoRemoto(servidor, bus, ESPERAR_INICIO_SERVIDOR);
        SwingUtilities.invokeLater(() -> {
            UiTheme.instalar();
            VentanaPrincipal ventana = new VentanaPrincipal(InfoJuego.NOMBRE);
            ventana.setContentPane(new EnrutadorPantalla(cliente, bus, System::currentTimeMillis, servidor));
            ventana.setVisible(true);
        });
    }
}
