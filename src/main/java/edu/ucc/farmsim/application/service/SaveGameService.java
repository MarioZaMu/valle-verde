package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.game.GameStateValidator;
import edu.ucc.farmsim.domain.save.SaveLoadException;
import edu.ucc.farmsim.domain.save.SaveRepository;
import edu.ucc.farmsim.domain.save.SaveStorageException;
import edu.ucc.farmsim.domain.save.SlotInfo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Pattern: Memento — Role: Caretaker (keeps the snapshots of the game; it never looks inside them).
 *
 * <p>Saves, loads and lists the save slots (RN-31 to RN-33). A load builds the new game on the side and only
 * replaces the game in progress when every check has passed, so a rejected slot changes nothing. When the
 * storage does not answer it reports {@code ERROR_ES}: playing never depends on it.
 *
 * <p>Stories: HU-27, HU-29, HU-30, HU-31, HU-32, HU-33.
 */
@Service
public final class SaveGameService {

    private static final Logger LOG = Logger.getLogger(SaveGameService.class.getName());
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SaveRepository repository;
    private final GameSession session;
    private final ViewMapper mapper;
    private final EventBus events;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param repository where games are stored
     * @param session    the game in progress
     * @param mapper     builds the views the UI draws
     * @param events     where events are published
     * @param clock      tells when a game is saved
     */
    public SaveGameService(SaveRepository repository, GameSession session, ViewMapper mapper, EventBus events,
                           Clock clock) {
        this.repository = repository;
        this.session = session;
        this.mapper = mapper;
        this.events = events;
        this.clock = clock;
    }

    /** Success with true if at least one slot holds a saved game; {@code ERROR_ES} if the storage fails. */
    public Result<Boolean> hasAnySave() {
        try {
            for (int slot = 1; slot <= GameRules.SAVE_SLOTS; slot++) {
                if (repository.slotExists(slot)) {
                    return Result.ok(true);
                }
            }
            return Result.ok(false);
        } catch (SaveStorageException e) {
            return Result.failWith(ErrorCode.ERROR_ES, e.getMessage());
        }
    }

    /** Every slot with what it holds. A damaged slot is listed as such and does not stop the rest. */
    public Result<List<SaveSlotView>> slots() {
        try {
            List<SaveSlotView> views = new ArrayList<>();
            for (SlotInfo info : repository.slots()) {
                views.add(view(info));
            }
            return Result.ok(List.copyOf(views));
        } catch (SaveStorageException e) {
            return Result.failWith(ErrorCode.ERROR_ES, e.getMessage());
        }
    }

    /**
     * Saves the game in progress in a slot, replacing what it held (RN-31). If the storage fails the previous
     * content stays and the game goes on.
     *
     * @param slot slot number, 1 to 3
     */
    public Result<SaveSlotView> save(int slot) {
        requireSlot(slot);
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        Instant now = clock.instant();
        try {
            repository.save(slot, state.createMemento(), now);
        } catch (SaveStorageException e) {
            return Result.failWith(ErrorCode.ERROR_ES, e.getMessage());
        }
        return Result.ok(view(SlotInfo.ok(slot, state.date(), state.money(), now)));
    }

    /**
     * Loads a slot and makes it the game in progress. The checks of RN-33 run in order and stop at the first
     * failure; the game in progress is replaced only when all of them pass.
     *
     * @param slot slot number, 1 to 3
     */
    public Result<HudView> load(int slot) {
        requireSlot(slot);
        GameMemento memento;
        try {
            memento = repository.load(slot);
        } catch (SaveStorageException e) {
            return Result.failWith(ErrorCode.ERROR_ES, e.getMessage());
        } catch (SaveLoadException e) {
            LOG.log(Level.WARNING, "Slot " + slot + " rejected: " + e.getMessage());
            return switch (e.reason()) {
                case MISSING -> Result.failWith(ErrorCode.ARCHIVO_INEXISTENTE, slot);
                case UNSUPPORTED_VERSION -> Result.failWith(ErrorCode.VERSION_NO_SOPORTADA);
                case CORRUPT -> Result.failWith(ErrorCode.ARCHIVO_CORRUPTO);
            };
        }
        List<String> violations = GameStateValidator.violations(memento);
        if (!violations.isEmpty()) {
            LOG.log(Level.WARNING, "Slot " + slot + " breaks the invariants: " + violations);
            return Result.failWith(ErrorCode.ARCHIVO_CORRUPTO);
        }
        GameMemento before = session.hasGame() ? session.current().createMemento() : null;
        GameState loaded;
        try {
            loaded = GameState.fromMemento(memento);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Slot " + slot + " could not be built", e);
            if (before != null) {
                session.current().restore(before);
            }
            return Result.failWith(ErrorCode.ARCHIVO_CORRUPTO);
        }
        session.replace(loaded);
        HudView hud = mapper.hud(loaded);
        events.publish(new GameEvent.GameStarted(hud));
        events.publish(new GameEvent.MapChanged());
        events.publish(new GameEvent.InventoryChanged());
        return Result.ok(hud);
    }

    private SaveSlotView view(SlotInfo info) {
        if (info.status() != SlotInfo.Status.OK) {
            return new SaveSlotView(info.slot(), info.status().name(), 0, null, 0, 0, null);
        }
        String stamp = STAMP.format(LocalDateTime.ofInstant(info.savedAt(), clock.getZone()));
        return new SaveSlotView(info.slot(), info.status().name(), info.date().year(), info.date().season().name(),
                info.date().day(), info.money(), stamp);
    }

    private static void requireSlot(int slot) {
        if (slot < 1 || slot > GameRules.SAVE_SLOTS) {
            throw new IllegalArgumentException("slot must be between 1 and " + GameRules.SAVE_SLOTS + ": " + slot);
        }
    }
}
