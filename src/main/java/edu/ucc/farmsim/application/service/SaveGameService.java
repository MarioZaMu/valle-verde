package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.save.SaveRepository;
import edu.ucc.farmsim.domain.save.SaveStorageException;
import org.springframework.stereotype.Service;

/**
 * Knows about the save slots. For now it only tells whether any slot holds a game; saving, loading and
 * listing arrive with HU-29, HU-30 and HU-32. When the storage does not answer it reports {@code ERROR_ES}
 * instead of failing: playing never depends on it (RN-31).
 *
 * <p>Stories: HU-33.
 */
@Service
public final class SaveGameService {

    private final SaveRepository repository;

    /**
     * Creates the service.
     *
     * @param repository where games are stored
     */
    public SaveGameService(SaveRepository repository) {
        this.repository = repository;
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
}
