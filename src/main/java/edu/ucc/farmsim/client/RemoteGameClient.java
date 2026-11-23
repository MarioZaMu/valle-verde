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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends each order of the player to the REST server and rebuilds the {@link Result} the screens expect, so
 * the UI cannot tell this class from the in-process {@code GameController} (DT-17). The events carried in
 * the response are republished on the local {@link EventBus}, after the result is rebuilt and in the order
 * the server produced them (DT-19).
 *
 * <p>Orders are sent synchronously from the calling thread with a time limit (DT-20). It never throws: an
 * unreachable server, a timeout or an unreadable answer becomes {@code ERROR_INTERNO}.
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-26, HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-35, HU-37, HU-38, HU-45, HU-46, HU-47, HU-49.
 */
public final class RemoteGameClient implements GameCommands {

    private static final Logger LOG = Logger.getLogger(RemoteGameClient.class.getName());
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final URI baseUri;
    private final EventBus bus;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final EventDecoder decoder = new EventDecoder(mapper);

    /**
     * Creates the client.
     *
     * @param baseUri address of the server, for example {@code http://127.0.0.1:8080}
     * @param bus     the local bus where the events of every response are republished
     */
    public RemoteGameClient(URI baseUri, EventBus bus) {
        this.baseUri = baseUri;
        this.bus = bus;
    }

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

    @Override
    public Result<Boolean> hasSavedGames() {
        return send("GET", "/api/saves/any", null, Boolean.class);
    }

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

    @Override
    public Result<List<AchievementView>> achievements() {
        return send("GET", "/api/achievements", null, new TypeReference<List<AchievementView>>() { });
    }

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

    private static String plot(int col, int row) {
        return "/api/farm/plots/" + col + "/" + row;
    }

    private <T> Result<T> send(String method, String path, Object body, Class<T> type) {
        return send(method, path, body, mapper.constructType(type));
    }

    private <T> Result<T> send(String method, String path, Object body, TypeReference<T> type) {
        return send(method, path, body, mapper.constructType(type));
    }

    private <T> Result<T> send(String method, String path, Object body, JavaType type) {
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8);
            HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path))
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
