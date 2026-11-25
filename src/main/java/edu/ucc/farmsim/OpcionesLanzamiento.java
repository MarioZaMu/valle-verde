package edu.ucc.farmsim;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que la línea de comandos le pide a {@link Main} que arranque (DT-22): el servidor y la ventana juntos (por
 * defecto), solo el servidor ({@code --servidor}) o solo la ventana ({@code --cliente}). Cualquier otro argumento pasa
 * intacto a Spring Boot, por ejemplo {@code --servidor.puerto=9000}.
 *
 * <p>Stories: none.
 *
 * @param modo        qué arrancar
 * @param puerto        puerto pedido con {@code --puerto}, o {@code null} para que el servidor elija uno libre
 * @param url         dirección del servidor pedida con {@code --url}, o {@code null} para la predeterminada
 * @param argsPrimavera  los argumentos que no son nuestros
 */
public record OpcionesLanzamiento(Modo modo, Integer puerto, String url, List<String> argsPrimavera) {

    /** Dirección que usa el cliente cuando no se da {@code --url}. */
    public static final String URL_PREDETERMINADO = "http://127.0.0.1:8080";

    /** Qué arrancar. */
    public enum Modo { BOTH, SERVER_ONLY, CLIENT_ONLY }

    /**
     * Lee la línea de comandos.
     *
     * @param args los argumentos de {@code main}
     * @throws IllegalArgumentException si se dan {@code --servidor} y {@code --cliente} a la vez, o un puerto no es un
     *                                  número
     */
    public static OpcionesLanzamiento parse(String... args) {
        boolean servidor = false;
        boolean cliente = false;
        Integer puerto = null;
        String url = null;
        List<String> resto = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--server")) {
                servidor = true;
            } else if (arg.equals("--client")) {
                cliente = true;
            } else if (arg.equals("--port") && i + 1 < args.length) {
                puerto = puertoAnalizar(args[++i]);
            } else if (arg.startsWith("--port=")) {
                puerto = puertoAnalizar(arg.substring("--port=".length()));
            } else if (arg.equals("--url") && i + 1 < args.length) {
                url = args[++i];
            } else if (arg.startsWith("--url=")) {
                url = arg.substring("--url=".length());
            } else {
                resto.add(arg);
            }
        }
        if (servidor && cliente) {
            throw new IllegalArgumentException("--server and --client cannot be used together");
        }
        Modo modo = servidor ? Modo.SERVER_ONLY : cliente ? Modo.CLIENT_ONLY : Modo.BOTH;
        return new OpcionesLanzamiento(modo, puerto, url, List.copyOf(resto));
    }

    /** Verdadero si esta ejecución abre la ventana. */
    public boolean ventanaNecesita() {
        return modo != Modo.SERVER_ONLY;
    }

    /** Verdadero si esta ejecución arranca el servidor. */
    public boolean servidorNecesita() {
        return modo != Modo.CLIENT_ONLY;
    }

    /** Los argumentos de Spring Boot, con el puerto pedido agregado cuando hay uno. */
    public String[] argumentosServidor() {
        List<String> todo = new ArrayList<>(argsPrimavera);
        if (puerto != null) {
            todo.add("--server.port=" + puerto);
        }
        return todo.toArray(new String[0]);
    }

    private static int puertoAnalizar(String texto) {
        try {
            int value = Integer.parseInt(texto);
            if (value < 0 || value > 65535) {
                throw new IllegalArgumentException("port out of range: " + texto);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("port is not a number: " + texto, e);
        }
    }
}
