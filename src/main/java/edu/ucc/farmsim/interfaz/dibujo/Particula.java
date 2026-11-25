package edu.ucc.farmsim.interfaz.dibujo;

/**
 * Una gota de lluvia o un copo de nieve que cae sobre el mapa. Su posición es una fracción del área del mapa, así que
 * no depende del tamaño de la ventana. Solo existe en la vista: nada del juego lo conoce.
 *
 * <p>Stories: HU-40.
 */
public final class Particula {

    /** Lo que cae. */
    public enum Categoria {
        /** Una gota de lluvia: rápida e inclinada. */
        RAIN,
        /** Un copo de nieve: lento y a la deriva. */
        SNOW
    }

    private final Categoria categoria;
    private final double velocidad;
    private final double deriva;
    private double x;
    private double y;

    Particula(Categoria categoria, double x, double y, double velocidad, double deriva) {
        this.categoria = categoria;
        this.x = x;
        this.y = y;
        this.velocidad = velocidad;
        this.deriva = deriva;
    }

    /** Lo que es esta partícula. */
    public Categoria categoria() {
        return categoria;
    }

    /** Posición horizontal, de 0 (izquierda) a 1 (derecha). */
    public double x() {
        return x;
    }

    /** Posición vertical, de 0 (arriba) a 1 (abajo). */
    public double y() {
        return y;
    }

    void avanzar(double segundos) {
        y += velocidad * segundos;
        x += deriva * segundos;
        y -= Math.floor(y);
        x -= Math.floor(x);
    }
}
