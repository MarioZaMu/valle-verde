package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.save.SaveRepository;

/**
 * Knows about the save slots. For now it only tells whether any slot holds a game; saving, loading and
 * listing arrive with HU-29, HU-30 and HU-32.
 *
 * <p>Stories: HU-33.
 */
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

    /** True if at least one slot holds a saved game. */
    public boolean hasAnySave() {
        for (int slot = 1; slot <= GameRules.SAVE_SLOTS; slot++) {
            if (repository.slotExists(slot)) {
                return true;
            }
        }
        return false;
    }
}
