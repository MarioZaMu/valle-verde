package edu.ucc.farmsim.dominio.granja;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;

/**
 * El personaje del jugador en la granja (RN-36): está en una parcela y mira hacia una de cuatro direcciones. Un paso va
 * a la parcela vecina si se puede caminar sobre ella; si no, el avatar se queda donde está pero gira para mirar hacia
 * allí. Caminar no cuesta energía ni mueve el calendario, así que el avatar no sabe nada de ninguno de los dos.
 *
 * <p>Stories: HU-45.
 */
public final class Avatar {

    private int col;
    private int fila;
    private Direccion mirada;

    private Avatar(int col, int fila, Direccion mirada) {
        this.col = col;
        this.fila = fila;
        this.mirada = mirada;
    }

    /** El avatar de una partida nueva: en la parcela de pasto configurada, mirando al sur. */
    public static Avatar start() {
        return new Avatar(ReglasJuego.COL_INICIO_AVATAR, ReglasJuego.FILA_INICIO_AVATAR, Direccion.SOUTH);
    }

    /** Columna de la parcela en que está. */
    public int col() {
        return col;
    }

    /** Fila de la parcela en que está. */
    public int fila() {
        return fila;
    }

    /** La dirección hacia la que mira. */
    public Direccion mirada() {
        return mirada;
    }

    /** Columna de la parcela que tiene enfrente (puede estar fuera del mapa). */
    public int colFrente() {
        return col + mirada.dCol();
    }

    /** Fila de la parcela que tiene enfrente (puede estar fuera del mapa). */
    public int filaFrente() {
        return fila + mirada.dRow();
    }

    /** Verdadero si está en una parcela del área cuya parcela superior izquierda y tamaño se dan. */
    public boolean esDentro(int izquierda, int arriba, int width, int height) {
        return col >= izquierda && col < izquierda + width && fila >= arriba && fila < arriba + height;
    }

    /**
     * Gira hacia la dirección y da un paso a la parcela vecina de ese lado si la granja le deja caminar sobre ella.
     *
     * @return {@code true} si se movió; {@code false} si el camino estaba bloqueado (solo giró)
     */
    public boolean paso(Direccion direccion, MapaGranja granja) {
        mirada = direccion;
        int aCol = col + direccion.dCol();
        int aFila = fila + direccion.dRow();
        if (!granja.esTransitable(aCol, aFila)) {
            return false;
        }
        col = aCol;
        fila = aFila;
        return true;
    }

    /** Copia de la posición y la dirección, para el memento. */
    public InstantaneaAvatar instantanea() {
        return new InstantaneaAvatar(col, fila, mirada);
    }

    /** Devuelve al avatar donde dice la instantánea. */
    public void restaurarDesde(InstantaneaAvatar instantanea) {
        this.col = instantanea.col();
        this.fila = instantanea.fila();
        this.mirada = instantanea.mirada();
    }

    /**
     * Devuelve al avatar a su lugar de inicio, mirando al sur. Si allí se construyó algo, va a la parcela transitable
     * más cercana (se usa al cargar una partida de versión 1, sin avatar).
     */
    public void devolverAInicio(MapaGranja granja) {
        int[] colocar = granja.cercanoTransitable(ReglasJuego.COL_INICIO_AVATAR, ReglasJuego.FILA_INICIO_AVATAR);
        this.col = colocar[0];
        this.fila = colocar[1];
        this.mirada = Direccion.SOUTH;
    }
}
