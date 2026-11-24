package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * Las casas del valle (RN-04): la tuya en la granja y cuatro en la aldea, donde viven las personas. Sus parcelas son
 * {@link Terrain#HOUSE}: nadie camina, siembra ni construye allí. Las posiciones salen de {@link GameRules#HOUSES}.
 *
 * <p>Stories: HU-50.
 */
public enum House {
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
        return GameRules.HOUSES[ordinal()][0];
    }

    /** Fila de la parcela superior izquierda. */
    public int row() {
        return GameRules.HOUSES[ordinal()][1];
    }

    /** Ancho en parcelas. */
    public int width() {
        return GameRules.HOUSES[ordinal()][2];
    }

    /** Alto en parcelas. */
    public int height() {
        return GameRules.HOUSES[ordinal()][3];
    }
}
