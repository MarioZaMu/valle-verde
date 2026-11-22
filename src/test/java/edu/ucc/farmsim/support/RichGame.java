package edu.ucc.farmsim.support;

import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.progress.Achievement;

/** Plays a few real days so that a game has crops, a building with animals, market movement and achievements. */
public final class RichGame {

    private RichGame() {
        // static helpers only
    }

    /** Starts a game and plays it into a state where every part of the memento holds something. */
    public static TestApp play() {
        TestApp app = TestApp.startedGame();
        GameFixtures.unlock(app.state(), Achievement.FIRST_HARVEST);
        app.controller.plant(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW, "CARROT_SEED");
        app.controller.plant(GameFixtures.SOIL_COL + 1, GameFixtures.SOIL_ROW, "POTATO_SEED");
        app.controller.water(GameFixtures.SOIL_COL, GameFixtures.SOIL_ROW);
        GameFixtures.plant(app.state(), GameFixtures.SOIL_COL + 2, GameFixtures.SOIL_ROW, CropType.CARROT, 4);
        app.controller.harvest(GameFixtures.SOIL_COL + 2, GameFixtures.SOIL_ROW);
        app.state().inventory().add(ItemType.CARROT, 5);
        app.controller.sell("CARROT", 3);
        app.controller.buy("HAY", 5);
        app.controller.build("COOP", GameFixtures.GRASS_COL, GameFixtures.GRASS_ROW);
        Building coop = app.state().farm().buildings().get(0);
        GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.HEALTHY, 0, false, 0);
        GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.SICK, 3, false, 1);
        app.controller.feed(0);
        app.controller.endDay();
        app.controller.endDay();
        return app;
    }
}
