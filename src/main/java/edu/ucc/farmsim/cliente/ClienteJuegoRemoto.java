package edu.ucc.farmsim.cliente;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaAccionEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificios;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaInventario;
import edu.ucc.farmsim.aplicacion.dto.VistaLogro;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaMercado;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado;
import edu.ucc.farmsim.aplicacion.dto.VistaTutorial;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Envía cada orden del jugador al servidor REST y reconstruye el {@link Resultado} que esperan las pantallas, de modo que
 * la interfaz no distingue esta clase del {@code ControladorJuego} interno (DT-17). Los eventos que trae la respuesta se
 * republican en el {@link BusEvento} local, después de reconstruir el resultado y en el orden en que los produjo el
 * servidor (DT-19).
 *
 * <p>Las órdenes se envían de forma síncrona desde el hilo que llama, con un límite de tiempo (DT-20). Nunca lanza
 * excepciones: un servidor inalcanzable, un tiempo agotado o una respuesta ilegible se convierten en {@code
 * ERROR_INTERNO}.
 *
 * <p>La dirección del servidor puede estar todavía en camino: la ventana abre mientras el servidor arranca, y una orden
 * enviada antes de que esté listo lo espera, hasta un límite (RNF-02). Un servidor que no logra arrancar también es
 * {@code ERROR_INTERNO}.
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-26, HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-35, HU-37, HU-38, HU-45, HU-46, HU-47, HU-49.
 */
public final class ClienteJuegoRemoto implements ComandosJuego {

    // ===== SECCIÓN 1 · Ajustes y herramientas ===============================================================
    // Límites de tiempo (2 s para conectar, 5 s por orden) y los ayudantes de esta clase: el cliente HTTP que
    // habla con el servidor, el mapper de Jackson que convierte objetos <-> JSON y el decodificador que vuelve a
    // convertir los "events" de una respuesta en objetos GameEvent. Aquí no hay reglas del juego: viven en el servidor.
    private static final Logger BITACORA = Logger.getLogger(ClienteJuegoRemoto.class.getName());
    private static final Duration ESPERA_CONECTAR = Duration.ofSeconds(2);
    private static final Duration ESPERA_PETICION = Duration.ofSeconds(5);

