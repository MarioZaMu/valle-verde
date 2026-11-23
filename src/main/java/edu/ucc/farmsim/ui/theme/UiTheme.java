package edu.ucc.farmsim.ui.theme;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;

/**
 * Installs the look and feel of the whole window: FlatLaf with a single font family of at least
 * {@link #FONT_SIZE} points (HU-43). If the theme cannot be installed the game keeps the default Swing look,
 * logs a warning and goes on; it never throws.
 *
 * <p>Stories: HU-43.
 */
public final class UiTheme {

    /** Size in points of the font used everywhere. */
    public static final int FONT_SIZE = 14;

    private static final Logger LOG = Logger.getLogger(UiTheme.class.getName());
    private static final int ARC = 12;

    /** Warm parchment and wood for every window and dialog, the same palette as the wooden boxes of the map. */
    private static final Map<String, String> VALLEY_COLOURS = Map.ofEntries(
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

    /** What actually installs a theme; it can fail with any exception. */
    @FunctionalInterface
    public interface Installer {
        /** Installs the theme. */
        void setup();
    }

    private UiTheme() {
        // static helpers only
    }

    /** Installs FlatLaf; returns {@code false} (and leaves the look unchanged) if it could not. */
    public static boolean install() {
        return install(() -> {
            UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE));
            UIManager.put("Button.arc", ARC);
            UIManager.put("Component.arc", ARC);
            UIManager.put("TextComponent.arc", ARC);
            FlatLaf.setGlobalExtraDefaults(VALLEY_COLOURS);
            if (!FlatLightLaf.setup()) {
                throw new IllegalStateException("FlatLaf did not start");
            }
        });
    }

    /**
     * Installs a theme with the given installer.
     *
     * @return {@code true} if it was installed; {@code false} if it failed (a warning is logged)
     */
    public static boolean install(Installer installer) {
        try {
            installer.setup();
            return true;
        } catch (RuntimeException | LinkageError e) {
            LOG.log(Level.WARNING, "The theme could not be installed; the default look is used", e);
            return false;
        }
    }
}
