package edu.ucc.farmsim.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaAccionEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificios;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaInventario;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaMercado;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.ConfiguracionTestSinMongo;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.RepositorioGuardadoFalso;
import edu.ucc.farmsim.configuracion.AplicacionServidor;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * El cliente contra el servidor real en un puerto libre: debe dar los mismos resultados y los mismos eventos que el
 * controlador en proceso (DT-17, DT-19), y nunca lanzar excepciones (R-03).
 */
@SpringBootTest(classes = AplicacionServidor.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("no-mongo")
@Import(ConfiguracionTestSinMongo.class)
class ClienteJuegoRemotoTest {

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    @LocalServerPort
    private int puerto;
    @Autowired
    private SesionJuego sesion;
    @Autowired
    private RepositorioGuardadoFalso guardados;

    private final BusEvento bus = new BusEvento();
    private final List<EventoJuego> eventos = new ArrayList<>();
    private ClienteJuegoRemoto cliente;

    @BeforeEach
    void connect() {
        RelojJuego.getInstance().reiniciarParaTests();
        sesion.replace(null);
        guardados.reiniciar();
        cliente = new ClienteJuegoRemoto(URI.create("http://127.0.0.1:" + puerto), bus);
        bus.suscribir(eventos::add);
    }

    @Test
    void hudElReconstruyeJuegoNuevoYJuegoRepublicaIniciadoOnBusElLocal() {
        Resultado<VistaHud> resultado = cliente.juegoNuevo(AplicacionTest.SEED);

        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(500, resultado.value().dinero());
        assertEquals(1, eventos.size());
        assertEquals(new EventoJuego.JuegoIniciado(resultado.value()), eventos.get(0));
    }

    @Test
    void RNF02_la_ventana_puede_abrir_antes_el_servidor_es_arriba_y_un_orden_espera_para_eso() throws Exception {
        java.util.concurrent.CompletableFuture<URI> servidor = new java.util.concurrent.CompletableFuture<>();
        ClienteJuegoRemoto early = new ClienteJuegoRemoto(servidor, bus, java.time.Duration.ofSeconds(5));
        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            servidor.complete(URI.create("http://127.0.0.1:" + puerto));
        }).start();

        long start = System.nanoTime();
        Resultado<VistaHud> resultado = early.juegoNuevo(AplicacionTest.SEED);

        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertTrue((System.nanoTime() - start) / 1_000_000 >= 250, "it waited for the server");
        assertEquals(500, resultado.value().dinero());
    }

    @Test
    void RNF02_aServerThatFailsToStartOrNeverComesUpIsAnInternalErrorNotAHang() {
        java.util.concurrent.CompletableFuture<URI> fallido = new java.util.concurrent.CompletableFuture<>();
        fallido.completeExceptionally(new IllegalStateException("port in use"));
        ClienteJuegoRemoto roto = new ClienteJuegoRemoto(fallido, bus, java.time.Duration.ofSeconds(5));
        ClienteJuegoRemoto nunca = new ClienteJuegoRemoto(new java.util.concurrent.CompletableFuture<>(), bus,
                java.time.Duration.ofMillis(200));

        assertEquals(CodigoError.ERROR_INTERNO, roto.hud().codigo());
        assertEquals(CodigoError.ERROR_INTERNO, nunca.juegoNuevo(AplicacionTest.SEED).codigo());
        assertFalse(nunca.tieneJuegoEnProgreso());
        assertTrue(eventos.isEmpty());
    }

    @Test
    void tieneJuegoEnServidorElSigueProgreso() {
        assertFalse(cliente.tieneJuegoEnProgreso());
        cliente.juegoNuevo(AplicacionTest.SEED);
        assertTrue(cliente.tieneJuegoEnProgreso());
    }

    @Test
    void HU45_aStepThroughTheServerGivesTheSameAnswerAndEventAsTheLocalController() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        cliente.tutorialSaltar();
        AplicacionTest local = AplicacionTest.juegoIniciado();
        eventos.clear();

        Resultado<VistaAvatar> remoto = cliente.mover("EAST");
        Resultado<VistaAvatar> aqui = local.controlador.mover("EAST");

        assertTrue(remoto.isOk(), String.valueOf(remoto.mensaje()));
        assertEquals(aqui.value(), remoto.value());
        assertEquals(local.eventos, eventos);
        assertEquals(aqui.value(), cliente.map().value().avatar());
        cliente.mover("SOUTH");
        cliente.mover("WEST");
        Resultado<VistaAvatar> bloqueado = cliente.mover("SOUTH");
        assertEquals(CodigoError.MOVIMIENTO_BLOQUEADO, bloqueado.codigo());
        assertEquals("No puedes pasar por ahí.", bloqueado.mensaje());
    }

    @Test
    void HU49_el_tutorial_por_el_servidor_coincide_el_local_controlador_paso_por_paso() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        AplicacionTest local = AplicacionTest.create();
        local.controlador.juegoNuevo(AplicacionTest.SEED);
        local.eventos.clear();
        eventos.clear();

        assertEquals(local.controlador.tutorial().value(), cliente.tutorial().value());
        assertEquals(local.controlador.mover("EAST").value(), cliente.mover("EAST").value());
        assertEquals(local.controlador.plantar(8, 6, "CARROT_SEED").value(), cliente.plantar(8, 6, "CARROT_SEED").value());
        assertEquals(local.controlador.tutorialSaltar().value(), cliente.tutorialSaltar().value());
        assertEquals(local.eventos, eventos, "the same TutorialAvanzado events, in the same places");
        assertTrue(cliente.tutorial().value().concluido());
        sesion.replace(null);
        assertEquals(CodigoError.SIN_PARTIDA, cliente.tutorial().codigo());
    }

    @Test
    void HU46_HU47_talkingAndDeliveringThroughTheServerMatchTheLocalController() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        AplicacionTest local = AplicacionTest.juegoIniciado();
        for (var estado : List.of(sesion.actual(), local.estado())) {
            estado.avatar().restaurarDesde(new edu.ucc.farmsim.dominio.granja.InstantaneaAvatar(28, 11,
                    edu.ucc.farmsim.dominio.granja.Direccion.EAST));
            estado.inventario().add(TipoArticulo.CARROT, 5);
        }
        eventos.clear();

        assertEquals(local.controlador.npcs().value(), cliente.npcs().value());
        assertEquals(local.controlador.hablar("MARTA").value(), cliente.hablar("MARTA").value());
        assertEquals(local.controlador.entregar("MARTA").value(), cliente.entregar("MARTA").value());
        assertEquals(local.eventos, eventos);
        assertEquals(CodigoError.NPC_LEJOS, cliente.hablar("INES").codigo());
        assertEquals(local.controlador.map().value(), cliente.map().value());
    }

    @Test
    void eventosElRepublicaSiembraDeOrdenElEnEllosServidorElOrdenElProducido() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        eventos.clear();

        Resultado<VistaParcela> resultado = cliente.plantar(COL, FILA, "CARROT_SEED");

        assertTrue(resultado.isOk());
        assertEquals("CARROT", resultado.value().cultivo());
        assertEquals(List.of(new EventoJuego.MapaCambiado(), new EventoJuego.EnergiaCambiada(98, 100),
                new EventoJuego.InventarioCambiado()), eventos);
    }

    @Test
    void aRejectionKeepsItsCodeAndSpanishMessageAndPublishesNothing() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        eventos.clear();

        Resultado<VistaParcela> resultado = cliente.plantar(COL, FILA, "PUMPKIN_SEED");

        assertFalse(resultado.isOk());
        assertEquals(CodigoError.FUERA_DE_ESTACION, resultado.codigo());
        assertEquals("No se puede sembrar Calabaza en Primavera: solo en Otoño.", resultado.mensaje());
        assertTrue(eventos.isEmpty());
    }

    @Test
    void withoutAGameTheServerAnswersSinPartida() {
        Resultado<VistaHud> resultado = cliente.hud();

        assertFalse(resultado.isOk());
        assertEquals(CodigoError.SIN_PARTIDA, resultado.codigo());
        assertEquals("No hay una partida en curso.", resultado.mensaje());
    }

    @Test
    void mapaElYAtrasVenirParcelasElComoDibujarPantallasElVistasEl() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        cliente.plantar(COL, FILA, "CARROT_SEED");

        Resultado<VistaMapa> map = cliente.map();
        Resultado<VistaParcela> parcela = cliente.inspeccionar(COL, FILA);

        assertTrue(map.isOk());
        assertEquals(48, map.value().cols());
        assertEquals(5, map.value().casas().size());
        assertEquals("CARROT", map.value().parcelaEn(COL, FILA).cultivo());
        assertEquals(map.value().parcelaEn(COL, FILA), parcela.value());
    }

    @Test
    void terminarRiegoDiaYDemasiadoViajarCosechar() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        cliente.plantar(COL, FILA, "CARROT_SEED");

        assertTrue(cliente.agua(COL, FILA).isOk());
        assertEquals(CodigoError.YA_REGADO, cliente.agua(COL, FILA).codigo());
        assertEquals(CodigoError.NO_MADURO, cliente.cosecha(COL, FILA).codigo());
        assertEquals(2, cliente.diaFin().value().dia());
    }

    @Test
    void savedGamesAreReportedAndAStorageOutageBecomesErrorEs() {
        assertEquals(false, cliente.tieneJuegosArchivados().value());
        guardados.conRanura(3);
        assertEquals(true, cliente.tieneJuegosArchivados().value());

        guardados.inalcanzable();
        Resultado<Boolean> abajo = cliente.tieneJuegosArchivados();

        assertEquals(CodigoError.ERROR_ES, abajo.codigo());
        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", abajo.mensaje());
    }

    @Test
    void internoErrorPasaErrorServidorUnInesperadoSinLanzando() {
        cliente.juegoNuevo(AplicacionTest.SEED);

        Resultado<VistaParcela> resultado = cliente.plantar(COL, FILA, "NOT_A_SEED");

        assertEquals(CodigoError.ERROR_INTERNO, resultado.codigo());
    }

    @Test
    void internoErrorPasaServidorUnInalcanzableSinLanzando() {
        ClienteJuegoRemoto perdido = new ClienteJuegoRemoto(URI.create("http://127.0.0.1:1"), bus);

        assertEquals(CodigoError.ERROR_INTERNO, perdido.hud().codigo());
        assertEquals(CodigoError.ERROR_INTERNO, perdido.juegoNuevo(1L).codigo());
        assertFalse(perdido.tieneJuegoEnProgreso());
    }

    @Test
    void resultadosElDaClienteElRemotosMismosYEventosComoElEnControladorProceso() {
        AplicacionTest local = AplicacionTest.create();
        List<EventoJuego> localEvents = local.eventos;
        RelojJuego.getInstance().reiniciarParaTests();

        List<Object> remoto = guion(cliente, eventos);
        RelojJuego.getInstance().reiniciarParaTests();
        List<Object> inProcess = guion(local.controlador, localEvents);

        assertEquals(inProcess, remoto);
    }

    @Test
    void comprandoVendiendoYMercadoElYClienteElPorViajarVistasInventario() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        sesion.actual().inventario().add(TipoArticulo.CARROT, 3);
        eventos.clear();

        Resultado<Integer> cotizar = cliente.cotizar("CARROT", 3);
        Resultado<Integer> sale = cliente.vender("CARROT", 3);
        Resultado<Integer> purchase = cliente.comprar("HAY", 4);
        Resultado<VistaMercado> mercado = cliente.mercado();
        Resultado<VistaInventario> inventario = cliente.inventario();

        assertEquals(79, cotizar.value());
        assertEquals(79, sale.value());
        assertEquals(20, purchase.value());
        assertEquals(7, mercado.value().vende().size());
        assertEquals("Heno", mercado.value().compra().stream().filter(o -> o.idArticulo().equals("HAY")).findFirst().orElseThrow().name());
        assertEquals(24, inventario.value().capacidad());
        assertEquals(List.of(new EventoJuego.DineroCambiado(579), new EventoJuego.InventarioCambiado(),
                new EventoJuego.DineroCambiado(559), new EventoJuego.InventarioCambiado()), eventos);
    }

    @Test
    void animalesEdificiosYClienteElPorViajarResumenDiaEl() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        sesion.actual().inventario().add(TipoArticulo.HAY, 3);
        sesion.actual().ganar(500);
        eventos.clear();

        Resultado<VistaEdificio> construido = cliente.build("COOP", 2, 7);
        Resultado<VistaEdificio> bought = cliente.animalComprar("HEN");
        Resultado<VistaAccionEdificio> fed = cliente.alimentar(0);
        Resultado<VistaEdificios> todo = cliente.edificios();
        Resultado<VistaMapa> map = cliente.map();
        cliente.diaFin();
        Resultado<VistaAccionEdificio> recolectado = cliente.collect(0);

        assertTrue(construido.isOk());
        assertEquals("Gallinero", construido.value().name());
        assertEquals(1, bought.value().animales().size());
        assertEquals(1, fed.value().monto());
        assertEquals(1, todo.value().edificios().size());
        assertEquals(1, map.value().edificios().size());
        assertEquals(0, map.value().edificioEn(3, 8).id());
        assertEquals(1, recolectado.value().monto());
        assertEquals(1, eventos.stream().filter(EventoJuego.ResumenDiaListo.class::isInstance).count());
        assertEquals(CodigoError.UBICACION_INVALIDA, cliente.build("COOP", 3, 2).codigo());
    }

    @Test
    void guardandoRanurasLogrosYClienteElPorViajarCargando() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        eventos.clear();

        Resultado<java.util.List<edu.ucc.farmsim.aplicacion.dto.VistaLogro>> logros = cliente.logros();
        Resultado<edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado> archivado = cliente.save(1);
        cliente.diaFin();
        Resultado<java.util.List<edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado>> ranuras = cliente.guardarRanuras();
        eventos.clear();
        Resultado<VistaHud> cargado = cliente.cargar(1);

        assertEquals(9, logros.value().size());
        assertEquals("OK", archivado.value().status());
        assertEquals(java.util.List.of("OK", "EMPTY", "EMPTY"),
                ranuras.value().stream().map(edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado::status).toList());
        assertEquals(1, cargado.value().dia());
        assertTrue(eventos.stream().anyMatch(EventoJuego.JuegoIniciado.class::isInstance));
        assertEquals(CodigoError.ARCHIVO_INEXISTENTE, cliente.cargar(3).codigo());
        assertEquals("No existe una partida en la ranura 3.", cliente.cargar(3).mensaje());
    }

    @Test
    void logroUnDesbloqueadoOnRepublicadoEsServidorElOnBusElLocal() {
        cliente.juegoNuevo(AplicacionTest.SEED);
        sesion.actual().inventario().add(TipoArticulo.CARROT, 1);
        eventos.clear();
        sesion.actual().progreso().cosecharRegistro();
        DatosJuego.plantar(sesion.actual(), COL, FILA, edu.ucc.farmsim.dominio.cultivo.TipoCultivo.CARROT, 4);

        cliente.cosecha(COL, FILA);

        assertTrue(eventos.contains(new EventoJuego.LogroDesbloqueado("LG-01", "Primera cosecha", 50)));
    }

    /** La misma secuencia de órdenes, registrando cada resultado y luego los eventos que causó cada una. */
    private static List<Object> guion(edu.ucc.farmsim.aplicacion.controlador.ComandosJuego juego, List<EventoJuego> visto) {
        List<Object> log = new ArrayList<>();
        visto.clear();
        log.add(describir(juego.juegoNuevo(AplicacionTest.SEED)));
        log.add(describir(juego.plantar(COL, FILA, "CARROT_SEED")));
        log.add(describir(juego.plantar(COL, FILA, "CARROT_SEED")));
        log.add(describir(juego.plantar(COL + 1, FILA, "PUMPKIN_SEED")));
        log.add(describir(juego.agua(COL, FILA)));
        log.add(describir(juego.agua(COL, FILA)));
        log.add(describir(juego.cosecha(COL, FILA)));
        log.add(describir(juego.diaFin()));
        log.add(describir(juego.inspeccionar(COL, FILA)));
        log.add(describir(juego.map()));
        log.add(describir(juego.comprar("CORN_SEED", 5)));
        log.add(describir(juego.comprar("MEDICINE", 99)));
        log.add(describir(juego.comprar("HAY", 0)));
        log.add(describir(juego.vender("CARROT", 1)));
        log.add(describir(juego.cotizar("CARROT", 1)));
        log.add(describir(juego.mercado()));
        log.add(describir(juego.inventario()));
        log.add(describir(juego.build("COOP", 2, 7)));
        log.add(describir(juego.build("COOP", 3, 2)));
        log.add(describir(juego.build("BARN", 8, 7)));
        log.add(describir(juego.animalComprar("HEN")));
        log.add(describir(juego.animalComprar("COW")));
        log.add(describir(juego.alimentar(0)));
        log.add(describir(juego.collect(0)));
        log.add(describir(juego.curar(0, 0)));
        log.add(describir(juego.edificios()));
        log.add(describir(juego.diaFin()));
        log.add(describir(juego.logros()));
        log.add(juego.save(2).isOk()); // la hora del guardado difiere entre los dos relojes
        log.add(juego.guardarRanuras().value().stream().map(edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado::status).toList());
        log.add(describir(juego.cargar(2)));
        log.add(describir(juego.cargar(3)));
        log.add(List.copyOf(visto));
        return log;
    }

    private static Object describir(Resultado<?> resultado) {
        return resultado.isOk() ? resultado.value() : resultado.codigo() + ": " + resultado.mensaje();
    }
}
