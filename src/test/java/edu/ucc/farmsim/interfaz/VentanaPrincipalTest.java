package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.InfoJuego;
import edu.ucc.farmsim.interfaz.pantallas.VentanaPrincipal;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.Test;

/** Prueba de humo de la ventana principal vacía (R-07: utilizable desde 1024 x 768). Se omite sin pantalla. */
class VentanaPrincipalTest {

    @Test
    void R07_windowHasTheGameTitleAndTheMinimumSize() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        VentanaPrincipal ventana = new VentanaPrincipal(InfoJuego.NOMBRE);
        try {
            assertEquals(InfoJuego.NOMBRE, ventana.getTitle());
            assertEquals(VentanaPrincipal.ANCHO_MINIMO, ventana.getMinimumSize().width);
            assertEquals(VentanaPrincipal.ALTO_MINIMO, ventana.getMinimumSize().height);
        } finally {
            ventana.dispose();
        }
    }
}
