package edu.ucc.farmsim.api;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST controller: it only translates HTTP into calls to the game and the answer back into HTTP. The
 * rules live below it; this class does not contain any (AR-11). It is not the GRASP controller, which is
 * {@code GameController}.
 *
 * <p>There is one game per server (S-01), so every order runs under the same lock (DT-20). The events an
 * order causes travel in the response (DT-19). The routes are documented in {@code docs/API.md}.
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-28, HU-33, HU-35, HU-37, HU-38.
 */
@RestController
@RequestMapping("/api")
public class GameRestController {

    private final GameCommands game;
    private final EventCollector collector;
    private final Object lock = new Object();

    /**
     * Creates the controller.
     *
     * @param game      the game, answered in this process
     * @param collector keeps the events of each order
     */
    public GameRestController(GameCommands game, EventCollector collector) {
        this.game = game;
        this.collector = collector;
    }

    /** Body of {@code POST /api/game}. */
    public record NewGameRequest(@NotNull Long seed) { }

    /** Body of {@code POST /api/farm/plots/{col}/{row}/crop}. */
    public record SowRequest(@NotBlank String seedId) { }

    /** Body of {@code POST /api/market/purchases} and {@code POST /api/market/sales}. */
    public record TradeRequest(@NotBlank String itemId, @NotNull Integer quantity) { }

    /** Starts a new game (RN-30). */
    @PostMapping("/game")
    public ResponseEntity<?> newGame(@Valid @RequestBody NewGameRequest request) {
        return run(() -> game.newGame(request.seed()), HttpStatus.CREATED);
    }

    /** The top bar of the game in progress. */
    @GetMapping("/game")
    public ResponseEntity<?> hud() {
        return run(game::hud, HttpStatus.OK);
    }

    /** Ends the day (RN-03). */
    @PostMapping("/game/days")
    public ResponseEntity<?> endDay() {
        return run(game::endDay, HttpStatus.OK);
    }

    /** The whole farm map. */
    @GetMapping("/farm/map")
    public ResponseEntity<?> map() {
        return run(game::map, HttpStatus.OK);
    }

    /** Describes one plot without changing anything. */
    @GetMapping("/farm/plots/{col}/{row}")
    public ResponseEntity<?> inspect(@PathVariable int col, @PathVariable int row) {
        return run(() -> game.inspect(col, row), HttpStatus.OK);
    }

    /** Sows a seed in a plot (RN-06). */
    @PostMapping("/farm/plots/{col}/{row}/crop")
    public ResponseEntity<?> plant(@PathVariable int col, @PathVariable int row,
                                   @Valid @RequestBody SowRequest request) {
        return run(() -> game.plant(col, row, request.seedId()), HttpStatus.CREATED);
    }

    /** Waters the crop of a plot (RN-07). */
    @PostMapping("/farm/plots/{col}/{row}/watering")
    public ResponseEntity<?> water(@PathVariable int col, @PathVariable int row) {
        return run(() -> game.water(col, row), HttpStatus.OK);
    }

    /** Harvests a mature crop or clears a withered one (RN-10, RN-34). */
    @PostMapping("/farm/plots/{col}/{row}/harvest")
    public ResponseEntity<?> harvest(@PathVariable int col, @PathVariable int row) {
        return run(() -> game.harvest(col, row), HttpStatus.OK);
    }

    /** Whether at least one save slot holds a game. */
    @GetMapping("/saves/any")
    public ResponseEntity<?> hasSavedGames() {
        return run(game::hasSavedGames, HttpStatus.OK);
    }

    /** Prices, trends and the purchase catalogue. */
    @GetMapping("/market")
    public ResponseEntity<?> market() {
        return run(game::market, HttpStatus.OK);
    }

    /** Buys units at the fixed price (RN-16). */
    @PostMapping("/market/purchases")
    public ResponseEntity<?> buy(@Valid @RequestBody TradeRequest request) {
        return run(() -> game.buy(request.itemId(), request.quantity()), HttpStatus.OK);
    }

    /** Sells units at the dynamic price (RN-17, RN-18). */
    @PostMapping("/market/sales")
    public ResponseEntity<?> sell(@Valid @RequestBody TradeRequest request) {
        return run(() -> game.sell(request.itemId(), request.quantity()), HttpStatus.OK);
    }

    /** What selling the quantity would pay now, without selling. */
    @GetMapping("/market/quotes")
    public ResponseEntity<?> quote(@RequestParam String itemId, @RequestParam int quantity) {
        return run(() -> game.quote(itemId, quantity), HttpStatus.OK);
    }

    /** The inventory as a grid. */
    @GetMapping("/inventory")
    public ResponseEntity<?> inventory() {
        return run(game::inventory, HttpStatus.OK);
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
