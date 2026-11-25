package edu.ucc.farmsim.dominio.granja;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;

/**
 * Las casas del valle (RN-04): la tuya en la granja y cuatro en la aldea, donde viven las personas. Sus parcelas son
 * {@link Terreno#HOUSE}: nadie camina, siembra ni construye allí. Las posiciones salen de {@link ReglasJuego#CASAS}.
 *
 * <p>Stories: HU-50.
 */
public enum Casa {
    /** Tu casa, en la granja. */
    HOME,
    /** La tienda de Marta. */
    SHOP,
    /** La casa de Don Tomás. */
    FARMHOUSE,
    /** La clínica de la Dra. Lucía. */
    CLINIC,
    /** La casa de Inés. */
    COTTAGE;

    /** Columna de la parcela superior izquierda. */
    public int col() {
        return ReglasJuego.CASAS[ordinal()][0];
    }

    /** Fila de la parcela superior izquierda. */
    public int fila() {
        return ReglasJuego.CASAS[ordinal()][1];
    }

    /** Ancho en parcelas. */
    public int width() {
        return ReglasJuego.CASAS[ordinal()][2];
    }

    /** Alto en parcelas. */
    public int height() {
        return ReglasJuego.CASAS[ordinal()][3];
    }
}
