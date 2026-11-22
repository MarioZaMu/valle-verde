package edu.ucc.farmsim.application.controller;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.DayService;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.application.service.ProgressService;
import edu.ucc.farmsim.application.service.SaveGameService;
import edu.ucc.farmsim.application.service.ViewMapper;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pattern: Controller — Role: GRASP controller (the single entry point the UI calls).
 *
 * <p>It only orchestrates: it asks the services to do the work and always answers with a {@link Result}.
 * No exception ever reaches the UI: anything unexpected becomes {@code ERROR_INTERNO} (R-03, RNF-07), and
 * it never touches repositories (AR-4).
 *
 * <p>Stories: HU-10, HU-28, HU-33.
 */
public final class GameController {

    private static final Logger LOG = Logger.getLogger(GameController.class.getName());

    private final GameSession session;
    private final ViewMapper mapper;
    private final ProgressService progress;
    private final DayService day;
    private final SaveGameService saves;

    /**
     * Creates the controller.
     *
     * @param session  the game in progress
     * @param mapper   builds the views the UI draws
     * @param progress starts games
     * @param day      ends days
     * @param saves    knows about the save slots
     */
    public GameController(GameSession session, ViewMapper mapper, ProgressService progress, DayService day,
                          SaveGameService saves) {
        this.session = session;
        this.mapper = mapper;
        this.progress = progress;
        this.day = day;
        this.saves = saves;
    }

    /** True if a game is in progress (the UI asks for confirmation before discarding it). */
    public boolean hasGameInProgress() {
        try {
            return session.hasGame();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "hasGameInProgress failed", e);
            return false;
        }
    }

    /**
     * Starts a new game with the initial state of RN-30, replacing any game in progress.
     *
     * @param seed seed of the random generator
     */
    public Result<HudView> newGame(long seed) {
        return guard(() -> progress.newGame(seed));
    }

    /** Ends the day: effects are resolved in the order of RN-03 and the new top bar is returned. */
    public Result<HudView> endDay() {
        return guard(day::endDay);
    }

    /** True if at least one of the save slots holds a game. */
    public Result<Boolean> hasSavedGames() {
        return guard(() -> Result.ok(saves.hasAnySave()));
    }

    /** The top bar of the game in progress. */
    public Result<HudView> hud() {
        return guard(() -> {
            if (!session.hasGame()) {
                return Result.failWith(ErrorCode.SIN_PARTIDA);
            }
            return Result.ok(mapper.hud(session.current()));
        });
    }

    private <T> Result<T> guard(Supplier<Result<T>> operation) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error", e);
            return Result.failWith(ErrorCode.ERROR_INTERNO);
        }
    }
}
