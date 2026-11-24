package edu.ucc.farmsim.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucc.farmsim.application.controller.GameCommands;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.ucc.farmsim.application.dto.AchievementView;
import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.InventoryView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.MarketView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.dto.TutorialView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Envía cada orden del jugador al servidor REST y reconstruye el {@link Result} que esperan las pantallas, de modo que
 * la interfaz no distingue esta clase del {@code GameController} interno (DT-17). Los eventos que trae la respuesta se
 * republican en el {@link EventBus} local, después de reconstruir el resultado y en el orden en que los produjo el
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
public final class RemoteGameClient implements GameCommands {

    // ===== SECCIÓN 1 · Ajustes y herramientas ===============================================================
    // Límites de tiempo (2 s para conectar, 5 s por orden) y los ayudantes de esta clase: el cliente HTTP que
    // habla con el servidor, el mapper de Jackson que convierte objetos <-> JSON y el decodificador que vuelve a
    // convertir los "events" de una respuesta en objetos GameEvent. Aquí no hay reglas del juego: viven en el servidor.
    private static final Logger LOG = Logger.getLogger(RemoteGameClient.class.getName());
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final CompletableFuture<URI> server;
    private final Duration startWait;
    private final EventBus bus;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final EventDecoder decoder = new EventDecoder(mapper);

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
    public RemoteGameClient(URI baseUri, EventBus bus) {
        this(CompletableFuture.completedFuture(baseUri), bus, Duration.ZERO);
    }

    /**
     * Crea un cliente para un servidor que quizá todavía esté arrancando.
     *
     * @param server    se completa con la dirección del servidor cuando escucha, o falla si no puede arrancar
     * @param bus       el bus local donde se republican los eventos de cada respuesta
     * @param startWait cuánto espera una orden a que el servidor esté listo antes de rendirse
     */
    public RemoteGameClient(CompletableFuture<URI> server, EventBus bus, Duration startWait) {
        this.server = server;
        this.startWait = startWait;
        this.bus = bus;
    }

    /** La dirección del servidor, esperándolo mientras arranca. */
    private URI baseUri() throws InterruptedException, ExecutionException, TimeoutException {
        if (server.isDone()) {
            return server.get();
        }
        return server.get(startWait.toMillis(), TimeUnit.MILLISECONDS);
    }

    // ===== SECCIÓN 3 · Partida y día (HU-10, HU-28, HU-34) ==================================================
    // Iniciar una partida nueva, pedir el HUD (día, estación, clima, dinero, energía) y terminar el día. Cada
    // método es una línea: elige el verbo y la ruta de docs/API.md e indica en qué DTO se convierte la respuesta.
    // hasGameInProgress() solo pregunta «¿el servidor responde con un HUD?».

    @Override
    public boolean hasGameInProgress() {
        return hud().isOk();
    }

    @Override
    public Result<HudView> newGame(long seed) {
        return send("POST", "/api/game", Map.of("seed", seed), HudView.class);
    }

    @Override
    public Result<HudView> hud() {
        return send("GET", "/api/game", null, HudView.class);
    }

    @Override
    public Result<HudView> endDay() {
        return send("POST", "/api/game/days", null, HudView.class);
    }

    // ===== SECCIÓN 4 · Parcelas (HU-01, HU-02, HU-04, HU-07) ================================================
    // Sembrar, regar, cosechar e inspeccionar una parcela. La parcela va dentro de la ruta
    // (/api/farm/plots/{col}/{row}); ver plot() al final. El servidor responde con un PlotView, una copia de solo
    // lectura: la pantalla nunca toca los objetos del dominio (Crop, Plot).

    @Override
    public Result<PlotView> plant(int col, int row, String seedId) {
        return send("POST", plot(col, row) + "/crop", Map.of("seedId", seedId), PlotView.class);
    }

    @Override
    public Result<PlotView> water(int col, int row) {
        return send("POST", plot(col, row) + "/watering", null, PlotView.class);
    }

    @Override
    public Result<PlotView> harvest(int col, int row) {
        return send("POST", plot(col, row) + "/harvest", null, PlotView.class);
    }

    @Override
    public Result<PlotView> inspect(int col, int row) {
        return send("GET", plot(col, row), null, PlotView.class);
    }

