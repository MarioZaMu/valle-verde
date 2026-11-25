package edu.ucc.farmsim.interfaz.tema;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;

/**
 * Instala el aspecto de toda la ventana: FlatLaf con una sola familia de fuente de al menos {@link #TAMANIO_TIPOGRAFIA} puntos
 * (HU-43). Si el tema no se puede instalar, el juego conserva el aspecto predeterminado de Swing, registra una
 * advertencia y sigue; nunca lanza excepciones.
 *
 * <p>Stories: HU-43.
 */
public final class UiTheme {

    /** Tamaño en puntos de la fuente que se usa en todas partes. */
    public static final int TAMANIO_TIPOGRAFIA = 14;

    private static final Logger BITACORA = Logger.getLogger(UiTheme.class.getName());
    private static final int ARCO = 12;

    /** Pergamino cálido y madera para cada ventana y diálogo, la misma paleta que las cajas de madera del mapa. */
    private static final Map<String, String> TONOS_VALLE = Map.ofEntries(
            Map.entry("@background", "#F4E4BC"),
            Map.entry("@foreground", "#3B2412"),
            Map.entry("@accentColor", "#B5622A"),
            Map.entry("@selectionBackground", "#C9782F"),
            Map.entry("@selectionForeground", "#FFF8E7"),
            Map.entry("Button.background", "#E9C98E"),
            Map.entry("Button.hoverBackground", "#F2D7A2"),
            Map.entry("Button.pressedBackground", "#D4AE6E"),
            Map.entry("Button.borderColor", "#8A5628"),
            Map.entry("Button.default.background", "#B5622A"),
            Map.entry("Button.default.foreground", "#FFF8E7"),
            Map.entry("ToggleButton.background", "#FBEFD2"),
            Map.entry("ToggleButton.selectedBackground", "#F2C14E"),
            Map.entry("ToggleButton.selectedForeground", "#3B2412"),
            Map.entry("Component.borderColor", "#B98A50"),
            Map.entry("Component.focusColor", "#C9981C"),
            Map.entry("TextField.background", "#FFF8E7"),
            Map.entry("ComboBox.background", "#FFF8E7"),
            Map.entry("ComboBox.buttonBackground", "#E9C98E"),
            Map.entry("Table.background", "#FFF8E7"),
            Map.entry("Table.alternateRowColor", "#F6EACB"),
            Map.entry("TableHeader.background", "#E2C487"),
            Map.entry("List.background", "#FFF8E7"),
            Map.entry("ProgressBar.foreground", "#5FA83A"),
            Map.entry("ToolTip.background", "#FFF8E7"),
            Map.entry("ToolTip.foreground", "#3B2412"));

    /** Lo que realmente instala un tema; puede fallar con cualquier excepción. */
    @FunctionalInterface
    public interface Instalador {
        /** Instala el tema. */
        void setup();
    }

    private UiTheme() {
                // solo métodos de utilidad estáticos
    }

    /** Instala FlatLaf; devuelve {@code false} (y deja el aspecto sin cambios) si no pudo. */
    public static boolean instalar() {
        return instalar(() -> {
            UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, TAMANIO_TIPOGRAFIA));
            UIManager.put("Button.arc", ARCO);
            UIManager.put("Component.arc", ARCO);
            UIManager.put("TextComponent.arc", ARCO);
            FlatLaf.setGlobalExtraDefaults(TONOS_VALLE);
            if (!FlatLightLaf.setup()) {
                throw new IllegalStateException("FlatLaf did not start");
            }
        });
    }

    /**
     * Instala un tema con el instalador dado.
     *
     * @return {@code true} si se instaló; {@code false} si falló (se registra una advertencia)
     */
    public static boolean instalar(Instalador instalador) {
        try {
            instalador.setup();
            return true;
        } catch (RuntimeException | LinkageError e) {
            BITACORA.log(Level.WARNING, "The theme could not be installed; the default look is used", e);
            return false;
        }
    }
}
