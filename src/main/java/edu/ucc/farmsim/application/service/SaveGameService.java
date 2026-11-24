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
 * <p>Guarda, carga y lista las ranuras de guardado (RN-31 a RN-33). Una carga construye la partida nueva aparte y solo
 * reemplaza la partida en curso cuando pasaron todas las comprobaciones, de modo que una ranura rechazada no cambia
 * nada. Cuando el almacenamiento no responde informa {@code ERROR_ES}: jugar nunca depende de él.
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
     * Crea el servicio.
     *
     * @param repository donde se guardan las partidas
     * @param session    la partida en curso
     * @param mapper     construye las vistas que dibuja la UI
     * @param events     donde se publican los eventos
     * @param clock      dice cuándo se guarda una partida
     */
    public SaveGameService(SaveRepository repository, GameSession session, ViewMapper mapper, EventBus events,
                           Clock clock) {
        this.repository = repository;
        this.session = session;
        this.mapper = mapper;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Éxito con true si al menos una ranura contiene una partida guardada; {@code ERROR_ES} si falla el almacenamiento.
     */
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

    /** Cada ranura con lo que contiene. Una ranura dañada se lista como tal y no detiene al resto. */
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
     * Guarda la partida en curso en una ranura, reemplazando lo que tenía (RN-31). Si falla el almacenamiento queda el
     * contenido anterior y el juego continúa.
     *
     * @param slot número de ranura, de 1 a 3
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
     * Carga una ranura y la convierte en la partida en curso. Las comprobaciones de RN-33 corren en orden y se detienen
     * en el primer fallo; la partida en curso se reemplaza solo cuando todas pasan.
     *
     * @param slot número de ranura, de 1 a 3
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
