package edu.ucc.farmsim.interfaz.recursos;

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
 * <p>Los nombres de sprite son rutas relativas bajo {@code /recursos/}, por ejemplo {@code cultivos/CARROT_MATURE.png} o
 * {@code animales/HEN_0.png}. Todo paquete de imágenes debe estar listado en {@code docs/RECURSOS.md} con su origen y
 * licencia (CC0 o CC-BY).
 *
 * <p>Stories: HU-42, HU-44, HU-48.
 */
public final class GestorRecurso {

    private static final Logger BITACORA = Logger.getLogger(GestorRecurso.class.getName());
    private static final String RAIZ = "/assets/";

    /** De dónde vienen los bytes de un sprite. */
    @FunctionalInterface
    public interface Fuente {
        /**
         * Abre un sprite.
         *
         * @param name ruta relativa del sprite, por ejemplo {@code cultivos/CARROT_MATURE.png}
         * @throws IOException si no existe ese sprite o no se puede abrir
         */
        InputStream abrir(String name) throws IOException;
    }

    private static GestorRecurso compartido;

    private final Fuente fuente;
    private final Map<String, Optional<BufferedImage>> cache = new HashMap<>();
    private final Map<String, Optional<ImageIcon>> iconos = new HashMap<>();
    private int lee;
    private boolean advertido;

    /**
     * Crea un administrador sobre una fuente.
     *
     * @param fuente de dónde se leen los sprites
     */
    public GestorRecurso(Fuente fuente) {
        this.fuente = fuente;
    }

    /** Un administrador que lee de la carpeta {@code recursos} del classpath. */
    public static GestorRecurso desdeClasspath() {
        return new GestorRecurso(name -> {
            InputStream stream = GestorRecurso.class.getResourceAsStream(RAIZ + name);
            if (stream == null) {
                throw new FileNotFoundException(RAIZ + name);
            }
            return stream;
        });
    }

    /**
     * El administrador que comparte toda la interfaz, de modo que cada sprite se lee una sola vez para todo el juego.
     */
    public static synchronized GestorRecurso compartido() {
        if (compartido == null) {
            compartido = desdeClasspath();
        }
        return compartido;
    }

    /** Un administrador sin ninguna imagen: todo se dibuja con vectores. */
    public static GestorRecurso soloVector() {
        return new GestorRecurso(name -> {
            throw new FileNotFoundException(name);
        });
    }

    /**
     * El sprite con el nombre dado, leído la primera vez y conservado después.
     *
     * @return la imagen, o vacío si falta o no se puede leer
     */
    public synchronized Optional<BufferedImage> sprite(String name) {
        Optional<BufferedImage> conocido = cache.get(name);
        if (conocido != null) {
            return conocido;
        }
        lee++;
        Optional<BufferedImage> cargado = cargar(name);
        cache.put(name, cargado);
        return cargado;
    }

    /**
     * El sprite como un ícono de Swing cuya descripción es su nombre, leído una sola vez; vacío si falta.
     *
     * @param name ruta relativa del sprite, por ejemplo {@code iconos/SOW.png}
     */
    public synchronized Optional<ImageIcon> icono(String name) {
        Optional<ImageIcon> conocido = iconos.get(name);
        if (conocido == null) {
            conocido = sprite(name).map(imagen -> new ImageIcon(imagen, name));
            iconos.put(name, conocido);
        }
        return conocido;
    }

    /** Cuántos archivos se pidieron a la fuente (un sprite pedido otra vez no cuenta). */
    public synchronized int lee() {
        return lee;
    }

    private Optional<BufferedImage> cargar(String name) {
        try (InputStream stream = fuente.abrir(name)) {
            BufferedImage imagen = ImageIO.read(stream);
            if (imagen == null) {
                falta(name, "it is not an image", null);
            }
            return Optional.ofNullable(imagen);
        } catch (IOException | RuntimeException e) {
            falta(name, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private void falta(String name, String porque, Exception causa) {
        if (advertido) {
            BITACORA.log(Level.FINE, "Sprite " + name + " is not available: " + porque, causa);
        } else {
            advertido = true;
            BITACORA.log(Level.WARNING, "Sprite " + name + " is not available (" + porque
                    + "); vector graphics are used instead. Further missing sprites are logged at FINE level.");
        }
    }
}
