package edu.ucc.farmsim.dominio.juego;

/**
 * Los pasos del final del día en el orden exacto de RN-03. El paso 9 (publicar los eventos) pertenece a la capa de
 * aplicación y corre después de todos estos.
 *
 * <p>Stories: HU-10.
 */
public enum FaseDia {
    /** 1. Clima del día: la lluvia riega los cultivos a la intemperie, la helada los marchita. */
    WEATHER_EFFECTS,
    /** 2. Riego, crecimiento y marchitamiento de los cultivos. */
    GROWTH,
    /** 3. Animales: alimentación, salud y producción. */
    ANIMALS,
    /** 4. Mercado: guardar los precios anteriores y luego recuperar la demanda. */
    MARKET,
    /** 5. Calendario: pasar al día siguiente (y a la estación y al año). */
    CALENDAR,
    /** 6. Pronóstico: el pronóstico de mañana pasa a ser el clima de hoy y se sortea uno nuevo. */
    FORECAST,
    /** 7. La energía vuelve al máximo. */
    ENERGY,
    /** 8. Se evalúan los logros. */
    ACHIEVEMENTS
}
