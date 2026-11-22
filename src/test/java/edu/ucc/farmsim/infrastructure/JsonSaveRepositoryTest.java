package edu.ucc.farmsim.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.infrastructure.persistence.JsonSaveRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** HU-33 · CA-3 support: the repository knows which of the 3 slots have a file. */
class JsonSaveRepositoryTest {

    @Test
    void HU33_ca3_reportsNoSavedGamesWhenTheSlotFilesAreMissing(@TempDir Path folder) {
        JsonSaveRepository repository = new JsonSaveRepository(folder);

        for (int slot = 1; slot <= 3; slot++) {
            assertFalse(repository.slotExists(slot), "slot " + slot);
        }
    }

    @Test
    void HU33_ca3_reportsASlotWhoseFileExists(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("slot2.json"), "{}");
        JsonSaveRepository repository = new JsonSaveRepository(folder);

        assertTrue(repository.slotExists(2));
        assertFalse(repository.slotExists(1));
    }

    @Test
    void HU33_ca3_aMissingFolderMeansNoSavedGames(@TempDir Path folder) {
        assertFalse(new JsonSaveRepository(folder.resolve("does-not-exist")).slotExists(1));
    }

    @Test
    void HU33_slotsOutsideOneToThreeAreProgrammingErrors(@TempDir Path folder) {
        JsonSaveRepository repository = new JsonSaveRepository(folder);

        assertThrows(IllegalArgumentException.class, () -> repository.slotExists(0));
        assertThrows(IllegalArgumentException.class, () -> repository.slotExists(4));
    }
}
