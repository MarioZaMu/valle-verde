package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.infraestructura.persistencia.Checksum;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.RepositorioGuardadoMongo;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * HU-29 a HU-31 contra un MongoDB real en {@code localhost:27017} (RN-31, RN-32), en una base de datos propia ({@code
 * valleverde_test}) que se borra después de cada prueba, de modo que las partidas guardadas del jugador nunca se tocan.
 * Sin un MongoDB en esta máquina las pruebas se omiten, ya que el juego mismo no necesita uno para jugar.
 */
class HU29MongoRealTest {

    private static final String ANFITRION = "localhost";
    private static final int PUERTO = 27017;
    private static final String BASEDATOS = "valleverde_test";
    private static final String URI = "mongodb://" + ANFITRION + ":" + PUERTO
            + "/?serverSelectionTimeoutMS=2000&connectTimeoutMS=2000&socketTimeoutMS=2000";

    private MongoClient cliente;
    private MongoTemplate mongo;

    @BeforeEach
    void connect() {
        assumeTrue(alcanzable(), "no MongoDB on " + ANFITRION + ":" + PUERTO);
        cliente = MongoClients.create(URI);
        mongo = new MongoTemplate(cliente, BASEDATOS);
        mongo.getDb().drop();
    }

    @AfterEach
    void soltarYCerrar() {
        if (cliente != null) {
            mongo.getDb().drop();
            cliente.close();
        }
    }

    private static boolean alcanzable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ANFITRION, PUERTO), 500);
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private AplicacionTest aplicacion() {
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), new RepositorioGuardadoMongo(mongo));
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.controlador.tutorialSaltar();
        return aplicacion;
    }

    @Test
    void HU29_ca1_guardando_escribe_la_ranura_documento_en_los_real_partidas_coleccion() {
        AplicacionTest aplicacion = aplicacion();

        assertTrue(aplicacion.controlador.save(2).isOk());

        Document almacenado = mongo.getCollection("partidas").find(new Document("_id", "slot2")).first();
        assertNotNull(almacenado, "the document is really in MongoDB");
        assertEquals(3, almacenado.getInteger("versionEsquema"));
        assertEquals(Checksum.of(almacenado.getString("estado")), almacenado.getString("checksum"));
        assertNotNull(almacenado.get("archivadoEn"));
        assertEquals(1, mongo.getCollection("partidas").countDocuments());
    }

    @Test
    void HU29_ca2_whenTheRealMongoRejectsTheWriteThePreviousDocumentStaysAndTheGameGoesOn() {
        AplicacionTest aplicacion = aplicacion();
        assertTrue(aplicacion.controlador.save(1).isOk());
        DocumentoGuardado antes = mongo.findById("slot1", DocumentoGuardado.class);
        aplicacion.controlador.diaFin();
        mongo.getDb().runCommand(new Document("collMod", "partidas")
                .append("validator", new Document("versionEsquema", new Document("$gt", 1000)))
                .append("validationLevel", "strict").append("validationAction", "error"));

        Resultado<?> resultado = aplicacion.controlador.save(1);

        assertEquals(CodigoError.ERROR_ES, resultado.codigo());
        assertEquals(antes, mongo.findById("slot1", DocumentoGuardado.class), "the old document is intact");
        assertEquals(2, aplicacion.controlador.hud().value().dia(), "the game is still in play");
    }

    @Test
    void HU29_ca2_whenMongoDoesNotAnswerSavingFailsWithinSecondsAndTheGameGoesOn() {
        try (MongoClient nowhere = MongoClients.create(
                "mongodb://127.0.0.1:1/?serverSelectionTimeoutMS=500&connectTimeoutMS=500")) {
            AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(),
                    new RepositorioGuardadoMongo(new MongoTemplate(nowhere, BASEDATOS)));
            aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
            long start = System.nanoTime();

            Resultado<?> resultado = aplicacion.controlador.save(1);

            assertEquals(CodigoError.ERROR_ES, resultado.codigo());
            assertTrue((System.nanoTime() - start) / 1_000_000 < 3_000, "gives up within the 2 s of RN-31");
            assertTrue(aplicacion.controlador.hud().isOk());
        }
    }

    @Test
    void HU31_ca1_aGameSavedInTheRealMongoLoadsBackExactlyInAnotherSession() {
        AplicacionTest first = aplicacion();
        assertTrue(first.controlador.plantar(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA, "CARROT_SEED").isOk());
        first.controlador.diaFin();
        assertTrue(first.controlador.save(3).isOk());
        VistaHud hud = first.controlador.hud().value();
        VistaMapa map = first.controlador.map().value();

        AplicacionTest segundo = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), new RepositorioGuardadoMongo(mongo));
        segundo.controlador.juegoNuevo(AplicacionTest.SEED + 7);
        Resultado<VistaHud> cargado = segundo.controlador.cargar(3);

        assertTrue(cargado.isOk(), String.valueOf(cargado.mensaje()));
        assertEquals(hud, segundo.controlador.hud().value());
        assertEquals(map, segundo.controlador.map().value());
        assertEquals("OK", segundo.controlador.guardarRanuras().value().get(2).status());
    }
}
