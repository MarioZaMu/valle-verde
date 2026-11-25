package edu.ucc.farmsim.apoyo;

import edu.ucc.farmsim.aplicacion.controlador.ControladorJuego;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.servicio.MapeadorVista;
import edu.ucc.farmsim.aplicacion.servicio.ServicioAnimal;
import edu.ucc.farmsim.aplicacion.servicio.ServicioAvatar;
import edu.ucc.farmsim.aplicacion.servicio.ServicioConversacion;
import edu.ucc.farmsim.aplicacion.servicio.ServicioDia;
import edu.ucc.farmsim.aplicacion.servicio.ServicioEdificio;
import edu.ucc.farmsim.aplicacion.servicio.ServicioGranja;
import edu.ucc.farmsim.aplicacion.servicio.ServicioJuegoGuardado;
import edu.ucc.farmsim.aplicacion.servicio.ServicioLogro;
import edu.ucc.farmsim.aplicacion.servicio.ServicioMercado;
import edu.ucc.farmsim.aplicacion.servicio.ServicioNpc;
import edu.ucc.farmsim.aplicacion.servicio.ServicioProgreso;
import edu.ucc.farmsim.aplicacion.servicio.ServicioTutorial;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.dominio.guardado.RepositorioGuardado;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma el grafo de la aplicación para las pruebas de aceptación: un reloj nuevo, una sesión, un bus de eventos y un
 * controlador.
 */
public final class AplicacionTest {

    /** Semilla fija que usan las pruebas a las que no les importa. */
    public static final long SEED = 2026L;

    /** El momento con el que se sella todo guardado hecho a través de un grafo de pruebas: 2026-11-22 10:30 UTC. */
    public static final Clock RELOJ_FIJO = Clock.fixed(java.time.Instant.parse("2026-11-22T10:30:00Z"),
            java.time.ZoneOffset.UTC);

    public final SesionJuego sesion;
    public final BusEvento bus;
    public final ControladorJuego controlador;
    /** Todo evento publicado en el bus desde que se creó la aplicación. */
    public final List<EventoJuego> eventos = new ArrayList<>();

    private AplicacionTest(SesionJuego sesion, BusEvento bus, ControladorJuego controlador) {
        this.sesion = sesion;
        this.bus = bus;
        this.controlador = controlador;
        bus.suscribir(eventos::add);
    }

    /**
     * Crea el grafo e inicia una partida nueva (Año 1, Primavera, día 1, 500 m, 10 semillas de zanahoria y 5 de papa)
     * con su tutorial de inicio ya saltado, de modo que los eventos de las pruebas sean solo los de la partida misma.
     */
    public static AplicacionTest juegoIniciado() {
        AplicacionTest aplicacion = create();
        aplicacion.controlador.juegoNuevo(SEED);
        aplicacion.controlador.tutorialSaltar();
        aplicacion.eventos.clear();
        return aplicacion;
    }

    /** La partida en curso. */
    public EstadoJuego estado() {
        return sesion.actual();
    }

    /** Crea el grafo con el pipeline estándar de fin de día. */
    public static AplicacionTest create() {
        return create(SecuenciaDia.estandar());
    }

    /** Crea el grafo con un pipeline de fin de día dado (pruebas que necesitan pasos extra o fallidos). */
    public static AplicacionTest create(SecuenciaDia secuencia) {
        RelojJuego.getInstance().reiniciarParaTests();
        SesionJuego sesion = new SesionJuego();
        return create(sesion, secuencia);
    }

    /** Crea el grafo alrededor de una sesión dada (pruebas que necesitan una sesión defectuosa). */
    public static AplicacionTest create(SesionJuego sesion, SecuenciaDia secuencia) {
        return create(sesion, secuencia, new RepositorioGuardadoFalso());
    }

    /** Crea el grafo alrededor de una sesión y un repositorio de partidas dados. */
    public static AplicacionTest create(SesionJuego sesion, SecuenciaDia secuencia, RepositorioGuardado repositorio) {
        return create(sesion, secuencia, repositorio, RELOJ_FIJO);
    }

    /**
     * Crea el grafo alrededor de una sesión, un repositorio de partidas y un reloj dados (el reloj sella los
     * guardados).
     */
    public static AplicacionTest create(SesionJuego sesion, SecuenciaDia secuencia, RepositorioGuardado repositorio, Clock reloj) {
        RelojJuego.getInstance().reiniciarParaTests();
        MapeadorVista mapeador = new MapeadorVista();
        BusEvento bus = new BusEvento();
        ServicioProgreso progreso = new ServicioProgreso(sesion, mapeador, bus);
        ServicioLogro logros = new ServicioLogro(sesion, bus);
        ServicioDia dia = new ServicioDia(sesion, secuencia, mapeador, bus, logros);
        ServicioJuegoGuardado guardados = new ServicioJuegoGuardado(repositorio, sesion, mapeador, bus, reloj);
        ServicioGranja granja = new ServicioGranja(sesion, mapeador, bus, logros);
        ServicioMercado mercado = new ServicioMercado(sesion, bus, logros);
        ServicioEdificio edificios = new ServicioEdificio(sesion, mapeador, bus, logros);
        ServicioAnimal animales = new ServicioAnimal(sesion, mapeador, bus, logros);
        return new AplicacionTest(sesion, bus,
                new ControladorJuego(sesion, mapeador, progreso, dia, guardados, granja, mercado, edificios, animales,
                        logros, new ServicioAvatar(sesion, mapeador, bus),
                        new ServicioNpc(sesion, mapeador, new ServicioConversacion(), bus, logros),
                        new ServicioTutorial(sesion, bus)));
    }
}