    // ===== SECCIÓN 5 · Valle, avatar, vecinos y tutorial (HU-35, HU-45, HU-46, HU-47, HU-49) ================
    // El mapa completo (48 × 24 parcelas) llega como un solo MapView; caminar envía una dirección y recibe la
    // nueva posición; hablar y entregar encargos usan el id del vecino; el tutorial se lee y se puede saltar.
    // Las listas (npcs) usan un TypeReference para que Jackson sepa el tipo de los elementos de la List.

    @Override
    public Result<MapView> map() {
        return send("GET", "/api/farm/map", null, MapView.class);
    }

    @Override
    public Result<AvatarView> move(String direction) {
        return send("POST", "/api/avatar/moves", Map.of("direction", direction), AvatarView.class);
    }

    @Override
    public Result<List<NpcView>> npcs() {
        return send("GET", "/api/npcs", null, new TypeReference<List<NpcView>>() { });
    }

    @Override
    public Result<DialogueView> talk(String npcId) {
        return send("POST", "/api/npcs/" + npcId + "/talk", null, DialogueView.class);
    }

    @Override
    public Result<DialogueView> deliver(String npcId) {
        return send("POST", "/api/npcs/" + npcId + "/errand", null, DialogueView.class);
    }

    @Override
    public Result<TutorialView> tutorial() {
        return send("GET", "/api/tutorial", null, TutorialView.class);
    }

    @Override
    public Result<TutorialView> skipTutorial() {
        return send("POST", "/api/tutorial/skip", null, TutorialView.class);
    }

    // La usa la pantalla de inicio para decidir si ofrece «Cargar» (pertenece a la SECCIÓN 9 · Partidas guardadas).
    @Override
    public Result<Boolean> hasSavedGames() {
        return send("GET", "/api/saves/any", null, Boolean.class);
    }

    // ===== SECCIÓN 6 · Mercado e inventario (HU-13 a HU-16, HU-37, HU-38) ===================================
    // Comprar, vender y cotizar (previsión de precio sin vender) van a /api/market; la respuesta es el total en
    // dinero. El precio dinámico NO se calcula aquí: el servidor usa la Strategy PricingStrategy y solo nos dice
    // el resultado. market() e inventory() traen lo que dibujan las ventanas de Mercado e Inventario.

    @Override
    public Result<Integer> buy(String itemId, int quantity) {
        return send("POST", "/api/market/purchases", Map.of("itemId", itemId, "quantity", quantity), Integer.class);
    }

    @Override
    public Result<Integer> sell(String itemId, int quantity) {
        return send("POST", "/api/market/sales", Map.of("itemId", itemId, "quantity", quantity), Integer.class);
    }

    @Override
    public Result<Integer> quote(String itemId, int quantity) {
        return send("GET", "/api/market/quotes?itemId=" + itemId + "&quantity=" + quantity, null, Integer.class);
    }

    @Override
    public Result<MarketView> market() {
        return send("GET", "/api/market", null, MarketView.class);
    }

    @Override
    public Result<InventoryView> inventory() {
        return send("GET", "/api/inventory", null, InventoryView.class);
    }

    // ===== SECCIÓN 7 · Edificios y animales (HU-18 a HU-23) ==================================================
    // Listar el catálogo, construir (el servidor usa el patrón Builder), comprar un animal, alimentar, recolectar
    // huevos o leche y curar. El id del edificio va en la ruta. El estado del animal (sano, hambriento, enfermo)
    // es el patrón State en el servidor; aquí solo lo recibimos dentro de un BuildingView / BuildingActionView.

    @Override
    public Result<BuildingsView> buildings() {
        return send("GET", "/api/buildings", null, BuildingsView.class);
    }

    @Override
    public Result<BuildingView> build(String buildingId, int col, int row) {
        return send("POST", "/api/buildings", Map.of("buildingId", buildingId, "col", col, "row", row),
                BuildingView.class);
    }

    @Override
    public Result<BuildingView> buyAnimal(String animalId) {
        return send("POST", "/api/animals", Map.of("animalId", animalId), BuildingView.class);
    }

    @Override
    public Result<BuildingActionView> feed(int buildingId) {
        return send("POST", "/api/buildings/" + buildingId + "/feeding", null, BuildingActionView.class);
    }

