package edu.ucc.farmsim.dominio.animal;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;

/**
 * Pattern: State — Role: Context (an animal whose behaviour depends on its health state).
 *
 * <p>Guarda lo que siguen RN-24 a RN-27: el estado, {@code diasSinAlimentar}, {@code hoyAlimentado} y las unidades de
 * producto que esperan en el edificio.
 *
 * <p>Stories: HU-19, HU-20, HU-21, HU-22, HU-23.
 */
public final class Animal {

    private final TipoAnimal tipo;
    private EstadoAnimal estado;
    private int diasSinAlimentar;
    private boolean hoyAlimentado;
    private int pendiente;

    /** Crea un animal sano que no ha comido y no tiene nada que recoger (RN-23). */
    public Animal(TipoAnimal tipo) {
        this(tipo, CondicionAnimal.HEALTHY, 0, false, 0);
    }

    private Animal(TipoAnimal tipo, CondicionAnimal condicion, int diasSinAlimentar, boolean hoyAlimentado, int pendiente) {
        this.tipo = tipo;
        this.estado = EstadoAnimal.of(condicion);
        this.diasSinAlimentar = diasSinAlimentar;
        this.hoyAlimentado = hoyAlimentado;
        this.pendiente = pendiente;
    }

    /** Reconstruye un animal desde una instantánea (memento, pruebas). */
    public static Animal desdeInstantanea(InstantaneaAnimal instantanea) {
        return new Animal(instantanea.tipo(), instantanea.condicion(), instantanea.diasSinAlimentar(),
                instantanea.hoyAlimentado(), instantanea.pendiente());
    }

    /** Una copia inmutable de este animal. */
    public InstantaneaAnimal instantanea() {
        return new InstantaneaAnimal(tipo, condicion(), diasSinAlimentar, hoyAlimentado, pendiente);
    }

    /** Tipo de animal. */
    public TipoAnimal tipo() {
        return tipo;
    }

    /** Sano, hambriento o enfermo. */
    public CondicionAnimal condicion() {
        return estado.condicion();
    }

    /** Verdadero cuando el animal está enfermo. */
    public boolean esEnfermo() {
        return condicion() == CondicionAnimal.SICK;
    }

    /** Días consecutivos sin comer. */
    public int diasSinAlimentar() {
        return diasSinAlimentar;
    }

    /** Si comió hoy. */
    public boolean esHoyAlimentado() {
        return hoyAlimentado;
    }

    /** Unidades de producto que esperan ser recogidas. */
    public int pendiente() {
        return pendiente;
    }

    /** Verdadero si alimentarlo ahora tiene sentido: puede comer y no ha comido hoy (RN-24). */
    public boolean alimentacionNecesita() {
        return estado.puedeComer() && !hoyAlimentado;
    }

    /**
     * Marca al animal como alimentado hoy.
     *
     * @return {@code true} si comió; {@code false} si está enfermo o ya comió
     */
    public boolean alimentar() {
        if (!alimentacionNecesita()) {
            return false;
        }
        hoyAlimentado = true;
        return true;
    }

    /**
     * Cura de inmediato a un animal enfermo (RN-27).
     *
     * @return {@code true} si estaba enfermo; {@code false} (y nada cambia) en caso contrario
     */
    public boolean curar() {
        if (!esEnfermo()) {
            return false;
        }
        estado = EstadoSano.INSTANCIA;
        diasSinAlimentar = 0;
        return true;
    }

    /** Toma todas las unidades que esperan ser recogidas. */
    public int collect() {
        int tomado = pendiente;
        pendiente = 0;
        return tomado;
    }

    /** Resuelve el final del día (RN-25) y borra la marca diaria. */
    public void terminarDeDia() {
        estado.onFinDia(this);
        hoyAlimentado = false;
    }

    void recuperarYProducir() {
        diasSinAlimentar = 0;
        estado = EstadoSano.INSTANCIA;
        if (pendiente < ReglasJuego.MAXIMO_PENDIENTE_POR_ANIMAL) {
            pendiente++;
        }
    }

    void comidaFalta() {
        diasSinAlimentar++;
        estado = diasSinAlimentar >= ReglasJuego.DIAS_SIN_ALIMENTAR_A_ENFERMO ? EstadoEnfermo.INSTANCIA : EstadoHambriento.INSTANCIA;
    }
}
