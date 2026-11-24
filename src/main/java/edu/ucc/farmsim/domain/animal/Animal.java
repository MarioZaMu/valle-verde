package edu.ucc.farmsim.domain.animal;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * Pattern: State — Role: Context (an animal whose behaviour depends on its health state).
 *
 * <p>Guarda lo que siguen RN-24 a RN-27: el estado, {@code daysWithoutFeed}, {@code fedToday} y las unidades de
 * producto que esperan en el edificio.
 *
 * <p>Stories: HU-19, HU-20, HU-21, HU-22, HU-23.
 */
public final class Animal {

    private final AnimalType type;
    private AnimalState state;
    private int daysWithoutFeed;
    private boolean fedToday;
    private int pending;

    /** Crea un animal sano que no ha comido y no tiene nada que recoger (RN-23). */
    public Animal(AnimalType type) {
        this(type, AnimalCondition.HEALTHY, 0, false, 0);
    }

    private Animal(AnimalType type, AnimalCondition condition, int daysWithoutFeed, boolean fedToday, int pending) {
        this.type = type;
        this.state = AnimalState.of(condition);
        this.daysWithoutFeed = daysWithoutFeed;
        this.fedToday = fedToday;
        this.pending = pending;
    }

    /** Reconstruye un animal desde una instantánea (memento, pruebas). */
    public static Animal fromSnapshot(AnimalSnapshot snapshot) {
        return new Animal(snapshot.type(), snapshot.condition(), snapshot.daysWithoutFeed(),
                snapshot.fedToday(), snapshot.pending());
    }

    /** Una copia inmutable de este animal. */
    public AnimalSnapshot snapshot() {
        return new AnimalSnapshot(type, condition(), daysWithoutFeed, fedToday, pending);
    }

    /** Tipo de animal. */
    public AnimalType type() {
        return type;
    }

    /** Sano, hambriento o enfermo. */
    public AnimalCondition condition() {
        return state.condition();
    }

    /** Verdadero cuando el animal está enfermo. */
    public boolean isSick() {
        return condition() == AnimalCondition.SICK;
    }

    /** Días consecutivos sin comer. */
    public int daysWithoutFeed() {
        return daysWithoutFeed;
    }

    /** Si comió hoy. */
    public boolean isFedToday() {
        return fedToday;
    }

    /** Unidades de producto que esperan ser recogidas. */
    public int pending() {
        return pending;
    }

    /** Verdadero si alimentarlo ahora tiene sentido: puede comer y no ha comido hoy (RN-24). */
    public boolean needsFeeding() {
        return state.canEat() && !fedToday;
    }

    /**
     * Marca al animal como alimentado hoy.
     *
     * @return {@code true} si comió; {@code false} si está enfermo o ya comió
     */
    public boolean feed() {
        if (!needsFeeding()) {
            return false;
        }
        fedToday = true;
        return true;
    }

    /**
     * Cura de inmediato a un animal enfermo (RN-27).
     *
     * @return {@code true} si estaba enfermo; {@code false} (y nada cambia) en caso contrario
     */
    public boolean cure() {
        if (!isSick()) {
            return false;
        }
        state = HealthyState.INSTANCE;
        daysWithoutFeed = 0;
        return true;
    }

    /** Toma todas las unidades que esperan ser recogidas. */
    public int collect() {
        int taken = pending;
        pending = 0;
        return taken;
    }

    /** Resuelve el final del día (RN-25) y borra la marca diaria. */
    public void endOfDay() {
        state.onDayEnd(this);
        fedToday = false;
    }

    void recoverAndProduce() {
        daysWithoutFeed = 0;
        state = HealthyState.INSTANCE;
        if (pending < GameRules.MAX_PENDING_PER_ANIMAL) {
            pending++;
        }
    }

    void missMeal() {
        daysWithoutFeed++;
        state = daysWithoutFeed >= GameRules.DAYS_WITHOUT_FEED_TO_SICK ? SickState.INSTANCE : HungryState.INSTANCE;
    }
}
