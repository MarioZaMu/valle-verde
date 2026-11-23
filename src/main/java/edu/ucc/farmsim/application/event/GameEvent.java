package edu.ucc.farmsim.application.event;

import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.HudView;

/**
 * Something that happened in the game and the screen may want to show. Events carry immutable views, never
 * domain objects, so the UI can react without importing the domain.
 *
 * <p>Stories: HU-01, HU-10, HU-14, HU-15, HU-17, HU-26, HU-34, HU-45, HU-47.
 */
public sealed interface GameEvent {

    /** A new game was created. */
    record GameStarted(HudView hud) implements GameEvent { }

    /** The day ended; the HUD shows the new date, weather and energy. */
    record DayEnded(HudView hud) implements GameEvent { }

    /** An achievement was unlocked and its reward paid (RN-28). */
    record AchievementUnlocked(String code, String name, int reward) implements GameEvent { }

    /** The summary of the day that just ended (HU-17). */
    record DaySummaryReady(DaySummaryView summary) implements GameEvent { }

    /** Something on the farm map changed (a crop was sown, watered, harvested, grew, withered; a building was built; an animal arrived, ate or produced...). */
    record MapChanged() implements GameEvent { }

    /** The player's money changed (a purchase or a sale). */
    record MoneyChanged(int money) implements GameEvent { }

    /** Something in the inventory changed. */
    record InventoryChanged() implements GameEvent { }

    /** The avatar took a step (RN-36). */
    record AvatarMoved(AvatarView avatar) implements GameEvent { }

    /** An errand of a person of the valley was delivered and paid (RN-38). */
    record ErrandCompleted(String npcId, String name, int reward) implements GameEvent { }

    /** Energy changed after an action. */
    record EnergyChanged(int energy, int maxEnergy) implements GameEvent { }
}
