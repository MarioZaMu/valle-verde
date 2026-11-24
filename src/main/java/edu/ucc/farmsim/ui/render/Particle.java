package edu.ucc.farmsim.ui.render;

/**
 * Una gota de lluvia o un copo de nieve que cae sobre el mapa. Su posición es una fracción del área del mapa, así que
 * no depende del tamaño de la ventana. Solo existe en la vista: nada del juego lo conoce.
 *
 * <p>Stories: HU-40.
 */
public final class Particle {

    /** Lo que cae. */
    public enum Kind {
        /** Una gota de lluvia: rápida e inclinada. */
        RAIN,
        /** Un copo de nieve: lento y a la deriva. */
        SNOW
    }

    private final Kind kind;
    private final double speed;
    private final double drift;
    private double x;
    private double y;

    Particle(Kind kind, double x, double y, double speed, double drift) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.drift = drift;
    }

    /** Lo que es esta partícula. */
    public Kind kind() {
        return kind;
    }

    /** Posición horizontal, de 0 (izquierda) a 1 (derecha). */
    public double x() {
        return x;
    }

    /** Posición vertical, de 0 (arriba) a 1 (abajo). */
    public double y() {
        return y;
    }

    void advance(double seconds) {
        y += speed * seconds;
        x += drift * seconds;
        y -= Math.floor(y);
        x -= Math.floor(x);
    }
}
