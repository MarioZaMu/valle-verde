package edu.ucc.farmsim.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
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
