package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.JuegoRico;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.infraestructura.persistencia.Checksum;
import edu.ucc.farmsim.infraestructura.persistencia.CodecGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-31: las partidas dañadas, ausentes o no admitidas se rechazan sin tocar la partida en curso (RN-33). */
class HU31GuardadosRechazarInvalidosTest {

    private final MongoEnMemoria mongo = new MongoEnMemoria();
    private AplicacionTest aplicacion;
    private MementoJuego antes;

    @BeforeEach
    void aGameIsInProgressAndSlot1HoldsAGoodSave() {
        aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        aplicacion.controlador.diaFin();
        aplicacion.controlador.guardar(1);
        aplicacion.controlador.diaFin();
        antes = aplicacion.estado().mementoCrear();
        aplicacion.eventos.clear();
    }

    private void afirmarRechazado(Resultado<VistaHud> resultado, CodigoError codigo) {
        assertEquals(codigo, resultado.codigo(), String.valueOf(resultado.mensaje()));
        assertEquals(antes, aplicacion.estado().mementoCrear(), "the game in progress must have 0 differences");
        assertTrue(aplicacion.eventos.isEmpty());
    }

    @Test
    void HU31_ca1_un_vacio_ranura_es_rechazado_como_inexistente_critico_test() {
        Resultado<VistaHud> resultado = aplicacion.controlador.cargar(3);

        afirmarRechazado(resultado, CodigoError.ARCHIVO_INEXISTENTE);
        assertEquals("No existe una partida en la ranura 3.", resultado.mensaje());
    }

    @Test
    void HU31_ca2_aChangedCharacterIsRejectedAsCorrupt_criticalTest() {
        mongo.manipular(1, d -> new DocumentoGuardado(d.id(), d.versionEsquema(), d.archivadoEn(),
                d.estado().replaceFirst("\"dinero\":(\\d)", "\"dinero\":9"), d.checksum()));

        afirmarRechazado(aplicacion.controlador.cargar(1), CodigoError.ARCHIVO_CORRUPTO);
    }

    @Test
    void HU31_ca2_aTruncatedJsonIsRejectedAsCorrupt_criticalTest() {
        mongo.manipular(1, d -> new DocumentoGuardado(d.id(), d.versionEsquema(), d.archivadoEn(),
                d.estado().substring(0, d.estado().length() / 2), d.checksum()));

        Resultado<VistaHud> resultado = aplicacion.controlador.cargar(1);

        afirmarRechazado(resultado, CodigoError.ARCHIVO_CORRUPTO);
        assertEquals("La partida está dañada y no se puede cargar. Tu partida actual no cambió.", resultado.mensaje());
    }

    @Test
    void HU31_ca3_un_desconocido_esquema_version_es_rechazado() {
        mongo.manipular(1, d -> new DocumentoGuardado(d.id(), 99, d.archivadoEn(), d.estado(), d.checksum()));

        Resultado<VistaHud> resultado = aplicacion.controlador.cargar(1);

        afirmarRechazado(resultado, CodigoError.VERSION_NO_SOPORTADA);
        assertEquals("La partida es de una versión no soportada.", resultado.mensaje());
    }

    @Test
    void HU31_ca4_negativeMoneyWithAValidChecksumIsRejectedByTheInvariants() {
        DatosJuego.dineroFijar(aplicacion.estado(), 100);
        MementoJuego bueno = aplicacion.estado().mementoCrear();
        MementoJuego roto = new MementoJuego(bueno.fecha(), -5, bueno.energia(), bueno.inventario(), bueno.granja(),
                bueno.hoy(), bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(), bueno.progreso());
        mongo.put(new CodecGuardado().codificar(2, roto, Instant.now()));
        antes = aplicacion.estado().mementoCrear();

        afirmarRechazado(aplicacion.controlador.cargar(2), CodigoError.ARCHIVO_CORRUPTO);
    }

    @Test
    void HU31_ca4_aMalformedJsonIsDetectedBeforeTheChecksumAndTheVersion() {
        String roto = "{\"fecha\":";
        mongo.put(new DocumentoGuardado("slot2", 99, Instant.now(), roto, "not-a-checksum"));

                // primero se comprueba el JSON: la versión y la suma de control equivocadas nunca se alcanzan
        afirmarRechazado(aplicacion.controlador.cargar(2), CodigoError.ARCHIVO_CORRUPTO);
    }

    @Test
    void HU31_ca4_aValidJsonWithAWrongVersionIsRejectedByVersionBeforeTheChecksum() {
        mongo.put(new DocumentoGuardado("slot2", 99, Instant.now(), "{}", "not-a-checksum"));

        afirmarRechazado(aplicacion.controlador.cargar(2), CodigoError.VERSION_NO_SOPORTADA);
    }

    @Test
    void HU31_ca4_everyInvariantOfRn33IsChecked() {
        MementoJuego bueno = JuegoRico.jugar().estado().mementoCrear();
        var listaEdificio = bueno.granja().edificios();
        var inventario = bueno.inventario();

        var energiaMuchoDemasiado = new MementoJuego(bueno.fecha(), bueno.dinero(), 101, inventario, bueno.granja(), bueno.hoy(),
                bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(), bueno.progreso());
        var solapado = new MementoJuego(bueno.fecha(), bueno.dinero(), bueno.energia(), inventario,
                new edu.ucc.farmsim.dominio.granja.InstantaneaMapaGranja(bueno.granja().cultivos(),
                        java.util.List.of(listaEdificio.get(0), listaEdificio.get(0))),
                bueno.hoy(), bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(), bueno.progreso());
        var inventarioIncorrecto = new MementoJuego(bueno.fecha(), bueno.dinero(), bueno.energia(),
                new edu.ucc.farmsim.dominio.economia.InstantaneaInventario(30, inventario.ranuras()), bueno.granja(), bueno.hoy(),
                bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(), bueno.progreso());
        var logroFalso = new MementoJuego(bueno.fecha(), bueno.dinero(), bueno.energia(), inventario, bueno.granja(),
                bueno.hoy(), bueno.pronostico(), bueno.estadoAzar(), bueno.mercado(), bueno.libro(),
                new edu.ucc.farmsim.dominio.progreso.InstantaneaProgreso(
                        java.util.Set.of(edu.ucc.farmsim.dominio.progreso.Logro.BIG_HARVESTER), 0, 0, 0, 0));

        for (MementoJuego malo : java.util.List.of(energiaMuchoDemasiado, solapado, inventarioIncorrecto, logroFalso)) {
            assertTrue(!edu.ucc.farmsim.dominio.juego.ValidadorEstadoJuego.violaciones(malo).isEmpty(), String.valueOf(malo));
        }
        assertEquals(java.util.List.of(), edu.ucc.farmsim.dominio.juego.ValidadorEstadoJuego.violaciones(bueno));
        assertEquals(1, edu.ucc.farmsim.dominio.juego.ValidadorEstadoJuego.violaciones(null).size());
    }

    @Test
    void HU31_ca5_cuando_mongo_bd_hace_no_respuesta_cargando_reportes_error_es_y_el_juego_hace_no_cambiar() {
        mongo.abajoIr();

        Resultado<VistaHud> resultado = aplicacion.controlador.cargar(1);

        afirmarRechazado(resultado, CodigoError.ERROR_ES);
        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", resultado.mensaje());
    }

    @Test
    void HU31_theChecksumIsTheSha256OfTheStateText() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Checksum.of("abc"));
    }
}
