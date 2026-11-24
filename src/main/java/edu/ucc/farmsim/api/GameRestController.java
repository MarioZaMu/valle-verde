package edu.ucc.farmsim.api;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * El controlador REST: solo traduce HTTP en llamadas al juego y la respuesta de vuelta en HTTP. Las reglas viven más
 * abajo; esta clase no contiene ninguna (AR-11). No es el controlador GRASP, que es {@code GameController}.
 *
 * <p>Hay un juego por servidor (S-01), así que toda orden corre bajo el mismo candado (DT-20). Los eventos que causa
 * una orden viajan en la respuesta (DT-19). Las rutas están documentadas en {@code docs/API.md}.
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-26, HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-35, HU-37, HU-38, HU-45, HU-46, HU-47, HU-49.
 */
@RestController
@RequestMapping("/api")
public class GameRestController {

    private final GameCommands game;
    private final EventCollector collector;
    private final Object lock = new Object();

    /**
     * Crea el controlador.
     *
     * @param game      el juego, que se responde en este proceso
     * @param collector guarda los eventos de cada orden
     */
    public GameRestController(GameCommands game, EventCollector collector) {
        this.game = game;
        this.collector = collector;
    }

    /** Cuerpo de {@code POST /api/game}. */
    public record NewGameRequest(@NotNull Long seed) { }

    /** Cuerpo de {@code POST /api/farm/plots/{col}/{row}/crop}. */
    public record SowRequest(@NotBlank String seedId) { }

    /** Cuerpo de {@code POST /api/market/purchases} y {@code POST /api/market/sales}. */
    public record TradeRequest(@NotBlank String itemId, @NotNull Integer quantity) { }

    /** Cuerpo de {@code POST /api/buildings}. */
    public record BuildRequest(@NotBlank String buildingId, @NotNull Integer col, @NotNull Integer row) { }

    /** Cuerpo de {@code POST /api/animals}. */
    public record AnimalPurchaseRequest(@NotBlank String animalId) { }

    /** Cuerpo de {@code POST /api/avatar/moves}. */
    public record MoveRequest(@NotNull @Pattern(regexp = "NORTH|SOUTH|EAST|WEST") String direction) { }

    /** Todos los logros con el progreso del jugador. */
    @GetMapping("/achievements")
    public ResponseEntity<?> achievements() {
        return run(game::achievements, HttpStatus.OK);
    }

    /** Lo que guarda cada ranura de partida. */
    @GetMapping("/saves")
    public ResponseEntity<?> saveSlots() {
        return run(game::saveSlots, HttpStatus.OK);
    }

    /** Guarda la partida en curso en una ranura, reemplazando lo que tenía (RN-31). */
    @PutMapping("/saves/{slot}")
    public ResponseEntity<?> save(@PathVariable int slot) {
        return run(() -> game.save(slot), HttpStatus.OK);
    }

    /** Carga una ranura y la convierte en la partida en curso (RN-33). */
    @PostMapping("/saves/{slot}/load")
    public ResponseEntity<?> load(@PathVariable int slot) {
        return run(() -> game.load(slot), HttpStatus.OK);
    }

    /** Inicia una partida nueva (RN-30). */
    @PostMapping("/game")
    public ResponseEntity<?> newGame(@Valid @RequestBody NewGameRequest request) {
        return run(() -> game.newGame(request.seed()), HttpStatus.CREATED);
    }

    /** La barra superior de la partida en curso. */
    @GetMapping("/game")
    public ResponseEntity<?> hud() {
        return run(game::hud, HttpStatus.OK);
    }

    /** Termina el día (RN-03). */
    @PostMapping("/game/days")
    public ResponseEntity<?> endDay() {
        return run(game::endDay, HttpStatus.OK);
    }

    /** Todo el mapa de la granja. */
    @GetMapping("/farm/map")
    public ResponseEntity<?> map() {
        return run(game::map, HttpStatus.OK);
    }

    /** Un paso del avatar (RN-36). */
    @PostMapping("/avatar/moves")
    public ResponseEntity<?> move(@Valid @RequestBody MoveRequest request) {
        return run(() -> game.move(request.direction()), HttpStatus.OK);
    }

    /** Las personas del valle (RN-37). */
    @GetMapping("/npcs")
    public ResponseEntity<?> npcs() {
        return run(game::npcs, HttpStatus.OK);
    }

    /** Lo que dice una persona del valle, si el avatar está a su lado (RN-37). */
    @PostMapping("/npcs/{id}/talk")
    public ResponseEntity<?> talk(@PathVariable String id) {
        return run(() -> game.talk(id), HttpStatus.OK);
    }

    /** Entrega el encargo de una persona del valle (RN-38). */
    @PostMapping("/npcs/{id}/errand")
    public ResponseEntity<?> deliver(@PathVariable String id) {
        return run(() -> game.deliver(id), HttpStatus.OK);
    }

    /** El tutorial de inicio de la partida en curso (RN-39). */
    @GetMapping("/tutorial")
    public ResponseEntity<?> tutorial() {
        return run(game::tutorial, HttpStatus.OK);
    }

