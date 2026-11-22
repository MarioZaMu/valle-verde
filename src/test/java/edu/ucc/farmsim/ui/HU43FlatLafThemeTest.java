package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.ui.theme.Contrast;
import edu.ucc.farmsim.ui.theme.Palette;
import edu.ucc.farmsim.ui.theme.UiTheme;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import javax.swing.JButton;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

/** HU-43: the FlatLaf theme, its fallback and the contrast of the palette (RNF-14). */
class HU43FlatLafThemeTest {

    @Test
    void HU43_ca1_theThemeIsFlatLafWithOneFontOfAtLeast14Points() throws Exception {
        assertTrue(UiTheme.install());

        LookAndFeel look = UIManager.getLookAndFeel();
        assertTrue(look.getName().startsWith("FlatLaf"), look.getName());
        Font base = UIManager.getFont("defaultFont");
        assertTrue(base.getSize() >= UiTheme.FONT_SIZE && UiTheme.FONT_SIZE >= 14);
        String family = new JButton("x").getFont().getFamily();
        assertEquals(family, UIManager.getFont("Label.font").getFamily());
        assertEquals(family, UIManager.getFont("Table.font").getFamily());
    }

    @Test
    void HU43_ca2_ifTheThemeCannotBeInstalledTheDefaultLookRemainsAndNothingIsThrown() {
        LookAndFeel before = UIManager.getLookAndFeel();

        boolean installed = UiTheme.install(() -> {
            throw new IllegalStateException("no theme");
        });

        assertFalse(installed);
        assertEquals(before.getName(), UIManager.getLookAndFeel().getName());
    }

    @Test
    void HU43_ca3_installingTheThemeWithoutAScreenDoesNotFail() {
        assertTrue(UiTheme.install());
        assertTrue(UiTheme.install());
    }

    @Test
    void HU43_ca4_everyTextAndBackgroundPairHasAContrastOfAtLeast4Point5() {
        List<Color[]> pairs = List.of(
                new Color[] {Palette.TOAST_TEXT, Palette.TOAST_ERROR},
                new Color[] {Palette.TOAST_TEXT, Palette.TOAST_SUCCESS},
                new Color[] {Palette.TOAST_TEXT, Palette.TOAST_ACHIEVEMENT},
                new Color[] {Palette.TOAST_TEXT, Palette.TOAST_INFO},
                new Color[] {Palette.HUD_TEXT, Palette.HUD_BACKGROUND},
                new Color[] {Palette.DIALOG_TEXT, Palette.DIALOG_BACKGROUND});

        for (Color[] pair : pairs) {
            double ratio = Contrast.ratio(pair[0], pair[1]);
            assertTrue(ratio >= 4.5, pair[0] + " on " + pair[1] + ": " + ratio);
        }
    }

    @Test
    void HU43_theContrastFormulaMatchesTheKnownExtremes() {
        assertEquals(21.0, Contrast.ratio(Color.BLACK, Color.WHITE), 0.01);
        assertEquals(1.0, Contrast.ratio(Color.WHITE, Color.WHITE), 0.001);
    }
}
