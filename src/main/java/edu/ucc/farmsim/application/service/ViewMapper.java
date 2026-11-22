package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.GameDate;

/**
 * Turns the domain state into the immutable views the UI draws, so the UI never sees domain objects.
 *
 * <p>Stories: HU-28, HU-34.
 */
public final class ViewMapper {

    /** The top bar of a game state. */
    public HudView hud(GameState state) {
        GameDate date = state.date();
        return new HudView(state.money(), date.year(), date.season().name(), date.day(),
                state.weather().today().name(), state.weather().forecast().name(),
                state.energy().current(), state.energy().max());
    }
}