    /** Salta el tutorial de inicio (RN-39). */
    @PostMapping("/tutorial/skip")
    public ResponseEntity<?> skipTutorial() {
        return run(game::skipTutorial, HttpStatus.OK);
    }

    /** Describe una parcela sin cambiar nada. */
    @GetMapping("/farm/plots/{col}/{row}")
    public ResponseEntity<?> inspect(@PathVariable int col, @PathVariable int row) {
        return run(() -> game.inspect(col, row), HttpStatus.OK);
    }

    /** Siembra una semilla en una parcela (RN-06). */
    @PostMapping("/farm/plots/{col}/{row}/crop")
    public ResponseEntity<?> plant(@PathVariable int col, @PathVariable int row,
                                   @Valid @RequestBody SowRequest request) {
        return run(() -> game.plant(col, row, request.seedId()), HttpStatus.CREATED);
    }

    /** Riega el cultivo de una parcela (RN-07). */
    @PostMapping("/farm/plots/{col}/{row}/watering")
    public ResponseEntity<?> water(@PathVariable int col, @PathVariable int row) {
        return run(() -> game.water(col, row), HttpStatus.OK);
    }

    /** Cosecha un cultivo maduro o limpia uno marchito (RN-10, RN-34). */
    @PostMapping("/farm/plots/{col}/{row}/harvest")
    public ResponseEntity<?> harvest(@PathVariable int col, @PathVariable int row) {
        return run(() -> game.harvest(col, row), HttpStatus.OK);
    }

    /** Si al menos una ranura de guardado contiene una partida. */
    @GetMapping("/saves/any")
    public ResponseEntity<?> hasSavedGames() {
        return run(game::hasSavedGames, HttpStatus.OK);
    }

    /** Precios, tendencias y el catálogo de compra. */
    @GetMapping("/market")
    public ResponseEntity<?> market() {
        return run(game::market, HttpStatus.OK);
    }

    /** Compra unidades al precio fijo (RN-16). */
    @PostMapping("/market/purchases")
    public ResponseEntity<?> buy(@Valid @RequestBody TradeRequest request) {
        return run(() -> game.buy(request.itemId(), request.quantity()), HttpStatus.OK);
    }

    /** Vende unidades al precio dinámico (RN-17, RN-18). */
    @PostMapping("/market/sales")
    public ResponseEntity<?> sell(@Valid @RequestBody TradeRequest request) {
        return run(() -> game.sell(request.itemId(), request.quantity()), HttpStatus.OK);
    }

    /** Lo que pagaría ahora vender la cantidad, sin vender. */
    @GetMapping("/market/quotes")
    public ResponseEntity<?> quote(@RequestParam String itemId, @RequestParam int quantity) {
        return run(() -> game.quote(itemId, quantity), HttpStatus.OK);
    }

    /** El inventario como una grilla. */
    @GetMapping("/inventory")
    public ResponseEntity<?> inventory() {
        return run(game::inventory, HttpStatus.OK);
    }

    /** Los edificios construidos y el catálogo de lo que se puede construir. */
    @GetMapping("/buildings")
    public ResponseEntity<?> buildings() {
        return run(game::buildings, HttpStatus.OK);
    }

    /** Construye un edificio sobre pasto libre (RN-20, RN-21). */
    @PostMapping("/buildings")
    public ResponseEntity<?> build(@Valid @RequestBody BuildRequest request) {
        return run(() -> game.build(request.buildingId(), request.col(), request.row()), HttpStatus.CREATED);
    }

    /** Compra un animal y lo aloja en el primer edificio compatible con espacio (RN-23). */
    @PostMapping("/animals")
    public ResponseEntity<?> buyAnimal(@Valid @RequestBody AnimalPurchaseRequest request) {
        return run(() -> game.buyAnimal(request.animalId()), HttpStatus.CREATED);
    }

    /** Alimenta a los animales de un edificio (RN-24). */
    @PostMapping("/buildings/{id}/feeding")
    public ResponseEntity<?> feed(@PathVariable int id) {
        return run(() -> game.feed(id), HttpStatus.OK);
    }

    /** Recoge los productos que esperan en un edificio (RN-26). */
    @PostMapping("/buildings/{id}/collection")
    public ResponseEntity<?> collect(@PathVariable int id) {
        return run(() -> game.collect(id), HttpStatus.OK);
    }

    /** Cura a un animal enfermo de un edificio (RN-27). */
    @PostMapping("/buildings/{id}/animals/{index}/treatment")
    public ResponseEntity<?> cure(@PathVariable int id, @PathVariable int index) {
        return run(() -> game.cure(id, index), HttpStatus.OK);
    }

    private <T> ResponseEntity<?> run(Supplier<Result<T>> order, HttpStatus success) {
        synchronized (lock) {
            collector.drain();
            Result<T> result = order.get();
            List<EventDto> events = collector.drain().stream().map(EventDto::of).toList();
            if (result.isOk()) {
                return ResponseEntity.status(success).body(new ApiResponse<>(result.value(), events));
            }
            return ResponseEntity.status(ErrorStatusMapper.statusOf(result.code()))
                    .body(new ApiError(result.code().name(), result.message()));
        }
    }
}
