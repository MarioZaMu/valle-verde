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
import javax.swing.ImageIcon;

/**
 * Carga una sola vez los sprites del juego y los conserva (HU-42 · CA-1). Un sprite que no existe o no se puede leer no
 * es un error: la respuesta es vacía y el renderizador dibuja el gráfico vectorial en su lugar (CA-2), de modo que el
 * juego se puede jugar por completo sin ninguna imagen (CA-3). El primer fallo se registra como advertencia; nunca se
 * intenta dos veces el mismo archivo.
 *
 * <p>Los nombres de sprite son rutas relativas bajo {@code /assets/}, por ejemplo {@code crops/CARROT_MATURE.png} o
 * {@code animals/HEN_0.png}. Todo paquete de imágenes debe estar listado en {@code docs/ASSETS.md} con su origen y
 * licencia (CC0 o CC-BY).
 *
 * <p>Stories: HU-42, HU-44, HU-48.
 */
public final class AssetManager {

    private static final Logger LOG = Logger.getLogger(AssetManager.class.getName());
    private static final String ROOT = "/assets/";

    /** De dónde vienen los bytes de un sprite. */
    @FunctionalInterface
    public interface Source {
        /**
         * Abre un sprite.
         *
         * @param name ruta relativa del sprite, por ejemplo {@code crops/CARROT_MATURE.png}
         * @throws IOException si no existe ese sprite o no se puede abrir
         */
        InputStream open(String name) throws IOException;
    }

    private static AssetManager shared;

    private final Source source;
    private final Map<String, Optional<BufferedImage>> cache = new HashMap<>();
    private final Map<String, Optional<ImageIcon>> icons = new HashMap<>();
    private int reads;
    private boolean warned;

    /**
     * Crea un administrador sobre una fuente.
     *
     * @param source de dónde se leen los sprites
     */
    public AssetManager(Source source) {
        this.source = source;
    }

    /** Un administrador que lee de la carpeta {@code assets} del classpath. */
    public static AssetManager fromClasspath() {
        return new AssetManager(name -> {
            InputStream stream = AssetManager.class.getResourceAsStream(ROOT + name);
            if (stream == null) {
                throw new FileNotFoundException(ROOT + name);
            }
            return stream;
        });
    }

    /**
     * El administrador que comparte toda la interfaz, de modo que cada sprite se lee una sola vez para todo el juego.
     */
    public static synchronized AssetManager shared() {
        if (shared == null) {
            shared = fromClasspath();
        }
        return shared;
    }

    /** Un administrador sin ninguna imagen: todo se dibuja con vectores. */
    public static AssetManager vectorOnly() {
        return new AssetManager(name -> {
            throw new FileNotFoundException(name);
        });
    }

    /**
     * El sprite con el nombre dado, leído la primera vez y conservado después.
     *
     * @return la imagen, o vacío si falta o no se puede leer
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

    /**
     * El sprite como un ícono de Swing cuya descripción es su nombre, leído una sola vez; vacío si falta.
     *
     * @param name ruta relativa del sprite, por ejemplo {@code icons/SOW.png}
     */
    public synchronized Optional<ImageIcon> icon(String name) {
        Optional<ImageIcon> known = icons.get(name);
        if (known == null) {
            known = sprite(name).map(image -> new ImageIcon(image, name));
            icons.put(name, known);
        }
        return known;
    }

    /** Cuántos archivos se pidieron a la fuente (un sprite pedido otra vez no cuenta). */
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
