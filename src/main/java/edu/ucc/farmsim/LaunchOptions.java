package edu.ucc.farmsim;

import java.util.ArrayList;
import java.util.List;

/**
 * What the command line asks {@link Main} to start (DT-22): the server and the window together (default),
 * only the server ({@code --server}) or only the window ({@code --client}). Any other argument goes to
 * Spring Boot untouched, for example {@code --server.port=9000}.
 *
 * <p>Stories: none.
 *
 * @param mode        what to start
 * @param port        port requested with {@code --port}, or {@code null} to let the server pick a free one
 * @param url         server address requested with {@code --url}, or {@code null} for the default
 * @param springArgs  the arguments that are not ours
 */
public record LaunchOptions(Mode mode, Integer port, String url, List<String> springArgs) {

    /** Address the client uses when {@code --url} is not given. */
    public static final String DEFAULT_URL = "http://127.0.0.1:8080";

    /** What to start. */
    public enum Mode { BOTH, SERVER_ONLY, CLIENT_ONLY }

    /**
     * Reads the command line.
     *
     * @param args the arguments of {@code main}
     * @throws IllegalArgumentException if both {@code --server} and {@code --client} are given, or a port is not a number
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

    /** True if this run opens the window. */
    public boolean needsWindow() {
        return mode != Mode.SERVER_ONLY;
    }

    /** True if this run starts the server. */
    public boolean needsServer() {
        return mode != Mode.CLIENT_ONLY;
    }

    /** The Spring Boot arguments, with the requested port added when there is one. */
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
