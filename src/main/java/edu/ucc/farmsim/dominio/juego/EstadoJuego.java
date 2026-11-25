package edu.ucc.farmsim.dominio.juego;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.economia.Inventario;
import edu.ucc.farmsim.dominio.economia.Mercado;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.granja.Avatar;
import edu.ucc.farmsim.dominio.granja.Direccion;
import edu.ucc.farmsim.dominio.granja.MapaGranja;
import edu.ucc.farmsim.dominio.npc.LibroEncargo;
import edu.ucc.farmsim.dominio.progreso.HechosProgreso;
import edu.ucc.farmsim.dominio.progreso.Logro;
import edu.ucc.farmsim.dominio.progreso.SeguidorLogro;
import edu.ucc.farmsim.dominio.progreso.Tutorial;
import edu.ucc.farmsim.dominio.tiempo.AzarEstado;
import edu.ucc.farmsim.dominio.tiempo.CicloClima;
import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;
import java.util.ArrayList;
import java.util.List;

/**
 * Pattern: Memento — Role: Originator (creates and restores snapshots of itself).
 *
 * <p>El estado de una partida: dinero, energía, inventario, mapa de la granja, clima y el generador aleatorio, más el
 * calendario compartido ({@link RelojJuego}). Es la raíz del agregado sobre la que trabaja toda operación. {@link
 * #mementoCrear()} y {@link #restaurar(MementoJuego)} deshacen un final de día fallido (HU-10) y respaldarán el guardado
 * y la carga (HU-29, HU-30).
 *
 * <p>Stories: HU-01, HU-10, HU-14, HU-15, HU-17, HU-18, HU-26, HU-28, HU-45, HU-47, HU-49.
 */
public final class EstadoJuego {

    private int dinero;
    private final Energia energia;
    private final Inventario inventario;
    private final MapaGranja granja;
    private final CicloClima clima;
    private final AzarEstado azar;
    private final Mercado mercado;
    private final LibroDia libro = new LibroDia();
    private final SeguidorLogro progreso = new SeguidorLogro();
    private final Avatar avatar = Avatar.start();
    private final LibroEncargo encargos = new LibroEncargo();
    private final Tutorial tutorial = new Tutorial();

    private EstadoJuego(int dinero, Energia energia, Inventario inventario, MapaGranja granja,
                      CicloClima clima, AzarEstado azar, Mercado mercado) {
        this.dinero = dinero;
        this.energia = energia;
        this.inventario = inventario;
        this.granja = granja;
        this.clima = clima;
        this.azar = azar;
        this.mercado = mercado;
    }

    /**
     * Crea el estado inicial de una partida nueva (RN-30). El calendario se reinicia en Año 1, Primavera, día 1; el día
     * 1 es soleado y el pronóstico del día 2 se sortea con el generador de la semilla dada.
     *
     * @param semilla semilla del generador aleatorio; la misma semilla siempre da la misma partida
     */
    public static EstadoJuego juegoNuevo(long semilla) {
        RelojJuego reloj = RelojJuego.getInstance();
        reloj.restaurar(FechaJuego.start());
        AzarEstado azar = new AzarEstado(semilla);
        Inventario inventario = Inventario.estandar();
        inventario.add(TipoArticulo.CARROT_SEED, ReglasJuego.SEMILLAS_ZANAHORIA_INICIO);
        inventario.add(TipoArticulo.POTATO_SEED, ReglasJuego.SEMILLAS_PAPA_INICIO);
        return new EstadoJuego(ReglasJuego.DINERO_INICIO, new Energia(), inventario, MapaGranja.inicial(),
                CicloClima.iniciarJuegoNuevo(reloj, azar), azar, Mercado.estandar());
    }

    /**
     * Construye una partida desde una instantánea leída de un guardado. La instantánea ya debe ser válida (véase {@link
     * ValidadorEstadoJuego}); el calendario se fija en su fecha.
     */
    public static EstadoJuego desdeMemento(MementoJuego memento) {
        EstadoJuego estado = juegoNuevo(0L);
        estado.restaurar(memento);
        return estado;
    }

    /** Dinero que tiene el jugador. */
    public int dinero() {
        return dinero;
    }

