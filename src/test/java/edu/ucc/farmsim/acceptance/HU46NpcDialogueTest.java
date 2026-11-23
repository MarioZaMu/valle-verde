package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.economy.Market;
import edu.ucc.farmsim.domain.farm.AvatarSnapshot;
import edu.ucc.farmsim.domain.farm.Direction;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.farm.Terrain;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.Weather;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.TestApp;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-46: talking to the people of the valley (RN-37). */
class HU46NpcDialogueTest {

    /** Puts the avatar on the plot west of the person, looking at it (south of it for Lucía, east for Inés). */
    static void nextTo(TestApp app, Npc npc) {
        app.state().avatar().restoreFrom(new AvatarSnapshot(npc.col() - 1, npc.row(), Direction.EAST));
    }

    private static String say(TestApp app, Npc npc) {
        Result<DialogueView> result = app.controller.talk(npc.name());
        assertTrue(result.isOk(), String.valueOf(result.message()));
        return result.value().text();
    }

    @Test
    void HU46_ca1_nextToMartaSheTalksAboutPricesInSpanish() {
        TestApp app = TestApp.startedGame();
        nextTo(app, Npc.MARTA);

        Result<DialogueView> result = app.controller.talk("MARTA");

        assertTrue(result.isOk(), String.valueOf(result.message()));
        DialogueView dialogue = result.value();
        assertEquals("Marta", dialogue.name());
        assertEquals("Vendedora del mercado", dialogue.role());
        assertTrue(dialogue.text().startsWith("¡Bienvenido al mercado! Hoy lo mejor pagado es "), dialogue.text());
        assertTrue(dialogue.text().contains(" m por unidad."), dialogue.text());
        assertEquals("Encargo: tráeme 5 Zanahorias y te pago 60 m.", dialogue.errand());
        assertFalse(dialogue.errandDone());
    }

    @Test
    void HU46_ca1_eachPersonTalksAboutItsOwnSubject() {
        TestApp app = TestApp.startedGame();
        nextTo(app, Npc.TOMAS);
        assertEquals("En Primavera conviene sembrar Zanahoria y Papa.", say(app, Npc.TOMAS));
        nextTo(app, Npc.LUCIA);
        assertEquals("Aún no tienes animales. Un Gallinero es un buen comienzo.", say(app, Npc.LUCIA));
        nextTo(app, Npc.INES);
        assertTrue(say(app, Npc.INES).startsWith("El pronóstico para mañana es "));
        assertEquals("Dra. Lucía", app.controller.npcs().value().get(2).name());
    }

    @Test
    void HU46_ca2_fromFarAwayTheTalkIsRejectedWithTheName() {
        TestApp app = TestApp.startedGame();
        for (Npc npc : Npc.values()) {
            Result<DialogueView> result = app.controller.talk(npc.name());

            assertEquals(ErrorCode.NPC_LEJOS, result.code());
        }
        assertEquals("Acércate a Don Tomás para hablar.", app.controller.talk("TOMAS").message());
        app.state().avatar().restoreFrom(new AvatarSnapshot(Npc.MARTA.col() - 1, Npc.MARTA.row() + 1, Direction.EAST));
        assertEquals(ErrorCode.NPC_LEJOS, app.controller.talk("MARTA").code(), "a diagonal is not next to");
        assertEquals(ErrorCode.SIN_PARTIDA, TestApp.create().controller.talk("MARTA").code());
        assertEquals(ErrorCode.ERROR_INTERNO, app.controller.talk("NOBODY").code());
    }

    @Test
    void HU46_ca3_theSameStateAlwaysGivesTheSameWordsAndTheyFollowTheGame() {
        TestApp app = TestApp.startedGame();
        nextTo(app, Npc.TOMAS);
        String spring = say(app, Npc.TOMAS);
        assertEquals(spring, say(app, Npc.TOMAS));
        GameFixtures.setDate(1, Season.SUMMER, 1);
        assertNotEquals(spring, say(app, Npc.TOMAS), "the season");
        String summer = say(app, Npc.TOMAS);
        app.state().weather().restore(Weather.DROUGHT, Weather.SUNNY);
        assertEquals(summer + " Con la Sequía de hoy, regar cuesta el doble.", say(app, Npc.TOMAS), "the weather");

        nextTo(app, Npc.INES);
        app.state().weather().restore(Weather.SUNNY, Weather.RAINY);
        assertEquals("El pronóstico para mañana es Lluvioso. No hará falta regar.", say(app, Npc.INES));
        app.state().weather().restore(Weather.SUNNY, Weather.FROST);
        assertTrue(say(app, Npc.INES).contains("Helada"), "the forecast");

        nextTo(app, Npc.LUCIA);
        Building coop = GameFixtures.coop(app.state());
        GameFixtures.hen(coop);
        assertEquals("1 animal aún no ha comido hoy: dale Heno.", say(app, Npc.LUCIA));
        GameFixtures.house(coop, AnimalType.HEN, AnimalCondition.SICK, 2, false, 0);
        assertEquals("Tienes 1 animal enfermo: cúralo con Medicina antes de que empeore.", say(app, Npc.LUCIA),
                "the sick animals");

        nextTo(app, Npc.MARTA);
        String prices = say(app, Npc.MARTA);
        for (ItemType item : Market.sellableItems()) {
            GameFixtures.setDemand(app.state(), item, GameRules.DEMAND_MIN);
        }
        assertNotEquals(prices, say(app, Npc.MARTA), "the prices");
    }

    @Test
    void HU46_ca4_nobodyCanWalkThroughAPersonNorBuildOnItAndTheyStandOnFreeGrass() {
        TestApp app = TestApp.startedGame();
        FarmMap farm = FarmMap.initial();
        for (Npc npc : Npc.values()) {
            assertEquals(Terrain.GRASS, farm.plotAt(npc.col(), npc.row()).terrain(), npc.name());
            assertFalse(farm.isWalkable(npc.col(), npc.row()));
            assertNull(farm.buildingAt(npc.col(), npc.row()));
            assertEquals(npc, Npc.at(npc.col(), npc.row()));
        }
        app.state().avatar().restoreFrom(new AvatarSnapshot(Npc.MARTA.col() - 1, Npc.MARTA.row(), Direction.NORTH));

        assertEquals(ErrorCode.MOVIMIENTO_BLOQUEADO, app.controller.move("EAST").code());
        assertEquals("EAST", app.controller.map().value().avatar().facing());
        GameFixtures.setMoney(app.state(), 5_000);
        assertEquals(ErrorCode.UBICACION_INVALIDA,
                app.controller.build("COOP", Npc.INES.col(), Npc.INES.row()).code());
        List<NpcView> drawn = app.controller.map().value().npcs();
        assertEquals(4, drawn.size());
        assertEquals(new NpcView("INES", "Inés", "Vecina", 37, 17, "EGG", 4, 80, false), drawn.get(3));
    }

    @Test
    void HU46_ca5_talkingCostsNoEnergyNorTimeAndChangesNothing() {
        TestApp app = TestApp.startedGame();
        nextTo(app, Npc.TOMAS);
        HudView before = app.controller.hud().value();
        var saved = app.state().createMemento();

        app.controller.talk("TOMAS");
        app.controller.npcs();

        assertEquals(before, app.controller.hud().value());
        assertEquals(saved, app.state().createMemento());
        assertTrue(app.events.isEmpty());
    }
}
