package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import org.junit.jupiter.api.Test;

/** HU-21: animals get hungry and fall sick when neglected (RN-03, RN-25). */
class HU21AnimalStatesTest {

    @Test
    void HU21_ca1_aHenThatIsNeverFedIsHungryTwiceAndThenSick() {
        // Given a healthy hen that is not fed
        TestApp app = TestApp.startedGame();
        Animal hen = GameFixtures.hen(GameFixtures.coop(app.state()));

        // When 1, 2 and 3 days go by
        app.controller.endDay();
        assertEquals(AnimalCondition.HUNGRY, hen.condition());
        assertEquals(1, hen.daysWithoutFeed());
        app.controller.endDay();
        assertEquals(AnimalCondition.HUNGRY, hen.condition());
        assertEquals(2, hen.daysWithoutFeed());
        app.controller.endDay();

        // Then it is hungry with 1, hungry with 2 and sick at 3
        assertEquals(AnimalCondition.SICK, hen.condition());
        assertEquals(3, hen.daysWithoutFeed());
    }

    @Test
    void HU21_ca2_aHungryHenThatAteTodayIsHealthyAgainWhenTheDayEnds() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        Animal hen = GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.HUNGRY, 2, false, 0);
        app.state().inventory().add(ItemType.HAY, 1);
        app.controller.feed(coop.id());

        app.controller.endDay();

        assertEquals(AnimalCondition.HEALTHY, hen.condition());
        assertEquals(0, hen.daysWithoutFeed());
    }

    @Test
    void HU21_ca3_aSickHenStaysSickEatsNothingAndItsCounterDoesNotChange() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        Animal hen = GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.SICK, 3, false, 0);
        app.state().inventory().add(ItemType.HAY, 2);

        app.controller.feed(coop.id());
        app.controller.endDay();
        app.controller.endDay();

        assertEquals(AnimalCondition.SICK, hen.condition());
        assertEquals(3, hen.daysWithoutFeed());
        assertEquals(2, app.state().inventory().count(ItemType.HAY));
    }

    @Test
    void HU21_ca4_theDailyMarkIsClearedAndAnUnfedAnimalCountsADayWithoutFood() {
        // Given a hen fed yesterday
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        Animal hen = GameFixtures.hen(coop);
        app.state().inventory().add(ItemType.HAY, 1);
        app.controller.feed(coop.id());
        app.controller.endDay();

        // When the new day begins
        assertFalse(hen.isFedToday());

        // Then, if the player does not feed her, the day counts as a day without food
        app.controller.endDay();
        assertEquals(AnimalCondition.HUNGRY, hen.condition());
        assertEquals(1, hen.daysWithoutFeed());
    }

    @Test
    void HU21_aFailedEndOfDayRestoresTheAnimalsExactly() {
        TestApp app = TestApp.startedGame();
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.HUNGRY, 1, true, 1);
        var before = app.state().createMemento();

        app.state().farm().buildings().get(0).animals().get(0).endOfDay();
        app.state().restore(before);

        var restored = app.state().farm().buildings().get(0).animals().get(0);
        assertEquals(AnimalCondition.HUNGRY, restored.condition());
        assertEquals(1, restored.daysWithoutFeed());
        assertEquals(1, restored.pending());
        assertEquals(true, restored.isFedToday());
    }
}