    /** Agrega dinero (una venta, una recompensa). */
    public void ganar(int monto) {
        if (monto < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + monto);
        }
        dinero += monto;
        libro.ingresosAgregar(monto);
    }

    /**
     * Toma dinero si alcanza; el saldo nunca baja de cero (RN-14).
     *
     * @return {@code true} si se gastó; {@code false} (y nada cambia) si el saldo es menor
     */
    public boolean gastar(int monto) {
        if (monto < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + monto);
        }
        if (monto > dinero) {
            return false;
        }
        dinero -= monto;
        libro.gastoAgregar(monto);
        return true;
    }

    /** Lo que se ganó y se gastó hoy. */
    public LibroDia libro() {
        return libro;
    }

    /** Los logros desbloqueados. */
    public SeguidorLogro progreso() {
        return progreso;
    }

    /** Números que miran los logros, tomados de la partida tal como está ahora. */
    public HechosProgreso hechos() {
        int animales = granja.edificios().stream().mapToInt(b -> b.animales().size()).sum();
        return new HechosProgreso(dinero, progreso.cosechas(), progreso.unidadesVendidos(), progreso.edificiosConstruidos(),
                animales, progreso.animalesCurados(), fecha().anio());
    }

    /**
     * Desbloquea todos los logros cuya condición se cumple y paga su recompensa (RN-28). Una recompensa puede cumplir
     * otra condición (el saldo), así que se repite hasta que no se desbloquea nada nuevo.
     *
     * @return los logros desbloqueados ahora, en orden
     */
    public List<Logro> logrosEvaluar() {
        List<Logro> todo = new ArrayList<>();
        List<Logro> fresco = progreso.evaluar(hechos());
        while (!fresco.isEmpty()) {
            for (Logro logro : fresco) {
                ganar(logro.recompensa());
            }
            todo.addAll(fresco);
            fresco = progreso.evaluar(hechos());
        }
        return todo;
    }

    /** El mercado: demanda, precios y tendencias. */
    public Mercado mercado() {
        return mercado;
    }

    /** La energía diaria del jugador. */
    public Energia energia() {
        return energia;
    }

    /** El inventario. */
    public Inventario inventario() {
        return inventario;
    }

    /** El mapa de la granja. */
    public MapaGranja granja() {
        return granja;
    }

    /** Clima de hoy y pronóstico de mañana. */
    public CicloClima clima() {
        return clima;
    }

    /** El personaje del jugador: dónde está y hacia dónde mira (RN-36). */
    public Avatar avatar() {
        return avatar;
    }

    /**
     * Un paso del avatar (RN-36): gira hacia la dirección y se mueve allí si se puede caminar sobre la parcela. No
     * cuesta energía ni mueve el calendario.
     *
     * @return {@code true} si se movió; {@code false} si el camino estaba bloqueado y solo giró
     */
    public boolean avatarPaso(Direccion direccion) {
        return avatar.paso(direccion, granja);
    }

    /** Los encargos de las personas del valle ya hechos (RN-38). */
    public LibroEncargo encargos() {
        return encargos;
    }

    /** Dónde va el jugador en el tutorial de inicio (RN-39). */
    public Tutorial tutorial() {
        return tutorial;
    }

    /** El generador aleatorio de esta partida. */
    public AzarEstado azar() {
        return azar;
    }

    /** Toma una instantánea inmutable de todo lo que cambia durante el juego. */
    public MementoJuego mementoCrear() {
        return new MementoJuego(fecha(), dinero, energia.actual(), inventario.instantanea(),
                granja.instantanea(), clima.hoy(), clima.pronostico(), azar.estado(), mercado.instantanea(),
                libro.instantanea(), progreso.instantanea(), avatar.instantanea(), encargos.instantanea(), tutorial.instantanea());
    }

    /** Deja el estado exactamente como estaba cuando se tomó la instantánea. */
    public void restaurar(MementoJuego memento) {
        RelojJuego.getInstance().restaurar(memento.fecha());
        this.dinero = memento.dinero();
        energia.set(memento.energia());
        inventario.restaurarDesde(memento.inventario());
        granja.restaurarDesde(memento.granja());
        clima.restaurar(memento.hoy(), memento.pronostico());
        azar.restaurar(memento.estadoAzar());
        mercado.restaurarDesde(memento.mercado());
        libro.restaurarDesde(memento.libro());
        progreso.restaurarDesde(memento.progreso());
        if (memento.avatar() == null) {
            avatar.devolverAInicio(granja);
        } else {
            avatar.restaurarDesde(memento.avatar());
        }
        encargos.restaurarDesde(memento.encargosHechos());
        tutorial.restaurarDesde(memento.tutorial());
    }

    /** Fecha actual del calendario. */
    public FechaJuego fecha() {
        return RelojJuego.getInstance().fecha();
    }
}
