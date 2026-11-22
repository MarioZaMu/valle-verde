package edu.ucc.farmsim.application.event;

import edu.ucc.farmsim.application.dto.HudView;

/**
 * Something that happened in the game and the screen may want to show. Events carry immutable views, never
 * domain objects, so the UI can react without importing the domain.
 *
 * <p>Stories: HU-10, HU-34.
 */
public sealed interface GameEvent {

    /** A new game was created. */
    record GameStarted(HudView hud) implements GameEvent { }

    /** The day ended; the HUD shows the new date, weather and energy. */
    record DayEnded(HudView hud) implements GameEvent { }

    /** Energy changed after an action. */
    record EnergyChanged(int energy, int maxEnergy) implements GameEvent { }
}
