package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.farm.AvatarSnapshot;
import edu.ucc.farmsim.domain.farm.Direction;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.support.RecordingDialogs;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.components.DialoguePresenter;
import edu.ucc.farmsim.ui.components.Tool;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.render.MapGeometry;
import edu.ucc.farmsim.ui.render.MapRenderer;
import edu.ucc.farmsim.ui.screens.GameScreenPresenter;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-46 y HU-47 en pantalla: E o un clic abre el diálogo, que entrega el encargo; el mapa las dibuja. */
class HU46NpcScreenTest {

    private static final int W = 1024;
    private static final int H = 640;

    private final List<String> messages = new ArrayList<>();
    private final RecordingDialogs dialogs = new RecordingDialogs();
    private final ToolModel tools = new ToolModel();

    private GameScreenPresenter presenter(TestApp app) {
        return new GameScreenPresenter(app.controller, tools, messages::add, dialogs);
    }

    @Test
    void HU46_ca1_eNextToAPersonOpensItsDialogueAndFarAwayItSaysNobodyIsNear() {
        TestApp app = TestApp.startedGame();
        GameScreenPresenter presenter = presenter(app);

        presenter.onTalkKey();
        assertEquals(Texts.NOBODY_TO_TALK, messages.get(messages.size() - 1));
        assertTrue(dialogs.opened.isEmpty());

        app.state().avatar().restoreFrom(new AvatarSnapshot(Npc.MARTA.col() - 1, Npc.MARTA.row(), Direction.SOUTH));
        presenter.onTalkKey();

        assertEquals(List.of("dialogue:MARTA"), dialogs.opened);
        assertEquals("Marta", dialogs.dialogues.get(0).name());
    }

    @Test
    void HU46_ca2_aClickOnAPersonTalksToItWhateverTheToolAndFromFarAwaySaysToComeCloser() {
        TestApp app = TestApp.startedGame();
        tools.select(Tool.WATER);
        GameScreenPresenter presenter = presenter(app);

        presenter.onPlotClicked(Npc.TOMAS.col(), Npc.TOMAS.row());
        assertEquals("Acércate a Don Tomás para hablar.", messages.get(messages.size() - 1));

        app.state().avatar().restoreFrom(new AvatarSnapshot(Npc.TOMAS.col(), Npc.TOMAS.row() + 1, Direction.NORTH));
        presenter.onPlotClicked(Npc.TOMAS.col(), Npc.TOMAS.row());
        assertEquals(List.of("dialogue:TOMAS"), dialogs.opened);
    }

    @Test
    void HU47_ca1_theDialogueDeliversTheErrandAndThenOnlyThanks() {
        TestApp app = TestApp.startedGame();
        app.state().avatar().restoreFrom(new AvatarSnapshot(Npc.MARTA.col() - 1, Npc.MARTA.row(), Direction.EAST));
        DialoguePresenter dialogue = new DialoguePresenter(app.controller, app.controller.talk("MARTA").value());
        assertTrue(dialogue.canDeliver());

        assertEquals("Te faltan 5 Zanahorias para el encargo.", dialogue.deliver());
        assertTrue(dialogue.canDeliver());
        app.state().inventory().add(ItemType.CARROT, 5);
        assertEquals(Texts.ERRAND_DELIVERED, dialogue.deliver());

        assertFalse(dialogue.canDeliver());
        assertEquals("¡Gracias otra vez por traerme 5 Zanahorias!", dialogue.view().errand());
        assertEquals(Texts.ERRAND_DELIVERED, dialogue.message());
    }

    @Test
    void HU47_ca1_thePeopleAreDrawnWithABubbleWhileTheirErrandIsPending() {
        for (AssetManager assets : List.of(AssetManager.fromClasspath(), AssetManager.vectorOnly())) {
            MapRenderer renderer = new MapRenderer(assets);
            int[] nobody = paint(renderer, field(false, false), 10, 1);
            int[] waiting = paint(renderer, field(true, false), 10, 1);
            int[] done = paint(renderer, field(true, true), 10, 1);

            assertFalse(Arrays.equals(nobody, waiting), "the person is drawn");
            assertFalse(Arrays.equals(waiting, done), "the bubble goes away when the errand is done");
        }
    }

    private static MapView field(boolean withMarta, boolean done) {
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 16; col++) {
                plots.add(new PlotView(col, row, "GRASS", null, null, false, 0));
            }
        }
        List<NpcView> npcs = withMarta
                ? List.of(new NpcView("MARTA", "Marta", "Vendedora del mercado", 10, 1, "CARROT", 5, 60, done))
                : List.of();
        return new MapView(16, 10, plots, List.of(), null, npcs);
    }

    private static int[] paint(MapRenderer renderer, MapView map, int col, int row) {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        renderer.render(g, map, W, H, null);
        g.dispose();
        Rectangle c = MapGeometry.fit(16, 10, W, H).cellBounds(col, row);
        Rectangle around = new Rectangle(c.x, c.y - c.height, c.width, c.height * 2);
        return image.getRGB(around.x, around.y, around.width, around.height, null, 0, around.width);
    }
}