    private final CompletableFuture<URI> servidor;
    private final Duration esperarInicio;
    private final BusEvento bus;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA_CONECTAR).build();
    private final ObjectMapper mapeador = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final DecodificadorEvento decodificador = new DecodificadorEvento(mapeador);

    // ===== SECCIÓN 2 · Constructores ========================================================================
    // Dos formas de crear el cliente: con una dirección conocida (pruebas, "--client --url=...") o con la promesa
    // de una dirección (el arranque normal, donde la ventana abre mientras el servidor aún arranca, RNF-02).
    // En ambos casos, el EventBus recibido es donde se republican los eventos de cada respuesta.

    /**
     * Crea el cliente.
     *
     * @param baseUri dirección del servidor, por ejemplo {@code http://127.0.0.1:8080}
     * @param bus     el bus local donde se republican los eventos de cada respuesta
     */
    public ClienteJuegoRemoto(URI baseUri, BusEvento bus) {
        this(CompletableFuture.completedFuture(baseUri), bus, Duration.ZERO);
    }

    /**
     * Crea un cliente para un servidor que quizá todavía esté arrancando.
     *
     * @param servidor    se completa con la dirección del servidor cuando escucha, o falla si no puede arrancar
     * @param bus       el bus local donde se republican los eventos de cada respuesta
     * @param esperarInicio cuánto espera una orden a que el servidor esté listo antes de rendirse
     */
    public ClienteJuegoRemoto(CompletableFuture<URI> servidor, BusEvento bus, Duration esperarInicio) {
        this.servidor = servidor;
        this.esperarInicio = esperarInicio;
        this.bus = bus;
    }

    /** La dirección del servidor, esperándolo mientras arranca. */
    private URI baseUri() throws InterruptedException, ExecutionException, TimeoutException {
        if (servidor.isDone()) {
            return servidor.get();
        }
        return servidor.get(esperarInicio.toMillis(), TimeUnit.MILLISECONDS);
    }

    // ===== SECCIÓN 3 · Partida y día (HU-10, HU-28, HU-34) ==================================================
    // Iniciar una partida nueva, pedir el HUD (día, estación, clima, dinero, energía) y terminar el día. Cada
    // método es una línea: elige el verbo y la ruta de docs/API.md e indica en qué DTO se convierte la respuesta.
    // hasGameInProgress() solo pregunta «¿el servidor responde con un HUD?».

    @Override
    public boolean tieneJuegoEnProgreso() {
        return hud().isOk();
    }

    @Override
    public Resultado<VistaHud> juegoNuevo(long semilla) {
        return send("POST", "/api/game", Map.of("semilla", semilla), VistaHud.class);
    }

    @Override
    public Resultado<VistaHud> hud() {
        return send("GET", "/api/game", null, VistaHud.class);
    }

    @Override
    public Resultado<VistaHud> diaFin() {
        return send("POST", "/api/game/days", null, VistaHud.class);
    }

    // ===== SECCIÓN 4 · Parcelas (HU-01, HU-02, HU-04, HU-07) ================================================
    // Sembrar, regar, cosechar e inspeccionar una parcela. La parcela va dentro de la ruta
    // (/api/farm/plots/{col}/{row}); ver plot() al final. El servidor responde con un PlotView, una copia de solo
    // lectura: la pantalla nunca toca los objetos del dominio (Crop, Plot).

    @Override
    public Resultado<VistaParcela> plantar(int col, int fila, String idSemilla) {
        return send("POST", parcela(col, fila) + "/crop", Map.of("idSemilla", idSemilla), VistaParcela.class);
    }

    @Override
    public Resultado<VistaParcela> agua(int col, int fila) {
        return send("POST", parcela(col, fila) + "/watering", null, VistaParcela.class);
    }

    @Override
    public Resultado<VistaParcela> cosecha(int col, int fila) {
        return send("POST", parcela(col, fila) + "/harvest", null, VistaParcela.class);
    }

    @Override
    public Resultado<VistaParcela> inspeccionar(int col, int fila) {
        return send("GET", parcela(col, fila), null, VistaParcela.class);
    }

    // ===== SECCIÓN 5 · Valle, avatar, vecinos y tutorial (HU-35, HU-45, HU-46, HU-47, HU-49) ================
    // El mapa completo (48 × 24 parcelas) llega como un solo MapView; caminar envía una dirección y recibe la
    // nueva posición; hablar y entregar encargos usan el id del vecino; el tutorial se lee y se puede saltar.
    // Las listas (npcs) usan un TypeReference para que Jackson sepa el tipo de los elementos de la List.

    @Override
    public Resultado<VistaMapa> mapa() {
        return send("GET", "/api/farm/map", null, VistaMapa.class);
    }

    @Override
    public Resultado<VistaAvatar> mover(String direccion) {
        return send("POST", "/api/avatar/moves", Map.of("direccion", direccion), VistaAvatar.class);
    }

    @Override
    public Resultado<List<VistaNpc>> npcs() {
        return send("GET", "/api/npcs", null, new TypeReference<List<VistaNpc>>() { });
    }

    @Override
    public Resultado<VistaConversacion> hablar(String npcId) {
        return send("POST", "/api/npcs/" + npcId + "/talk", null, VistaConversacion.class);
    }

    @Override
    public Resultado<VistaConversacion> entregar(String npcId) {
        return send("POST", "/api/npcs/" + npcId + "/errand", null, VistaConversacion.class);
    }

    @Override
    public Resultado<VistaTutorial> tutorial() {
        return send("GET", "/api/tutorial", null, VistaTutorial.class);
    }

    @Override
    public Resultado<VistaTutorial> tutorialSaltar() {
        return send("POST", "/api/tutorial/skip", null, VistaTutorial.class);
    }

    // La usa la pantalla de inicio para decidir si ofrece «Cargar» (pertenece a la SECCIÓN 9 · Partidas guardadas).
    @Override
    public Resultado<Boolean> tieneJuegosArchivados() {
        return send("GET", "/api/saves/any", null, Boolean.class);
    }

    // ===== SECCIÓN 6 · Mercado e inventario (HU-13 a HU-16, HU-37, HU-38) ===================================
    // Comprar, vender y cotizar (previsión de precio sin vender) van a /api/market; la respuesta es el total en
    // dinero. El precio dinámico NO se calcula aquí: el servidor usa la Strategy PricingStrategy y solo nos dice
    // el resultado. market() e inventory() traen lo que dibujan las ventanas de Mercado e Inventario.

    @Override
    public Resultado<Integer> comprar(String idArticulo, int cantidad) {
        return send("POST", "/api/market/purchases", Map.of("idArticulo", idArticulo, "cantidad", cantidad), Integer.class);
    }

    @Override
    public Resultado<Integer> vender(String idArticulo, int cantidad) {
        return send("POST", "/api/market/sales", Map.of("idArticulo", idArticulo, "cantidad", cantidad), Integer.class);
    }

    @Override
    public Resultado<Integer> cotizar(String idArticulo, int cantidad) {
        return send("GET", "/api/market/quotes?idArticulo=" + idArticulo + "&cantidad=" + cantidad, null, Integer.class);
    }

    @Override
    public Resultado<VistaMercado> mercado() {
        return send("GET", "/api/market", null, VistaMercado.class);
    }

    @Override
    public Resultado<VistaInventario> inventario() {
        return send("GET", "/api/inventory", null, VistaInventario.class);
    }

    // ===== SECCIÓN 7 · Edificios y animales (HU-18 a HU-23) ==================================================
    // Listar el catálogo, construir (el servidor usa el patrón Builder), comprar un animal, alimentar, recolectar
    // huevos o leche y curar. El id del edificio va en la ruta. El estado del animal (sano, hambriento, enfermo)
    // es el patrón State en el servidor; aquí solo lo recibimos dentro de un BuildingView / BuildingActionView.

    @Override
    public Resultado<VistaEdificios> edificios() {
        return send("GET", "/api/buildings", null, VistaEdificios.class);
    }

    @Override
    public Resultado<VistaEdificio> construir(String idEdificio, int col, int fila) {
        return send("POST", "/api/buildings", Map.of("idEdificio", idEdificio, "col", col, "fila", fila),
                VistaEdificio.class);
    }

    @Override
    public Resultado<VistaEdificio> animalComprar(String animalId) {
        return send("POST", "/api/animals", Map.of("animalId", animalId), VistaEdificio.class);
    }

    @Override
    public Resultado<VistaAccionEdificio> alimentar(int idEdificio) {
        return send("POST", "/api/buildings/" + idEdificio + "/feeding", null, VistaAccionEdificio.class);
    }

    @Override
    public Resultado<VistaAccionEdificio> collect(int idEdificio) {
        return send("POST", "/api/buildings/" + idEdificio + "/collection", null, VistaAccionEdificio.class);
    }

    @Override
    public Resultado<VistaAccionEdificio> curar(int idEdificio, int indiceAnimal) {
        return send("POST", "/api/buildings/" + idEdificio + "/animals/" + indiceAnimal + "/treatment", null,
                VistaAccionEdificio.class);
    }

    // ===== SECCIÓN 8 · Logros (HU-26, HU-27) ================================================================
    // Lista de solo lectura con la condición, la recompensa y el progreso de cada logro; alimenta la ventana «Logros».

    @Override
    public Resultado<List<VistaLogro>> logros() {
        return send("GET", "/api/achievements", null, new TypeReference<List<VistaLogro>>() { });
    }

    // ===== SECCIÓN 9 · Partidas guardadas (HU-29 a HU-32) ===================================================
    // Listar las 3 ranuras, guardar (PUT) y cargar (POST). Quien habla con MongoDB es el servidor, mediante el
    // Memento (GameMemento) y el puerto SaveRepository; el cliente nunca ve la base de datos. Una ranura dañada o
    // vacía vuelve como Result.fail con su código, para que la pantalla muestre el mensaje y conserve la partida.

    @Override
    public Resultado<List<VistaRanuraGuardado>> guardarRanuras() {
        return send("GET", "/api/saves", null, new TypeReference<List<VistaRanuraGuardado>>() { });
    }

    @Override
    public Resultado<VistaRanuraGuardado> guardar(int ranura) {
        return send("PUT", "/api/saves/" + ranura, null, VistaRanuraGuardado.class);
    }

    @Override
    public Resultado<VistaHud> cargar(int ranura) {
        return send("POST", "/api/saves/" + ranura + "/load", null, VistaHud.class);
    }

    // ===== SECCIÓN 10 · Ayudantes privados (el motor de la clase) ===========================================
    // plot(): arma la ruta de una parcela. send(): las tres versiones terminan en la que hace la llamada HTTP.
    // rebuild(): convierte la respuesta JSON en un Result y republica los eventos. codeOf(): convierte el texto
    // del código de error del servidor en un ErrorCode. Léelos en ese orden.

    private static String parcela(int col, int fila) {
        return "/api/farm/plots/" + col + "/" + fila;
    }

    private <T> Resultado<T> send(String method, String path, Object body, Class<T> tipo) {
        return send(method, path, body, mapeador.constructType(tipo));
    }

    private <T> Resultado<T> send(String method, String path, Object body, TypeReference<T> tipo) {
        return send(method, path, body, mapeador.constructType(tipo));
    }

    // GUÍA DE LECTURA: todos los métodos públicos de arriba (plant, water, sell, save...) terminan aquí.
    // Para seguir una orden:
    //   1. La pantalla llama a un método de GameCommands, p. ej. plant(col, row, seedId), y no sabe que habla con
    //      un servidor: esta clase implementa la misma interfaz que el GameController interno (DT-17).
    //   2. send() convierte la orden en una petición HTTP (verbo + ruta + cuerpo JSON) para la API REST de
    //      Spring Boot; espera al servidor si aún arranca (baseUri) y se rinde tras REQUEST_TIMEOUT.
    //   3. rebuild() lee la respuesta JSON: si es 2xx reconstruye el valor desde "data", republica cada elemento
    //      de "events" en el EventBus local (esto hace que el HUD y el mapa se redibujen: patrón Observer) y
    //      devuelve Result.ok; si es un error devuelve Result.fail con el código y el mensaje del servidor.
    //   4. Nunca se lanza una excepción a la pantalla: cualquier fallo de red se vuelve ERROR_INTERNO (idea de Result).
    // El lado servidor de la misma orden es GameRestController -> GameController -> un servicio -> el dominio.
    private <T> Resultado<T> send(String method, String path, Object body, JavaType tipo) {
        try {
            HttpRequest.BodyPublisher publicador = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapeador.writeValueAsString(body), StandardCharsets.UTF_8);
            HttpRequest.Builder peticion = HttpRequest.newBuilder(baseUri().resolve(path))
                    .timeout(ESPERA_PETICION)
                    .header("Accept", "application/json")
                    .method(method, publicador);
            if (body != null) {
                peticion.header("Content-Type", "application/json");
            }
            HttpResponse<String> respuesta = http.send(peticion.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return reconstruir(respuesta, tipo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            BITACORA.log(Level.WARNING, "Interrupted while waiting for the server", e);
            return Resultado.fallarCon(CodigoError.ERROR_INTERNO);
        } catch (ExecutionException | TimeoutException e) {
            BITACORA.log(Level.WARNING, "The server is not up for " + method + " " + path, e);
            return Resultado.fallarCon(CodigoError.ERROR_INTERNO);
        } catch (IOException | RuntimeException e) {
            BITACORA.log(Level.WARNING, "The server did not answer " + method + " " + path, e);
            return Resultado.fallarCon(CodigoError.ERROR_INTERNO);
        }
    }

    private <T> Resultado<T> reconstruir(HttpResponse<String> respuesta, JavaType tipo) throws IOException {
        JsonNode raiz = mapeador.readTree(respuesta.body());
        if (raiz == null || raiz.isMissingNode() || !raiz.isObject()) {
            BITACORA.warning("The server answered " + respuesta.statusCode() + " without a JSON body");
            return Resultado.fallarCon(CodigoError.ERROR_INTERNO);
        }
        if (respuesta.statusCode() / 100 == 2) {
            T value = mapeador.readerFor(tipo).readValue(raiz.get("datos"));
            for (EventoJuego evento : decodificador.decodificar(raiz.get("eventos"))) {
                bus.publish(evento);
            }
            return Resultado.ok(value);
        }
        CodigoError codigo = codigoDe(raiz.path("codigo").asText());
        if (codigo == null) {
            BITACORA.warning("The server rejected the request: " + raiz);
            return Resultado.fallarCon(CodigoError.ERROR_INTERNO);
        }
        return Resultado.fallar(codigo, raiz.path("mensaje").asText());
    }

    private static CodigoError codigoDe(String name) {
        for (CodigoError codigo : CodigoError.values()) {
            if (codigo.name().equals(name)) {
                return codigo;
            }
        }
        return null;
    }
}
