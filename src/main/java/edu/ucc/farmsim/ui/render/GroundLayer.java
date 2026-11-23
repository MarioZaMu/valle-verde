package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.ui.assets.AssetManager;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The ground of the farm and of the land around it, painted at the size of the art (32 pixels a tile) as one
 * continuous image: grass with a few variants chosen by position, tilled soil, the path of the valley and water
 * (houses stand on grass). Where soil, path or water meets grass, the grass grows a ragged pixel edge over it and casts a short shadow, so the plots read as one
 * field and one pond instead of a grid of squares. The image only changes when a terrain, a watered plot, the
 * season or the frame of the water changes, so it is built once and reused frame after frame.
 *
 * <p>Stories: HU-35, HU-44, HU-48, HU-50.
 */
final class GroundLayer {

    /** Frames of the water animation. */
    static final int WATER_FRAMES = 3;

    private static final int T = MapGeometry.TILE;
    private static final int MAX_ENTRIES = 4;
    private static final double SOIL_SHADOW = 0.70;
    private static final double BANK_SHADOW = 0.72;
    private static final int FOAM = 0xFF8EC6EE;

    private final AssetManager assets;
    private final Map<String, BufferedImage> cache = new LinkedHashMap<>(16, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private int paints;

    GroundLayer(AssetManager assets) {
        this.assets = assets;
    }

    /** True when every ground sprite of the season exists, so the continuous ground can be painted. */
    boolean available(String season) {
        return assets.sprite(grassName(season, 0)).isPresent() && assets.sprite("terrain/SOIL.png").isPresent()
                && assets.sprite("terrain/SOIL_WATERED.png").isPresent() && assets.sprite("terrain/WATER.png").isPresent();
    }

    /**
     * The ground of the tiles from {@code (col0, row0)} (inclusive) to {@code (col1, row1)} (exclusive), where the
     * tiles outside the map are grass.
     */
    BufferedImage image(MapView map, String season, int frame, int col0, int row0, int col1, int row1) {
        StringBuilder key = new StringBuilder(map.plots().size() + 40);
        key.append(season).append('|').append(frame).append('|').append(col0).append(',').append(row0).append(',')
                .append(col1).append(',').append(row1).append('|').append(map.cols()).append('|');
        for (PlotView plot : map.plots()) {
            key.append(plot.terrain().charAt(0)).append(plot.watered() ? '1' : '0');
        }
        return cache.computeIfAbsent(key.toString(), k -> paint(map, season, frame, col0, row0, col1, row1));
    }

    /** How many times the ground was really painted (not taken from the cache). */
    int paints() {
        return paints;
    }

    private BufferedImage paint(MapView map, String season, int frame, int col0, int row0, int col1, int row1) {
        paints++;
        int cols = col1 - col0;
        int rows = row1 - row0;
        String[] terrain = terrains(map);
        boolean[] watered = watered(map);
        BufferedImage out = new BufferedImage(cols * T, rows * T, BufferedImage.TYPE_INT_ARGB);
        int[] tile = new int[T * T];
        int[] grass = new int[T * T];
        for (int row = row0; row < row1; row++) {
            for (int col = col0; col < col1; col++) {
                String here = terrainAt(terrain, map, col, row);
                BufferedImage grassTile = sprite(grassName(season, variant(col, row))).orElseGet(
                        () -> sprite(grassName(season, 0)).orElseThrow());
                grassTile.getRGB(0, 0, T, T, grass, 0, T);
                if ("SOIL".equals(here) || "WATER".equals(here) || "PATH".equals(here)) {
                    String name = "WATER".equals(here) ? waterName(frame)
                            : "PATH".equals(here) ? pathName()
                            : watered[row * map.cols() + col] ? "terrain/SOIL_WATERED.png" : "terrain/SOIL.png";
                    BufferedImage base = sprite(name).orElseGet(() -> sprite("terrain/WATER.png").orElseThrow());
                    base.getRGB(0, 0, T, T, tile, 0, T);
                    edges(tile, grass, here, terrain, map, col, row);
                } else {
                    System.arraycopy(grass, 0, tile, 0, tile.length);
                }
                out.setRGB((col - col0) * T, (row - row0) * T, T, T, tile, 0, T);
            }
        }
        return out;
    }

    /** Grass grows over the sides of soil and water that touch something else, and casts a short shadow. */
    private static void edges(int[] tile, int[] grass, String here, String[] terrain, MapView map, int col, int row) {
        boolean water = "WATER".equals(here);
        int[][] sides = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int side = 0; side < sides.length; side++) {
            String next = terrainAt(terrain, map, col + sides[side][0], row + sides[side][1]);
            if (here.equals(next)) {
                continue;
            }
            for (int along = 0; along < T; along++) {
                int global = side < 2 ? col * T + along : row * T + along;
                int depth = fringe(global, side);
                for (int k = 0; k < depth + 3; k++) {
                    int x = side < 2 ? along : (side == 2 ? k : T - 1 - k);
                    int y = side < 2 ? (side == 0 ? k : T - 1 - k) : along;
                    int i = y * T + x;
                    if (k < depth) {
                        tile[i] = grass[i];
                    } else if (side == 0 && k < depth + (water ? 3 : 2)) {
                        tile[i] = darken(tile[i], water ? BANK_SHADOW : SOIL_SHADOW);
                    } else if (k == depth) {
                        tile[i] = water ? FOAM : darken(tile[i], 0.82);
                    }
                }
            }
        }
    }

