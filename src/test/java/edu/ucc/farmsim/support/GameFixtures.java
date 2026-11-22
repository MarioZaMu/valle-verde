package edu.ucc.farmsim.support;

import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropSnapshot;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Season;

/** Helpers that put a game in a known situation without going through the whole day-by-day flow. */
public final class GameFixtures {

    /** A soil plot inside the starting soil block. */
    public static final int SOIL_COL = 5;
    public static final int SOIL_ROW = 3;

    private GameFixtures() {
        // static helpers only
    }

    /** Puts a crop with a given progress on a plot, bypassing the sowing rules. */
    public static Crop plant(GameState state, int col, int row, CropType type, int daysGrown,
                             int daysWithoutWater, boolean watered, boolean withered) {
        Crop crop = Crop.fromSnapshot(new CropSnapshot(type, daysGrown, daysWithoutWater, watered, withered));
        state.farm().plotAt(col, row).plant(crop);
        return crop;
    }

    /** Puts a crop that is alive, unwatered and has grown for the given number of days. */
    public static Crop plant(GameState state, int col, int row, CropType type, int daysGrown) {
        return plant(state, col, row, type, daysGrown, 0, false, false);
    }

    /** Moves the calendar to a date. */
    public static void setDate(int year, Season season, int day) {
        GameClock.getInstance().restore(new GameDate(year, season, day));
    }
}
