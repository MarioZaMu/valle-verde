package edu.ucc.farmsim;

import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.client.RemoteGameClient;
import edu.ucc.farmsim.config.ServerApplication;
import edu.ucc.farmsim.ui.screens.MainWindow;
import edu.ucc.farmsim.ui.screens.ScreenRouter;
import edu.ucc.farmsim.ui.theme.UiTheme;
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
 * servidor Spring Boot en esta máquina y una ventana que le habla por HTTP; {@code --server} arranca solo el servidor y
 * {@code --client --url=...} solo la ventana. Con ambos, el servidor arranca en un hilo propio mientras se abre la
 * ventana, de modo que la pantalla de inicio aparece sin esperar a Spring (RNF-02); una orden enviada antes de que el
 * servidor esté listo lo espera.
 *
 * <p>Stories: HU-33, HU-43.
 */
public final class Main {

    private static final int EXIT_NO_DISPLAY = 1;
    private static final int EXIT_BAD_ARGUMENTS = 2;
    private static final int EXIT_SERVER_FAILED = 3;
    private static final Duration SERVER_START_WAIT = Duration.ofSeconds(30);

    private Main() {
                // solo punto de entrada
    }

    /**
     * Arranca la aplicación.
     *
     * @param args véase {@link LaunchOptions}
     */
    public static void main(String[] args) {
        LaunchOptions options;
        try {
            options = LaunchOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(GameInfo.NAME + ": " + e.getMessage());
            System.exit(EXIT_BAD_ARGUMENTS);
            return;
        }
        if (options.needsWindow() && GraphicsEnvironment.isHeadless()) {
            System.err.println(GameInfo.NAME + " necesita una pantalla para ejecutarse.");
            System.exit(EXIT_NO_DISPLAY);
            return;
        }
        if (!options.needsWindow()) {
            startServer(options);
            return;
        }
        if (!options.needsServer()) {
            String url = options.url() != null ? options.url() : LaunchOptions.DEFAULT_URL;
            openWindow(CompletableFuture.completedFuture(URI.create(url)));
            return;
        }
        CompletableFuture<URI> server = new CompletableFuture<>();
        Thread starter = new Thread(() -> {
            try {
                server.complete(URI.create("http://127.0.0.1:" + startServer(options)));
            } catch (RuntimeException e) {
                server.completeExceptionally(e);
                System.err.println(GameInfo.NAME + ": el servidor no pudo arrancar: " + e.getMessage());
                System.exit(EXIT_SERVER_FAILED);
            }
        }, "server-start");
        starter.start();
        openWindow(server);
    }

    /** Arranca el servidor Spring Boot y devuelve el puerto en el que escucha. */
    private static int startServer(LaunchOptions options) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ServerApplication.class)
                .headless(false)
                .run(options.serverArguments());
        return ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    private static void openWindow(CompletableFuture<URI> server) {
        EventBus bus = new EventBus();
        RemoteGameClient client = new RemoteGameClient(server, bus, SERVER_START_WAIT);
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            MainWindow window = new MainWindow(GameInfo.NAME);
            window.setContentPane(new ScreenRouter(client, bus, System::currentTimeMillis, server));
            window.setVisible(true);
        });
    }
}
