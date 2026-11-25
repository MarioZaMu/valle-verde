package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.interfaz.tema.Contraste;
import edu.ucc.farmsim.interfaz.tema.Paleta;
import edu.ucc.farmsim.interfaz.tema.UiTheme;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import javax.swing.JButton;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

/** HU-43: el tema FlatLaf, su respaldo y el contraste de la paleta (RNF-14). */
class HU43TemaLafPlanoTest {

    @Test
    void HU43_ca1_theThemeIsFlatLafWithOneFontOfAtLeast14Points() throws Exception {
        assertTrue(UiTheme.instalar());

        LookAndFeel mirar = UIManager.getLookAndFeel();
        assertTrue(mirar.getName().startsWith("FlatLaf"), mirar.getName());
        Font base = UIManager.getFont("defaultFont");
        assertTrue(base.getSize() >= UiTheme.TAMANIO_TIPOGRAFIA && UiTheme.TAMANIO_TIPOGRAFIA >= 14);
        String familia = new JButton("x").getFont().getFamily();
        assertEquals(familia, UIManager.getFont("Label.font").getFamily());
        assertEquals(familia, UIManager.getFont("Table.font").getFamily());
    }

    @Test
    void HU43_ca2_si_el_tema_no_puede_ser_instalado_el_predeterminado_mirar_permanece_y_nada_es_lanzado() {
        LookAndFeel antes = UIManager.getLookAndFeel();

        boolean instalado = UiTheme.instalar(() -> {
            throw new IllegalStateException("no theme");
        });

        assertFalse(instalado);
        assertEquals(antes.getName(), UIManager.getLookAndFeel().getName());
    }

    @Test
    void HU43_ca3_installingTheThemeWithoutAScreenDoesNotFail() {
        assertTrue(UiTheme.instalar());
        assertTrue(UiTheme.instalar());
    }

    @Test
    void HU43_ca4_everyTextAndBackgroundPairHasAContrastOfAtLeast4Point5() {
        List<Color[]> pares = List.of(
                new Color[] {Paleta.TEXTO_AVISO, Paleta.ERROR_AVISO},
                new Color[] {Paleta.TEXTO_AVISO, Paleta.EXITO_AVISO},
                new Color[] {Paleta.TEXTO_AVISO, Paleta.LOGRO_AVISO},
                new Color[] {Paleta.TEXTO_AVISO, Paleta.INFO_AVISO},
                new Color[] {Paleta.TEXTO_HUD, Paleta.FONDO_HUD},
                new Color[] {Paleta.TEXTO_DIALOGO, Paleta.FONDO_DIALOGO});

        for (Color[] par : pares) {
            double razon = Contraste.razon(par[0], par[1]);
            assertTrue(razon >= 4.5, par[0] + " on " + par[1] + ": " + razon);
        }
    }

    @Test
    void HU43_el_contraste_formula_coincide_los_conocido_extremos() {
        assertEquals(21.0, Contraste.razon(Color.BLACK, Color.WHITE), 0.01);
        assertEquals(1.0, Contraste.razon(Color.WHITE, Color.WHITE), 0.001);
    }
}
