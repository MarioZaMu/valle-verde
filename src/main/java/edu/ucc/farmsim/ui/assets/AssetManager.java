package edu.ucc.farmsim.ui.assets;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Loads the sprites of the game once and keeps them (HU-42 · CA-1). A sprite that does not exist or cannot be
 * read is not an error: the answer is empty and the renderer draws the vector graphic instead (CA-2), so the
 * game is fully playable with no image at all (CA-3). The first failure is logged as a warning; the same
 * file is never tried twice.
 *
 * <p>Sprite names are relative paths below {@code /assets/}, for example {@code crops/CARROT_MATURE.png} or
 * {@code animals/HEN_0.png}. Every package of images must be listed in {@code docs/ASSETS.md} with its source
 * and licence (CC0 or CC-BY).
 *
 * <p>Stories: HU-42.
 */
public final class AssetManager {

    private static final Logger LOG = Logger.getLogger(AssetManager.class.getName());
    private static final String ROOT = "/assets/";

    /** Where the bytes of a sprite come from. */
    @FunctionalInterface
    public interface Source {
        /**
         * Opens a sprite.
         *
         * @param name relative path of the sprite, for example {@code crops/CARROT_MATURE.png}
         * @throws IOException if there is no such sprite or it cannot be opened
         */
        InputStream open(String name) throws IOException;
    }

    private final Source source;
    private final Map<String, Optional<BufferedImage>> cache = new HashMap<>();
    private int reads;
    private boolean warned;

    /**
     * Creates a manager over a source.
     *
     * @param source where the sprites are read from
     */
    public AssetManager(Source source) {
        this.source = source;
    }

    /** A manager that reads from the {@code assets} folder of the class path. */
    public static AssetManager fromClasspath() {
        return new AssetManager(name -> {
            InputStream stream = AssetManager.class.getResourceAsStream(ROOT + name);
            if (stream == null) {
                throw new FileNotFoundException(ROOT + name);
            }
            return stream;
        });
    }

    /** A manager with no images at all: everything is drawn with vectors. */
    public static AssetManager vectorOnly() {
        return new AssetManager(name -> {
            throw new FileNotFoundException(name);
        });
    }

    /**
     * The sprite with the given name, read the first time and kept afterwards.
     *
     * @return the image, or empty if it is missing or unreadable
     */
    public synchronized Optional<BufferedImage> sprite(String name) {
        Optional<BufferedImage> known = cache.get(name);
        if (known != null) {
            return known;
        }
        reads++;
        Optional<BufferedImage> loaded = load(name);
        cache.put(name, loaded);
        return loaded;
    }

    /** How many files were asked of the source (a sprite requested again does not count). */
    public synchronized int reads() {
        return reads;
    }

    private Optional<BufferedImage> load(String name) {
        try (InputStream stream = source.open(name)) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                miss(name, "it is not an image", null);
            }
            return Optional.ofNullable(image);
        } catch (IOException | RuntimeException e) {
            miss(name, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private void miss(String name, String why, Exception cause) {
        if (warned) {
            LOG.log(Level.FINE, "Sprite " + name + " is not available: " + why, cause);
        } else {
            warned = true;
            LOG.log(Level.WARNING, "Sprite " + name + " is not available (" + why
                    + "); vector graphics are used instead. Further missing sprites are logged at FINE level.");
        }
    }
}
