package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.farm.Plot;
import java.util.ArrayList;
import java.util.List;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.GameDate;

/**
 * Turns the domain state into the immutable views the UI draws, so the UI never sees domain objects.
 *
 * <p>Stories: HU-01, HU-28, HU-34, HU-35.
 */
public final class ViewMapper {

    /** The top bar of a game state. */
    public HudView hud(GameState state) {
        GameDate date = state.date();
        return new HudView(state.money(), date.year(), date.season().name(), date.day(),
                state.weather().today().name(), state.weather().forecast().name(),
                state.energy().current(), state.energy().max());
    }

    /** The plot at the given coordinates, which must be inside the map. */
    public PlotView plot(GameState state, int col, int row) {
        Plot plot = state.farm().plotAt(col, row);
        Crop crop = plot.crop();
        if (crop == null) {
            return new PlotView(col, row, plot.terrain().name(), null, null, false, 0);
        }
        return new PlotView(col, row, plot.terrain().name(), crop.type().name(), crop.stage().name(),
                crop.isWatered(), crop.daysLeft());
    }

    /** The whole farm map, plot by plot in row-major order. */
    public MapView map(GameState state) {
        int cols = state.farm().cols();
        int rows = state.farm().rows();
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                plots.add(plot(state, col, row));
            }
        }
        return new MapView(cols, rows, List.copyOf(plots));
    }
}