    @Override
    public Result<BuildingActionView> collect(int buildingId) {
        return send("POST", "/api/buildings/" + buildingId + "/collection", null, BuildingActionView.class);
    }

    @Override
    public Result<BuildingActionView> cure(int buildingId, int animalIndex) {
        return send("POST", "/api/buildings/" + buildingId + "/animals/" + animalIndex + "/treatment", null,
                BuildingActionView.class);
    }

    // ===== SECCIÓN 8 · Logros (HU-26, HU-27) ================================================================
    // Lista de solo lectura con la condición, la recompensa y el progreso de cada logro; alimenta la ventana «Logros».

    @Override
    public Result<List<AchievementView>> achievements() {
        return send("GET", "/api/achievements", null, new TypeReference<List<AchievementView>>() { });
    }

    // ===== SECCIÓN 9 · Partidas guardadas (HU-29 a HU-32) ===================================================
    // Listar las 3 ranuras, guardar (PUT) y cargar (POST). Quien habla con MongoDB es el servidor, mediante el
    // Memento (GameMemento) y el puerto SaveRepository; el cliente nunca ve la base de datos. Una ranura dañada o
    // vacía vuelve como Result.fail con su código, para que la pantalla muestre el mensaje y conserve la partida.

    @Override
    public Result<List<SaveSlotView>> saveSlots() {
        return send("GET", "/api/saves", null, new TypeReference<List<SaveSlotView>>() { });
    }

    @Override
    public Result<SaveSlotView> save(int slot) {
        return send("PUT", "/api/saves/" + slot, null, SaveSlotView.class);
    }

    @Override
    public Result<HudView> load(int slot) {
        return send("POST", "/api/saves/" + slot + "/load", null, HudView.class);
    }

    // ===== SECCIÓN 10 · Ayudantes privados (el motor de la clase) ===========================================
    // plot(): arma la ruta de una parcela. send(): las tres versiones terminan en la que hace la llamada HTTP.
    // rebuild(): convierte la respuesta JSON en un Result y republica los eventos. codeOf(): convierte el texto
    // del código de error del servidor en un ErrorCode. Léelos en ese orden.

    private static String plot(int col, int row) {
        return "/api/farm/plots/" + col + "/" + row;
    }

    private <T> Result<T> send(String method, String path, Object body, Class<T> type) {
        return send(method, path, body, mapper.constructType(type));
    }

    private <T> Result<T> send(String method, String path, Object body, TypeReference<T> type) {
        return send(method, path, body, mapper.constructType(type));
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
    private <T> Result<T> send(String method, String path, Object body, JavaType type) {
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8);
            HttpRequest.Builder request = HttpRequest.newBuilder(baseUri().resolve(path))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .method(method, publisher);
            if (body != null) {
                request.header("Content-Type", "application/json");
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return rebuild(response, type);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Interrupted while waiting for the server", e);
            return Result.failWith(ErrorCode.ERROR_INTERNO);
        } catch (ExecutionException | TimeoutException e) {
            LOG.log(Level.WARNING, "The server is not up for " + method + " " + path, e);
            return Result.failWith(ErrorCode.ERROR_INTERNO);
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "The server did not answer " + method + " " + path, e);
            return Result.failWith(ErrorCode.ERROR_INTERNO);
        }
    }

    private <T> Result<T> rebuild(HttpResponse<String> response, JavaType type) throws IOException {
        JsonNode root = mapper.readTree(response.body());
        if (root == null || root.isMissingNode() || !root.isObject()) {
            LOG.warning("The server answered " + response.statusCode() + " without a JSON body");
            return Result.failWith(ErrorCode.ERROR_INTERNO);
        }
        if (response.statusCode() / 100 == 2) {
            T value = mapper.readerFor(type).readValue(root.get("data"));
            for (GameEvent event : decoder.decode(root.get("events"))) {
                bus.publish(event);
            }
            return Result.ok(value);
        }
        ErrorCode code = codeOf(root.path("code").asText());
        if (code == null) {
            LOG.warning("The server rejected the request: " + root);
            return Result.failWith(ErrorCode.ERROR_INTERNO);
        }
        return Result.fail(code, root.path("message").asText());
    }

    private static ErrorCode codeOf(String name) {
        for (ErrorCode code : ErrorCode.values()) {
            if (code.name().equals(name)) {
                return code;
            }
        }
        return null;
    }
}
