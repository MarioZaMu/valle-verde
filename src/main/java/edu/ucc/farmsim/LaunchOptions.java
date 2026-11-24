package edu.ucc.farmsim;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que la línea de comandos le pide a {@link Main} que arranque (DT-22): el servidor y la ventana juntos (por
 * defecto), solo el servidor ({@code --server}) o solo la ventana ({@code --client}). Cualquier otro argumento pasa
 * intacto a Spring Boot, por ejemplo {@code --server.port=9000}.
 *
 * <p>Stories: none.
 *
 * @param mode        qué arrancar
 * @param port        puerto pedido con {@code --port}, o {@code null} para que el servidor elija uno libre
 * @param url         dirección del servidor pedida con {@code --url}, o {@code null} para la predeterminada
 * @param springArgs  los argumentos que no son nuestros
 */
public record LaunchOptions(Mode mode, Integer port, String url, List<String> springArgs) {

    /** Dirección que usa el cliente cuando no se da {@code --url}. */
    public static final String DEFAULT_URL = "http://127.0.0.1:8080";

    /** Qué arrancar. */
    public enum Mode { BOTH, SERVER_ONLY, CLIENT_ONLY }

    /**
     * Lee la línea de comandos.
     *
     * @param args los argumentos de {@code main}
     * @throws IllegalArgumentException si se dan {@code --server} y {@code --client} a la vez, o un puerto no es un
     *                                  número
     */
    public static LaunchOptions parse(String... args) {
        boolean server = false;
        boolean client = false;
        Integer port = null;
        String url = null;
        List<String> rest = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--server")) {
                server = true;
            } else if (arg.equals("--client")) {
                client = true;
            } else if (arg.equals("--port") && i + 1 < args.length) {
                port = parsePort(args[++i]);
            } else if (arg.startsWith("--port=")) {
                port = parsePort(arg.substring("--port=".length()));
            } else if (arg.equals("--url") && i + 1 < args.length) {
                url = args[++i];
            } else if (arg.startsWith("--url=")) {
                url = arg.substring("--url=".length());
            } else {
                rest.add(arg);
            }
        }
        if (server && client) {
            throw new IllegalArgumentException("--server and --client cannot be used together");
        }
        Mode mode = server ? Mode.SERVER_ONLY : client ? Mode.CLIENT_ONLY : Mode.BOTH;
        return new LaunchOptions(mode, port, url, List.copyOf(rest));
    }

    /** Verdadero si esta ejecución abre la ventana. */
    public boolean needsWindow() {
        return mode != Mode.SERVER_ONLY;
    }

    /** Verdadero si esta ejecución arranca el servidor. */
    public boolean needsServer() {
        return mode != Mode.CLIENT_ONLY;
    }

    /** Los argumentos de Spring Boot, con el puerto pedido agregado cuando hay uno. */
    public String[] serverArguments() {
        List<String> all = new ArrayList<>(springArgs);
        if (port != null) {
            all.add("--server.port=" + port);
        }
        return all.toArray(new String[0]);
    }

    private static int parsePort(String text) {
        try {
            int value = Integer.parseInt(text);
            if (value < 0 || value > 65535) {
                throw new IllegalArgumentException("port out of range: " + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("port is not a number: " + text, e);
        }
    }
}
