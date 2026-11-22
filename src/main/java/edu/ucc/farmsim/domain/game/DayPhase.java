package edu.ucc.farmsim.domain.game;

/**
 * The steps of the end of the day in the exact order of RN-03. Step 9 (publishing the events) belongs to
 * the application layer and runs after all of these.
 *
 * <p>Stories: HU-10.
 */
public enum DayPhase {
    /** 1. Weather of the day: rain waters outdoor crops, frost withers them. */
    WEATHER_EFFECTS,
    /** 2. Watering, growth and withering of crops. */
    GROWTH,
    /** 3. Animals: feeding, health and production. */
    ANIMALS,
    /** 4. Market: save previous prices, then recover demand. */
    MARKET,
    /** 5. Calendar: move to the next day (and season, and year). */
    CALENDAR,
    /** 6. Forecast: tomorrow's forecast becomes today's weather and a new one is drawn. */
    FORECAST,
    /** 7. Energy back to the maximum. */
    ENERGY,
    /** 8. Achievements are evaluated. */
    ACHIEVEMENTS
}
