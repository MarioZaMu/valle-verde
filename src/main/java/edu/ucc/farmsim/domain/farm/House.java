package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * The houses of the valley (RN-04): yours on the farm and four in the village, where the people live. Their
 * plots are {@link Terrain#HOUSE}: nobody walks, sows or builds there. Positions come from
 * {@link GameRules#HOUSES}.
 *
 * <p>Stories: HU-50.
 */
public enum House {
    /** Your house, on the farm. */
    HOME,
    /** The shop of Marta. */
    SHOP,
    /** The house of Don Tomás. */
    FARMHOUSE,
    /** The clinic of Dra. Lucía. */
    CLINIC,
    /** The house of Inés. */
    COTTAGE;

    /** Column of the top-left plot. */
    public int col() {
        return GameRules.HOUSES[ordinal()][0];
    }

    /** Row of the top-left plot. */
    public int row() {
        return GameRules.HOUSES[ordinal()][1];
    }

    /** Width in plots. */
    public int width() {
        return GameRules.HOUSES[ordinal()][2];
    }

    /** Height in plots. */
    public int height() {
        return GameRules.HOUSES[ordinal()][3];
    }
}
