package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.granja.Direccion;
import edu.ucc.farmsim.dominio.granja.InstantaneaAvatar;
import edu.ucc.farmsim.dominio.granja.MapaGranja;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.dominio.juego.ValidadorEstadoJuego;
import edu.ucc.farmsim.infraestructura.persistencia.Checksum;
import edu.ucc.farmsim.infraestructura.persistencia.CodecGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import org.junit.jupiter.api.Test;

/** HU-45: caminar con el avatar en la granja (RN-36). */
class HU45MovimientoAvatarTest {

    private static final String BLOQUEADO = "No puedes pasar por ahí.";

    private static void colocar(AplicacionTest aplicacion, int col, int fila, Direccion mirada) {
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(col, fila, mirada));
    }

    private static VistaAvatar avatar(AplicacionTest aplicacion) {
        return aplicacion.controlador.mapa().value().avatar();
    }

    @Test
    void HU45_ca1_aStepEastMovesOnePlotLooksEastAndCostsNoEnergyNorTime() {
                // Dada una partida nueva: el avatar en su parcela de pasto, mirando al sur, con la parcela del este libre
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        assertEquals(new VistaAvatar(ReglasJuego.COL_INICIO_AVATAR, ReglasJuego.FILA_INICIO_AVATAR, "SOUTH"), avatar(aplicacion));
        VistaHud antes = aplicacion.controlador.hud().value();

                // Cuando el jugador da un paso al este
        Resultado<VistaAvatar> resultado = aplicacion.controlador.mover("EAST");

                // Entonces está una parcela al este, mira al este, y ni la energía ni el día cambiaron
        VistaAvatar movido = new VistaAvatar(ReglasJuego.COL_INICIO_AVATAR + 1, ReglasJuego.FILA_INICIO_AVATAR, "EAST");
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(movido, resultado.value());
        assertEquals(movido, avatar(aplicacion));
        assertEquals(antes, aplicacion.controlador.hud().value());
        assertEquals(java.util.List.of(new EventoJuego.AvatarMovido(movido)), aplicacion.eventos);
    }

    @Test
    void HU45_ca1_soilWithACropCanBeWalkedOnAndTheWalkIsUndoneWithTheRestOfTheGame() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        assertTrue(aplicacion.controlador.plantar(ReglasJuego.COL_INICIO_AVATAR, ReglasJuego.FILA_INICIO_AVATAR - 1, "CARROT_SEED").isOk());

        assertTrue(aplicacion.controlador.mover("NORTH").isOk());

        assertEquals(new VistaAvatar(ReglasJuego.COL_INICIO_AVATAR, ReglasJuego.FILA_INICIO_AVATAR - 1, "NORTH"), avatar(aplicacion));
        MementoJuego archivado = aplicacion.estado().mementoCrear();
        aplicacion.controlador.mover("NORTH");
        aplicacion.estado().restaurar(archivado);
        assertEquals(new VistaAvatar(ReglasJuego.COL_INICIO_AVATAR, ReglasJuego.FILA_INICIO_AVATAR - 1, "NORTH"), avatar(aplicacion));
    }

    @Test
    void HU45_ca2_waterTheEdgeAnObstacleAndABuildingBlockTheWayButTheAvatarTurns() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.gallinero(aplicacion.estado());
        int energia = aplicacion.estado().energia().actual();
        Object[][] casos = {
                        // dónde está, hacia dónde mira, hacia dónde intenta ir
            {12, 0, Direccion.SOUTH, "EAST"},  // el estanque
            {12, 0, Direccion.EAST, "NORTH"},  // el borde superior del mapa
            {7, 8, Direccion.NORTH, "SOUTH"},  // un árbol en (7, 9)
            {4, 7, Direccion.SOUTH, "WEST"},   // el gallinero cubre (2..3, 7..8)
        };
        for (Object[] c : casos) {
            colocar(aplicacion, (int) c[0], (int) c[1], (Direccion) c[2]);
            aplicacion.eventos.clear();

            Resultado<VistaAvatar> resultado = aplicacion.controlador.mover((String) c[3]);

            assertFalse(resultado.isOk(), "towards " + c[3] + " from (" + c[0] + ", " + c[1] + ")");
            assertEquals(CodigoError.MOVIMIENTO_BLOQUEADO, resultado.codigo());
            assertEquals(BLOQUEADO, resultado.mensaje());
            assertEquals(new VistaAvatar((int) c[0], (int) c[1], (String) c[3]), avatar(aplicacion), "it only turned");
            assertTrue(aplicacion.eventos.isEmpty(), "a rejection publishes nothing");
            assertEquals(energia, aplicacion.estado().energia().actual());
        }
    }

    @Test
    void HU45_ca2_aBuildingIsNeverPlacedOnTheAvatarAndWithoutAGameThereIsNoStep() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        colocar(aplicacion, DatosJuego.COL_HIERBA + 1, DatosJuego.FILA_HIERBA + 1, Direccion.SOUTH);

        Resultado<?> build = aplicacion.controlador.construir("COOP", DatosJuego.COL_HIERBA, DatosJuego.FILA_HIERBA);

        assertEquals(CodigoError.UBICACION_INVALIDA, build.codigo());
        assertEquals(CodigoError.SIN_PARTIDA, AplicacionTest.create().controlador.mover("EAST").codigo());
        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.mover("UP").codigo(), "not a direction");
    }

    @Test
    void HU45_ca4_posicion_y_direccion_sobrevivir_guardando_y_cargando() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        colocar(aplicacion, 12, 3, Direccion.WEST);
        assertTrue(aplicacion.controlador.guardar(1).isOk());
        assertEquals(CodecGuardado.VERSION_ESQUEMA, mongo.documento(1).versionEsquema());

        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED + 1);
        assertEquals(new VistaAvatar(ReglasJuego.COL_INICIO_AVATAR, ReglasJuego.FILA_INICIO_AVATAR, "SOUTH"), avatar(aplicacion));
        Resultado<VistaHud> cargado = aplicacion.controlador.cargar(1);

        assertTrue(cargado.isOk(), String.valueOf(cargado.mensaje()));
        assertEquals(new VistaAvatar(12, 3, "WEST"), avatar(aplicacion));
    }

    @Test
    void HU45_ca4_aSaveOfVersionOneWithoutAvatarLoadsWithTheAvatarAtItsStart() throws Exception {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        colocar(aplicacion, 12, 3, Direccion.WEST);
        aplicacion.controlador.guardar(2);
        mongo.manipular(2, HU45MovimientoAvatarTest::comoUnoVersion);
        assertFalse(mongo.documento(2).estado().contains("avatar"));
        colocar(aplicacion, 1, 1, Direccion.EAST);

        Resultado<VistaHud> cargado = aplicacion.controlador.cargar(2);

        assertTrue(cargado.isOk(), String.valueOf(cargado.mensaje()));
        assertEquals(new VistaAvatar(ReglasJuego.COL_INICIO_AVATAR, ReglasJuego.FILA_INICIO_AVATAR, "SOUTH"), avatar(aplicacion));
    }

    @Test
    void HU45_ca4_withoutAvatarItGoesToTheNearestFreePlotAndASaveWithTheAvatarInThePondIsRejected() {
        MapaGranja granja = MapaGranja.inicial();
        assertArrayEquals(new int[] {7, 7}, granja.cercanoTransitable(7, 7));
        granja.colocar(TipoEdificio.COOP, 7, 7);
        assertArrayEquals(new int[] {7, 6}, granja.cercanoTransitable(7, 7), "the soil just north of the coop");
        assertArrayEquals(new int[] {12, 1}, granja.cercanoTransitable(13, 1), "next to the pond");

        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.construir(aplicacion.estado(), TipoEdificio.COOP, ReglasJuego.COL_INICIO_AVATAR, ReglasJuego.FILA_INICIO_AVATAR);
        MementoJuego bueno = aplicacion.estado().mementoCrear();
        MementoJuego v1 = new MementoJuego(bueno.fecha(), bueno.dinero(), bueno.energia(), bueno.inventario(), bueno.granja(),
                bueno.hoy(), bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(), bueno.progreso());
        aplicacion.estado().restaurar(v1);
        assertEquals(new VistaAvatar(7, 6, "SOUTH"), avatar(aplicacion));
        assertTrue(ValidadorEstadoJuego.violaciones(v1).isEmpty());

        MementoJuego enEstanqueEl = new MementoJuego(bueno.fecha(), bueno.dinero(), bueno.energia(), bueno.inventario(),
                bueno.granja(), bueno.hoy(), bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(),
                bueno.progreso(), new InstantaneaAvatar(13, 0, Direccion.NORTH));
        MementoJuego onGallineroEl = new MementoJuego(bueno.fecha(), bueno.dinero(), bueno.energia(), bueno.inventario(),
                bueno.granja(), bueno.hoy(), bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(),
                bueno.progreso(), new InstantaneaAvatar(7, 7, Direccion.NORTH));
        MementoJuego ningunaParteMirando = new MementoJuego(bueno.fecha(), bueno.dinero(), bueno.energia(), bueno.inventario(),
                bueno.granja(), bueno.hoy(), bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(),
                bueno.progreso(), new InstantaneaAvatar(5, 8, null));
        assertFalse(ValidadorEstadoJuego.violaciones(enEstanqueEl).isEmpty());
        assertFalse(ValidadorEstadoJuego.violaciones(onGallineroEl).isEmpty());
        assertFalse(ValidadorEstadoJuego.violaciones(ningunaParteMirando).isEmpty());
    }

    @Test
    void HU45_ca4_documentos_de_un_desconocido_version_son_todavia_rechazado() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.controlador.guardar(1);
        for (int version : new int[] {0, CodecGuardado.VERSION_ESQUEMA + 1}) {
            mongo.manipular(1, d -> new DocumentoGuardado(d.id(), version, d.archivadoEn(), d.estado(), d.checksum()));
            assertEquals(CodigoError.VERSION_NO_SOPORTADA, aplicacion.controlador.cargar(1).codigo(), "version " + version);
        }
    }

    /**
     * La misma partida escrita como la escribía la versión 1: sin avatar en el estado, versión 1, suma de control del
     * texto.
     */
    private static DocumentoGuardado comoUnoVersion(DocumentoGuardado documento) {
        try {
            ObjectMapper json = new ObjectMapper();
            ObjectNode estado = (ObjectNode) json.readTree(documento.estado());
            estado.remove("avatar");
            String texto = json.writeValueAsString(estado);
            return new DocumentoGuardado(documento.id(), 1, documento.archivadoEn(), texto, Checksum.of(texto));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
