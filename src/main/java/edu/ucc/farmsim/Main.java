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
 * Composition root: the only place that starts the server and the window and joins them (DT-22). With no
 * arguments it starts the Spring Boot server on this machine and a window that talks to it over HTTP;
 * {@code --server} starts only the server and {@code --client --url=...} only the window. With both, the server
 * starts on a thread of its own while the window opens, so the start screen appears without waiting for Spring
 * (RNF-02); an order sent before the server is up waits for it.
 *
 * <p>Stories: HU-33, HU-43.
 */
public final class Main {

    private static final int EXIT_NO_DISPLAY = 1;
    private static final int EXIT_BAD_ARGUMENTS = 2;
    private static final int EXIT_SERVER_FAILED = 3;
    private static final Duration SERVER_START_WAIT = Duration.ofSeconds(30);

    private Main() {
        // entry point only
    }

    /**
     * Starts the application.
     *
     * @param args see {@link LaunchOptions}
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

    /** Starts the Spring Boot server and returns the port it listens on. */
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
