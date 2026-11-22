package edu.ucc.farmsim.infrastructure.persistence;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.save.SaveRepository;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores each game in a JSON file, one per slot ({@code slot1.json} to {@code slot3.json}) inside a folder
 * (RN-31). For now it only reports which slots have a file; writing and reading come with HU-29 and HU-30.
 *
 * <p>Stories: HU-33.
 */
public final class JsonSaveRepository implements SaveRepository {

    private final Path folder;

    /**
     * Creates the repository.
     *
     * @param folder folder that holds the slot files (it does not have to exist yet)
     */
    public JsonSaveRepository(Path folder) {
        this.folder = folder;
    }

    @Override
    public boolean slotExists(int slot) {
        if (slot < 1 || slot > GameRules.SAVE_SLOTS) {
            throw new IllegalArgumentException("slot must be between 1 and " + GameRules.SAVE_SLOTS + ": " + slot);
        }
        return Files.isRegularFile(folder.resolve("slot" + slot + ".json"));
    }
}
