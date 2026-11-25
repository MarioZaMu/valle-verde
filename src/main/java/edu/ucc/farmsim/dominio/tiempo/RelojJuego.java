package edu.ucc.farmsim.dominio.tiempo;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;

/**
 * Pattern: Singleton — Role: Singleton instance (the game's only calendar).
 *
 * <p>Guarda la fecha actual y la avanza de un día a la vez (RN-01). Hay un calendario por partida en marcha; los
 * servicios y el memento lo leen y restauran a través de esta instancia. El juego usa un solo hilo (S-01), así que no
 * hace falta sincronización.
 *
 * <p>Las pruebas deben llamar a {@link #reiniciarParaTests()} antes de cada caso para que ninguna fecha se filtre entre
 * ellos.
 *
 * <p>Stories: HU-09.
 */
public final class RelojJuego {

    private static final RelojJuego INSTANCIA = new RelojJuego();

    private int anio;
    private int dia;
    private EstadoEstacion estacion;

    private RelojJuego() {
        reiniciarParaTests();
    }

    /** Devuelve el único reloj. */
    public static RelojJuego getInstance() {
        return INSTANCIA;
    }

    /** Fecha actual. */
    public FechaJuego fecha() {
        return new FechaJuego(anio, estacion.estacion(), dia);
    }

    /** Estado de la estación actual. */
    public EstadoEstacion estadoEstacion() {
        return estacion;
    }

    /** La fecha del día siguiente, sin avanzar el reloj. */
    public FechaJuego manana() {
        if (dia < ReglasJuego.DIAS_POR_ESTACION) {
            return new FechaJuego(anio, estacion.estacion(), dia + 1);
        }
        EstadoEstacion next = estacion.next();
        int anioSiguiente = estacion.estacion() == Estacion.WINTER ? anio + 1 : anio;
        return new FechaJuego(anioSiguiente, next.estacion(), ReglasJuego.DIA_INICIO);
    }

    /** Mueve el calendario un día adelante: el día 28 de una estación pasa al día 1 de la siguiente. */
    public void diaAvanzar() {
        FechaJuego next = manana();
        this.anio = next.anio();
        this.dia = next.dia();
        this.estacion = EstadoEstacion.of(next.estacion());
    }

    /** Fija el calendario en una fecha dada (lo usa el memento al restaurar una partida). */
    public void restaurar(FechaJuego fecha) {
        this.anio = fecha.anio();
        this.dia = fecha.dia();
        this.estacion = EstadoEstacion.of(fecha.estacion());
    }

    /** Devuelve el reloj al Año 1, Primavera, día 1. */
    public void reiniciarParaTests() {
        restaurar(FechaJuego.start());
    }
}
