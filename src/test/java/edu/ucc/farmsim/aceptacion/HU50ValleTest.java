package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucc.farmsim.aplicacion.dto.VistaCasa;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.granja.Casa;
import edu.ucc.farmsim.dominio.granja.Direccion;
import edu.ucc.farmsim.dominio.granja.InstantaneaAvatar;
import edu.ucc.farmsim.dominio.granja.MapaGranja;
import edu.ucc.farmsim.dominio.granja.Terreno;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.infraestructura.persistencia.Checksum;
import edu.ucc.farmsim.infraestructura.persistencia.CodecGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** HU-50: un valle con la granja al oeste y una aldea al este, unidas por un camino (RN-04, RN-37, RN-40). */
class HU50ValleTest {

    /** La primera columna de la aldea: todo lo que está al este de ella pertenece a la aldea. */
    private static final int COL_ALDEA = 26;

    @Test
    void HU50_ca1_theValleyIs48By24WithTheFarmToTheWestAndTheVillageWithItsHousesToTheEast() {
        MapaGranja granja = MapaGranja.inicial();

        assertEquals(48, granja.cols());
        assertEquals(24, granja.filas());
        assertEquals(40, granja.count(Terreno.SOIL));
        assertEquals(976, granja.count(Terreno.GRASS));
        assertEquals(6, granja.count(Terreno.WATER));
        assertEquals(30, granja.count(Terreno.OBSTACLE));
        assertEquals(34, granja.count(Terreno.PATH));
        assertEquals(66, granja.count(Terreno.HOUSE));
        assertEquals(Terreno.SOIL, granja.parcelaEn(4, 2).terreno(), "the farm keeps its soil where it was");
        assertEquals(Terreno.WATER, granja.parcelaEn(13, 0).terreno());
        assertTrue(Casa.HOME.col() < COL_ALDEA, "your house is on the farm");
        for (Casa casa : List.of(Casa.SHOP, Casa.FARMHOUSE, Casa.CLINIC, Casa.COTTAGE)) {
            assertTrue(casa.col() >= COL_ALDEA, casa + " is in the village");
        }
    }

    @Test
    void HU50_ca1_cada_persona_esta_en_la_aldea_en_frente_de_su_casa_y_el_mapa_muestra_los_casas() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        VistaMapa map = aplicacion.controlador.map().value();
        Map<String, Casa> hogares = Map.of("MARTA", Casa.SHOP, "TOMAS", Casa.FARMHOUSE, "LUCIA", Casa.CLINIC,
                "INES", Casa.COTTAGE);

        assertEquals(48, map.cols());
        assertEquals(5, map.casas().size());
        for (VistaNpc npc : map.npcs()) {
            Casa casa = hogares.get(npc.id());
            assertTrue(npc.col() >= COL_ALDEA, npc.name() + " lives in the village");
            assertTrue(npc.col() >= casa.col() && npc.col() < casa.col() + casa.width(), npc.name());
            assertTrue(npc.fila() == casa.fila() - 1 || npc.fila() == casa.fila() + casa.height(),
                    npc.name() + " stands right in front of its house");
            VistaCasa vista = map.casas().stream().filter(h -> h.id().equals(casa.name())).findFirst().orElseThrow();
            assertEquals(casa.col(), vista.col());
            assertEquals(casa.width(), vista.width());
        }
        assertEquals(29, Npc.MARTA.col());
        assertEquals(11, Npc.MARTA.fila());
    }

    @Test
    void HU50_ca2_housesBlockTheWayThePathIsWalkedAndNeitherIsSownNorBuiltOn() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        int energia = aplicacion.estado().energia().actual();
        Casa tienda = Casa.SHOP;
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(tienda.col(), tienda.fila() - 1, Direccion.NORTH));

        assertEquals(CodigoError.MOVIMIENTO_BLOQUEADO, aplicacion.controlador.mover("SOUTH").codigo(), "into the shop");
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(18, 7, Direccion.NORTH));
        assertTrue(aplicacion.controlador.mover("SOUTH").isOk(), "along the path");
        assertEquals("PATH", aplicacion.controlador.map().value().parcelaEn(18, 8).terreno());

        assertFalse(aplicacion.controlador.plantar(20, 12, "CARROT_SEED").isOk(), "on the path");
        assertFalse(aplicacion.controlador.plantar(tienda.col(), tienda.fila(), "CARROT_SEED").isOk(), "on a house");
        assertEquals(CodigoError.UBICACION_INVALIDA, aplicacion.controlador.build("COOP", 20, 12).codigo());
        assertEquals(CodigoError.UBICACION_INVALIDA, aplicacion.controlador.build("COOP", tienda.col() - 1, tienda.fila()).codigo());
        assertEquals(energia, aplicacion.estado().energia().actual());
    }

    @Test
    void HU50_ca6_aSaveOfTheOld16By10MapLoadsWithEverythingOnTheSamePlotsAndIsSavedAsVersion3() throws Exception {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.controlador.tutorialSaltar();
        assertTrue(aplicacion.controlador.plantar(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA, "CARROT_SEED").isOk());
        DatosJuego.build(aplicacion.estado(), TipoEdificio.COOP, 9, 7);
        aplicacion.estado().avatar().restaurarDesde(new InstantaneaAvatar(12, 3, Direccion.WEST));
        assertTrue(aplicacion.controlador.save(1).isOk());
        assertEquals(3, mongo.documento(1).versionEsquema());
        mongo.manipular(1, HU50ValleTest::comoDosVersion);
        assertEquals(ReglasJuego.COLS_MAPA_HEREDADOS * ReglasJuego.FILAS_MAPA_HEREDADOS,
                new ObjectMapper().readTree(mongo.documento(1).estado()).path("granja").path("cultivos").size());

        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED + 1);
        var cargado = aplicacion.controlador.cargar(1);

        assertTrue(cargado.isOk(), String.valueOf(cargado.mensaje()));
        assertNotNull(aplicacion.controlador.inspeccionar(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA).value().cultivo());
        assertEquals("COOP", aplicacion.controlador.map().value().edificioEn(9, 7).tipo());
        assertEquals(12, aplicacion.controlador.map().value().avatar().col());
        assertEquals(48 * 24, aplicacion.estado().mementoCrear().granja().cultivos().size());
        assertTrue(aplicacion.controlador.save(1).isOk());
        assertEquals(CodecGuardado.VERSION_ESQUEMA, mongo.documento(1).versionEsquema());
        assertEquals(3, CodecGuardado.VERSION_ESQUEMA);
    }

    /**
     * La misma partida como la escribía la versión 2: una lista de cultivos de 16 x 10 (la esquina noroeste del valle).
     */
    private static DocumentoGuardado comoDosVersion(DocumentoGuardado documento) {
        try {
            ObjectMapper json = new ObjectMapper();
            ObjectNode estado = (ObjectNode) json.readTree(documento.estado());
            ArrayNode cultivos = (ArrayNode) estado.path("granja").path("cultivos");
            ArrayNode viejo = json.createArrayNode();
            for (int fila = 0; fila < ReglasJuego.FILAS_MAPA_HEREDADOS; fila++) {
                for (int col = 0; col < ReglasJuego.COLS_MAPA_HEREDADOS; col++) {
                    viejo.add(cultivos.get(fila * ReglasJuego.COLS_MAPA + col));
                }
            }
            ((ObjectNode) estado.path("granja")).set("cultivos", viejo);
            String texto = json.writeValueAsString(estado);
            return new DocumentoGuardado(documento.id(), 2, documento.archivadoEn(), texto, Checksum.of(texto));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
