package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.apoyo.RepositorioGuardadoFalso;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.dominio.progreso.Logro;
import edu.ucc.farmsim.interfaz.componentes.DialogoLogros;
import edu.ucc.farmsim.interfaz.componentes.DialogoRanuras;
import edu.ucc.farmsim.interfaz.componentes.PresentadorLogros;
import edu.ucc.farmsim.interfaz.componentes.PresentadorRanuras;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;

/** Las pantallas de ranuras de guardado y de logros (HU-26, HU-29, HU-30, HU-32). */
class SlotsAndAchievementsUiTest {

    private static AplicacionTest aplicacion(MongoEnMemoria mongo) {
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        return aplicacion;
    }

    @Test
    void HU29_ca3_guardando_en_un_ocupado_ranura_pide_primero_y_cancelando_conserva_el_viejo_guardado() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = aplicacion(mongo);
        PresentadorRanuras presentador = new PresentadorRanuras(aplicacion.controlador);
        assertFalse(presentador.confirmacionNecesita(2));
        assertEquals("Partida guardada en la ranura 2.", presentador.save(2, () -> {
            throw new AssertionError("an empty slot must not ask");
        }));
        String first = mongo.documento(2).estado();
        aplicacion.controlador.diaFin();
        List<String> pedido = new ArrayList<>();

        String cancelado = presentador.save(2, () -> {
            pedido.add("asked");
            return false;
        });
        assertEquals("", cancelado);
        assertEquals(first, mongo.documento(2).estado());

        String aceptado = presentador.save(2, () -> {
            pedido.add("asked");
            return true;
        });

        assertEquals(List.of("asked", "asked"), pedido);
        assertEquals("Partida guardada en la ranura 2.", aceptado);
        assertTrue(!first.equals(mongo.documento(2).estado()));
    }

    @Test
    void HU30_cargando_desde_el_presentador_reemplaza_el_juego_y_rechaza_que_no_puede_ser_cargado() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = aplicacion(mongo);
        PresentadorRanuras presentador = new PresentadorRanuras(aplicacion.controlador);
        presentador.save(1, () -> true);
        aplicacion.controlador.diaFin();

        assertTrue(presentador.cargar(1).isOk());
        assertEquals(1, aplicacion.controlador.hud().value().dia());
        assertFalse(presentador.cargar(3).isOk());
        assertEquals("No existe una partida en la ranura 3.", presentador.cargar(3).mensaje());
        assertTrue(presentador.puedeCargar(1));
        assertFalse(presentador.puedeCargar(3));
    }

    @Test
    void HU32_cuando_el_almacenamiento_falla_el_presentador_conserva_el_mensaje() {
        RepositorioGuardadoFalso repositorio = new RepositorioGuardadoFalso().inalcanzable();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), repositorio);
        aplicacion.controlador.juegoNuevo(1L);

        PresentadorRanuras presentador = new PresentadorRanuras(aplicacion.controlador);

        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", presentador.error());
        assertTrue(presentador.lineas().isEmpty());
    }

    @Test
    void HU26_theAchievementsPresenterShowsTheStateWithoutRelyingOnColour() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.desbloquear(aplicacion.estado(), Logro.FIRST_HARVEST);
        PresentadorLogros presentador = new PresentadorLogros(aplicacion.controlador);

        assertEquals(9, presentador.logros().size());
        assertEquals("Desbloqueado", presentador.estado(presentador.logros().get(0)));
        assertEquals("1/25", presentador.estado(presentador.logros().get(1)));
        Object[] fila = presentador.fila(presentador.logros().get(4));
        assertEquals("Capitalista", fila[0]);
        assertEquals("Ten un saldo de 3.000 m o más.", fila[1]);
        assertEquals("200 m", fila[2]);
        assertEquals("500/3000", fila[3]);
        assertEquals("¡Logro desbloqueado: Primera cosecha! +50 m", Textos.logroDesbloqueado("Primera cosecha", 50));
    }

    @Test
    void HU26_el_dialogo_lista_el_nueve_logros() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        DialogoLogros dialogo = new DialogoLogros(null, new PresentadorLogros(aplicacion.controlador));

        assertTrue(dialogo.isModal());
        assertEquals(9, find(dialogo, JTable.class).getRowCount());
        dialogo.dispose();
    }

    @Test
    void HU29_el_dialogo_guardados_y_carga_con_su_botones_y_dice_cual_ranura_fue_usado() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = aplicacion(mongo);
        List<Integer> usado = new ArrayList<>();
        List<String> cargado = new ArrayList<>();
        DialogoRanuras dialogo = new DialogoRanuras(null, new PresentadorRanuras(aplicacion.controlador), usado::add,
                () -> cargado.add("loaded"));
        @SuppressWarnings("unchecked")
        JList<String> lista = find(dialogo, JList.class);
        JButton save = llamado(dialogo, "save-button");
        JButton cargar = llamado(dialogo, "load-button");
        assertFalse(save.isEnabled());
        assertEquals(3, lista.getModel().getSize());

        lista.setSelectedIndex(1);
        assertTrue(save.isEnabled());
        assertFalse(cargar.isEnabled());
        save.doClick();
        assertEquals(List.of(2), usado);
        assertTrue(cargar.isEnabled());
        aplicacion.controlador.diaFin();
        cargar.doClick();

        assertEquals(List.of(2, 2), usado);
        assertEquals(List.of("loaded"), cargado);
        assertEquals(1, aplicacion.controlador.hud().value().dia());
        assertFalse(dialogo.isDisplayable());
    }

    private static <T extends Component> T find(Container raiz, Class<T> tipo) {
        List<T> encontrado = new ArrayList<>();
        collect(raiz, tipo, encontrado);
        return encontrado.get(0);
    }

    private static JButton llamado(Container raiz, String name) {
        List<JButton> encontrado = new ArrayList<>();
        collect(raiz, JButton.class, encontrado);
        return encontrado.stream().filter(b -> name.equals(b.getName())).findFirst().orElseThrow();
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
