package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.aplicacion.dto.VistaOfertaEdificio;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.progreso.Logro;
import edu.ucc.farmsim.interfaz.componentes.DialogoConstruccion;
import edu.ucc.farmsim.interfaz.componentes.PresentadorConstruccion;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;

/** El diálogo de construcción abierto con la herramienta Construir (HU-18). */
class HU18DialogoConstruccionTest {

    private static VistaOfertaEdificio oferta(PresentadorConstruccion presentador, String tipo) {
        return presentador.catalogo().stream().filter(o -> o.tipo().equals(tipo)).findFirst().orElseThrow();
    }

    @Test
    void HU18_ca1_el_catalogo_muestra_tamanio_costo_y_estado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PresentadorConstruccion presentador = new PresentadorConstruccion(aplicacion.controlador, 2, 7);

        Object[] gallinero = presentador.fila(oferta(presentador, "COOP"));

        assertEquals("Esquina superior izquierda en (2, 7)", presentador.textoColocar());
        assertEquals("500 m", presentador.textoDinero());
        assertEquals("Gallinero", gallinero[0]);
        assertEquals("2 × 2", gallinero[1]);
        assertEquals("400 m", gallinero[2]);
        assertEquals("0/2 construidos", gallinero[3]);
        assertTrue(presentador.puedeConstruir(oferta(presentador, "COOP")));
    }

    @Test
    void HU18_ca5_aLockedBuildingSaysWhichAchievementItNeedsAndCannotBeBuilt() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PresentadorConstruccion presentador = new PresentadorConstruccion(aplicacion.controlador, 2, 7);

        VistaOfertaEdificio establo = oferta(presentador, "BARN");

        assertEquals("Bloqueado: requiere el logro «Manos a la tierra»", presentador.estado(establo));
        assertEquals("Edificio bloqueado: requiere el logro «Manos a la tierra».", presentador.bloquear(establo));
        assertFalse(presentador.puedeConstruir(establo));
    }

    @Test
    void HU18_ca3_sin_dinero_el_construir_boton_es_bloqueado_con_el_faltante_monto() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 399);
        PresentadorConstruccion presentador = new PresentadorConstruccion(aplicacion.controlador, 2, 7);

        assertEquals("Fondos insuficientes: faltan 1 m.", presentador.bloquear(oferta(presentador, "COOP")));
        assertFalse(presentador.puedeConstruir(oferta(presentador, "COOP")));
    }

    @Test
    void HU18_ca4_en_el_limite_el_edificio_es_bloqueado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.construir(aplicacion.estado(), TipoEdificio.COOP, 2, 7);
        DatosJuego.construir(aplicacion.estado(), TipoEdificio.COOP, 5, 7);
        PresentadorConstruccion presentador = new PresentadorConstruccion(aplicacion.controlador, 8, 7);

        assertEquals("Ya tienes el máximo de Gallineros.", presentador.bloquear(oferta(presentador, "COOP")));
    }

    @Test
    void HU18_ca1_edificio_desde_el_dialogo_usa_la_clicado_parcela_como_la_esquina() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PresentadorConstruccion presentador = new PresentadorConstruccion(aplicacion.controlador, 2, 7);

        String mensaje = presentador.construir(oferta(presentador, "COOP"));

        assertEquals("Construiste: Gallinero.", mensaje);
        assertEquals(0, presentador.ultimoConstruido().orElseThrow().id());
        assertEquals(200, aplicacion.estado().dinero()); // 500 - 400 + 100 of LG-06
        assertEquals("200 m", presentador.textoDinero());
    }

    @Test
    void HU18_ca2_anInvalidPlaceAnswersTheRn35MessageAndBuildsNothing() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PresentadorConstruccion presentador = new PresentadorConstruccion(aplicacion.controlador, 4, 2);

        String mensaje = presentador.construir(oferta(presentador, "COOP"));

        assertEquals("No se puede construir aquí: el área debe ser hierba libre dentro del mapa.", mensaje);
        assertTrue(presentador.ultimoConstruido().isEmpty());
        assertEquals(500, aplicacion.estado().dinero());
    }

    @Test
    void HU18_el_dialogo_construye_la_seleccionado_fila_y_cierra_y_un_desbloqueado_establo_habilita_el_boton() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 2000);
        DatosJuego.desbloquear(aplicacion.estado(), Logro.GREEN_THUMB);
        DialogoConstruccion dialogo = new DialogoConstruccion(null, new PresentadorConstruccion(aplicacion.controlador, 2, 7));
        JTable tabla = first(dialogo, JTable.class);
        JButton build = llamado(dialogo, "build-button");
        assertTrue(dialogo.isModal());
        assertEquals(4, tabla.getRowCount());
        assertFalse(build.isEnabled());

        tabla.setRowSelectionInterval(1, 1);
        assertTrue(build.isEnabled());
        tabla.setRowSelectionInterval(3, 3);
        assertFalse(build.isEnabled());
        tabla.setRowSelectionInterval(0, 0);
        build.doClick();

        assertEquals(1, aplicacion.estado().granja().edificios().size());
        assertFalse(dialogo.isDisplayable());
    }

    private static JButton llamado(Container raiz, String name) {
        List<JButton> encontrado = new ArrayList<>();
        collect(raiz, JButton.class, encontrado);
        return encontrado.stream().filter(b -> name.equals(b.getName())).findFirst().orElseThrow();
    }

    private static <T extends Component> T first(Container raiz, Class<T> tipo) {
        List<T> encontrado = new ArrayList<>();
        collect(raiz, tipo, encontrado);
        return encontrado.get(0);
    }

    private static <T extends Component> void collect(Container contenedor, Class<T> tipo, List<T> encontrado) {
        for (Component hijo : contenedor.getComponents()) {
            if (tipo.isInstance(hijo)) {
                encontrado.add(tipo.cast(hijo));
            }
            if (hijo instanceof Container interior) {
                collect(interior, tipo, encontrado);
            }
        }
    }
}
