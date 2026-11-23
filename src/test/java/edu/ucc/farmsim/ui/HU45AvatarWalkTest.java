package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.domain.farm.AvatarSnapshot;
import edu.ucc.farmsim.domain.farm.Direction;
import edu.ucc.farmsim.support.RecordingDialogs;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.components.Tool;
import edu.ucc.farmsim.ui.components.ToolModel;
import edu.ucc.farmsim.ui.render.MapGeometry;
import edu.ucc.farmsim.ui.render.MapRenderer;
import edu.ucc.farmsim.ui.render.VisualEffects;
import edu.ucc.farmsim.ui.screens.GameScreenPresenter;
import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** HU-45 · CA-3 and CA-5: the tool in front of the avatar and the walk animation, without a display. */
class HU45AvatarWalkTest {

    private static final int W = 1024;
    private static final int H = 640;

    private final List<String> messages = new ArrayList<>();
    private final ToolModel tools = new ToolModel();
    private final AtomicLong now = new AtomicLong(10_000);

    private GameScreenPresenter presenter(TestApp app) {
        return new GameScreenPresenter(app.controller, tools, messages::add, new RecordingDialogs());
    }

    private static MapView field(AvatarView avatar) {
        List<PlotView> plots = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 16; col++) {
                plots.add(new PlotView(col, row, "GRASS", null, null, false, 0));
            }
        }
        return new MapView(16, 10, plots, List.of(), avatar);
    }

    private static int[] paintCell(MapRenderer renderer, MapView map, VisualEffects fx, int col, int row) {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        renderer.render(g, map, W, H, null, fx);
        g.dispose();
        Rectangle c = MapGeometry.fit(16, 10, W, H).cellBounds(col, row);
        return image.getRGB(c.x, c.y, c.width, c.height, null, 0, c.width);
    }

    @Test
    void HU45_ca3_spaceUsesTheToolOnThePlotInFrontAndAClickStillWorksAnywhere() {
        TestApp app = TestApp.startedGame();
        assertTrue(app.controller.plant(7, 6, "CARROT_SEED").isOk());
        assertTrue(app.controller.plant(10, 3, "CARROT_SEED").isOk());
        app.state().avatar().restoreFrom(new AvatarSnapshot(7, 7, Direction.NORTH));
        GameScreenPresenter presenter = presenter(app);
        tools.select(Tool.WATER);

        presenter.onUseTool();

        assertTrue(app.controller.inspect(7, 6).value().watered(), "the crop in front was watered");
        assertFalse(app.controller.inspect(10, 3).value().watered());
        presenter.onPlotClicked(10, 3);
        assertTrue(app.controller.inspect(10, 3).value().watered(), "a click needs no closeness");

        presenter.onUseTool();
        assertEquals("El cultivo ya está regado hoy.", messages.get(messages.size() - 1), "the same rules as always");
    }

    @Test
    void HU45_ca3_lookingOutOfTheFarmSpaceSaysThereIsNothingInFront() {
        TestApp app = TestApp.startedGame();
        app.state().avatar().restoreFrom(new AvatarSnapshot(12, 0, Direction.NORTH));
        tools.select(Tool.WATER);

        presenter(app).onUseTool();

        assertEquals(Texts.NOTHING_IN_FRONT, messages.get(messages.size() - 1));
    }

    @Test
    void HU45_ca2_theArrowsWalkAndABlockedStepShowsItsMessage() {
        TestApp app = TestApp.startedGame();
        GameScreenPresenter presenter = presenter(app);

        assertTrue(presenter.onMove("EAST"));
        assertEquals("", messages.get(messages.size() - 1));
        app.state().avatar().restoreFrom(new AvatarSnapshot(7, 8, Direction.NORTH));
        assertFalse(presenter.onMove("SOUTH"));
        assertFalse(presenter.onMove("SOUTH"));

        assertEquals("No puedes pasar por ahí.", messages.get(messages.size() - 1));
        assertEquals("SOUTH", app.controller.map().value().avatar().facing());
    }

    @Test
    void HU45_ca5_aStepPlaysThreeFramesAndAtRestOnlyOneIsShown() {
        VisualEffects fx = new VisualEffects(now::get, new Random(1));
        fx.onMap(field(new AvatarView(7, 7, "SOUTH")));
        assertFalse(fx.isWalking());
        assertEquals(0, fx.avatarFrame());
        assertNull(fx.avatarFrom());
        assertFalse(fx.needsFrames(), "nothing moves at rest");

        fx.onMap(field(new AvatarView(8, 7, "EAST")));

        assertTrue(fx.isWalking());
        assertTrue(fx.needsFrames());
        assertArrayEquals(new int[] {7, 7}, fx.avatarFrom());
        Set<Integer> frames = new HashSet<>();
        long start = now.get();
        for (long t = 0; t < VisualEffects.STEP_MS; t += 10) {
            now.set(start + t);
            frames.add(fx.avatarFrame());
        }
        assertEquals(Set.of(0, 1, 2), frames);
        now.set(start + VisualEffects.STEP_MS);
        assertFalse(fx.isWalking());
        assertEquals(0, fx.avatarFrame());
        assertEquals(1.0, fx.avatarProgress());
        assertFalse(fx.needsFrames());

        fx.onMap(field(new AvatarView(2, 2, "EAST")));
        assertFalse(fx.isWalking(), "a jump (a loaded game) is not a walk");
        fx.onMap(field(new AvatarView(2, 3, "SOUTH")));
        fx.setEnabled(false);
        assertFalse(fx.isWalking(), "with the effects off nothing moves");
        assertEquals(0, fx.avatarFrame());
    }

    @Test
    void HU45_ca5_theAvatarIsDrawnAndItsWalkChangesThePictureButNotTheGame() {
        TestApp app = TestApp.startedGame();
        AvatarView before = app.controller.map().value().avatar();
        for (AssetManager assets : List.of(AssetManager.fromClasspath(), AssetManager.vectorOnly())) {
            MapRenderer renderer = new MapRenderer(assets);
            assertFalse(Arrays.equals(paintCell(renderer, field(null), null, 7, 7),
                    paintCell(renderer, field(new AvatarView(7, 7, "SOUTH")), null, 7, 7)), "the avatar is drawn");
        }
        MapRenderer renderer = new MapRenderer();
        assertFalse(Arrays.equals(paintCell(renderer, field(new AvatarView(7, 7, "SOUTH")), null, 7, 7),
                paintCell(renderer, field(new AvatarView(7, 7, "EAST")), null, 7, 7)), "it looks where it faces");

        VisualEffects fx = new VisualEffects(now::get, new Random(1));
        fx.onWorld("SPRING", "SUNNY");
        fx.onMap(field(new AvatarView(7, 7, "EAST")));
        fx.onMap(field(new AvatarView(8, 7, "EAST")));
        long start = now.get();
        int[] leaving = paintCell(renderer, field(new AvatarView(8, 7, "EAST")), fx, 8, 7);
        now.set(start + VisualEffects.STEP_MS * 2 / 3);
        int[] arriving = paintCell(renderer, field(new AvatarView(8, 7, "EAST")), fx, 8, 7);
        now.set(start + VisualEffects.STEP_MS * 5);
        int[] rest = paintCell(renderer, field(new AvatarView(8, 7, "EAST")), fx, 8, 7);
        now.set(start + VisualEffects.STEP_MS * 9);

        assertFalse(Arrays.equals(leaving, arriving), "it moves across the plot");
        assertTrue(Arrays.equals(rest, paintCell(renderer, field(new AvatarView(8, 7, "EAST")), fx, 8, 7)),
                "at rest the picture stays still");
        assertEquals(before, app.controller.map().value().avatar(), "drawing never touches the game");
    }
}