    /** How many pixels the grass reaches over the edge at that point; ragged but continuous across tiles. */
    private static int fringe(int global, int side) {
        int v = Math.floorMod(global * 37 + side * 11 + (global / 5) * 3, 9);
        return v < 2 ? 4 : (v < 5 ? 3 : 2);
    }

    private static int darken(int argb, double k) {
        int r = (int) (((argb >> 16) & 0xFF) * k);
        int g = (int) (((argb >> 8) & 0xFF) * k);
        int b = (int) ((argb & 0xFF) * k);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Which of the four grass tiles covers a place: mostly the plain one, sometimes tufts, flowers or pebbles. */
    static int variant(int col, int row) {
        int h = Math.floorMod(col * 73_856_093 ^ row * 19_349_663, 20);
        return h < 12 ? 0 : (h < 16 ? 1 : (h < 18 ? 2 : 3));
    }

    static String grassName(String season, int variant) {
        return "terrain/GRASS_" + season + (variant == 0 ? "" : "_" + variant) + ".png";
    }

    /** The trodden earth of the path, or plain soil when the pack has no path tile. */
    private String pathName() {
        return sprite("terrain/PATH.png").isPresent() ? "terrain/PATH.png" : "terrain/SOIL.png";
    }

    static String waterName(int frame) {
        return frame == 0 ? "terrain/WATER.png" : "terrain/WATER_" + frame + ".png";
    }

    private Optional<BufferedImage> sprite(String name) {
        return assets.sprite(name);
    }

    private static String[] terrains(MapView map) {
        String[] all = new String[map.cols() * map.rows()];
        for (PlotView plot : map.plots()) {
            String t = plot.terrain();
            all[plot.row() * map.cols() + plot.col()] = "OBSTACLE".equals(t) || "HOUSE".equals(t) ? "GRASS" : t;
        }
        return all;
    }

    private static boolean[] watered(MapView map) {
        boolean[] all = new boolean[map.cols() * map.rows()];
        for (PlotView plot : map.plots()) {
            all[plot.row() * map.cols() + plot.col()] = plot.watered();
        }
        return all;
    }

    private static String terrainAt(String[] terrain, MapView map, int col, int row) {
        if (col < 0 || row < 0 || col >= map.cols() || row >= map.rows()) {
            return "GRASS";
        }
        String t = terrain[row * map.cols() + col];
        return t == null ? "GRASS" : t;
    }
}
