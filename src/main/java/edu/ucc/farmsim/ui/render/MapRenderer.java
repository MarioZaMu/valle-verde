package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.application.dto.AnimalView;
import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.HouseView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.theme.Palette;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Dibuja el mapa de la granja con Java2D, de una de dos maneras.
 *
 * <p>Con el paquete de pixel art (HU-44, HU-48) la vista es un trozo de campo visto desde arriba a tres cuartos: un
 * suelo continuo donde la tierra, el camino y el agua se funden con el pasto, bosques alrededor del valle, y árboles,
 * casas, edificios, cultivos y animales que se paran sobre su baldosa, se elevan sobre ella y se dibujan de atrás hacia
 * adelante, cada uno con su sombra. La granja queda al oeste y la aldea con sus casas al este (HU-50). Lo que tapa un
 * cultivo o la parcela bajo el cursor se vuelve translúcido. Con la vida ambiental activada, el agua ondea, los árboles
 * se mecen, los animales deambulan y nubes, mariposas u hojas cruzan los campos. El avatar está entre ellos y camina de
 * parcela en parcela con tres cuadros (HU-45), y las personas del valle están sobre sus parcelas con un globo que
 * muestra lo que pide su encargo mientras no está hecho (HU-46, HU-47). Las personas miden una parcela de ancho y dos
 * de alto: sus pies están en su parcela y su cabeza se eleva a la parcela de arriba (HU-50).
 *
 * <p>Sin el paquete, cada terreno y cada etapa de cultivo se dibuja con formas vectoriales, de modo que el juego no
 * necesita ninguna imagen (HU-42); un solo sprite que falte también recurre a su forma vectorial. Con unos {@link
 * VisualEffects} pinta la paleta de la estación, el clima, el tinte de noche y las animaciones (HU-40, HU-41). Funciona
 * sobre cualquier {@link Graphics2D}, incluido el de una imagen, así que se prueba sin pantalla.
 *
 * <p>Stories: HU-18, HU-19, HU-25, HU-35, HU-40, HU-41, HU-42, HU-44, HU-45, HU-46, HU-47, HU-48, HU-50.
 */
public final class MapRenderer {

    private static final int T = MapGeometry.TILE;
    private static final float STEM = 0.30f;
    private static final float LEAF_W = 0.22f;
    private static final float LEAF_H = 0.12f;
    private static final float FRUIT = 0.16f;
    private static final int NIGHT_MAX_ALPHA = 150;
    private static final float BEHIND_ALPHA = 0.5f;
    private static final int WATER_FRAME_MS = 450;
    private static final double TREE_TRUNK_FROM = 44.0 / 64.0;
    private static final Color NIGHT = new Color(10, 20, 60);
    private static final Color SUN = new Color(0xFFC21A);
    private static final Color SNOWFLAKE = new Color(0xFFFFFF);
    private static final Color RAIN = new Color(0xBFD9FF);
    private static final Color SHADOW = new Color(0, 0, 0, 56);
    private static final Color CURSOR = new Color(0xFFFFFF);
    private static final Color CURSOR_EDGE = new Color(0x3B2412);
    private static final Color CURSOR_FILL = new Color(255, 255, 255, 48);
    private static final Color BUBBLE = new Color(0xFFF8E7);
    private static final Color AVATAR_SHIRT = new Color(0x3F7FBF);
    private static final Color AVATAR_SKIN = new Color(0xF2C79B);
    private static final Color AVATAR_HAT = new Color(0xC8963E);
    private static final Color AVATAR_EYE = new Color(0x2A1A10);
    private static final Color NPC_MARTA = new Color(0xC0504D);
    private static final Color NPC_TOMAS = new Color(0x6B8E23);
    private static final Color NPC_LUCIA = new Color(0xF2F2F2);
    private static final Color NPC_INES = new Color(0x8E5BB5);
    private static final Color PANTS = new Color(0x4A3B2A);
    private static final Color PATH = new Color(0xC9A66B);
    private static final Color WALL = new Color(0xE8D7B0);
    private static final Color ROOF = new Color(0xA0442E);
    private static final Color DOOR = new Color(0x6B4226);
    private static final int GROUND_COPIES = 4;
    /** Baldosas de bosque pintadas alrededor del valle; más solo cuando la ventana muestra más que eso fuera de él. */
    private static final int GROUND_EDGE = 8;
    /** Baldosas más allá del borde de la ventana, para los árboles que lo sobrepasan. */
    private static final int GROUND_SPILL = 2;

    private final AssetManager assets;
    private final PixelScaler scaler = new PixelScaler();
    private final PixelScaler groundScaler = new PixelScaler(GROUND_COPIES);
    private final GroundLayer ground;

    /** Algo que está en el mapa, dibujado en orden de sus pies (cuanto más abajo, más tarde). */
    private record Item(double depth, int order, Runnable paint) {
    }

    /** Crea un renderizador que busca sprites en la carpeta {@code assets} del classpath. */
    public MapRenderer() {
        this(AssetManager.shared());
    }

    /**
     * Crea un renderizador.
     *
     * @param assets de donde vienen los sprites
     */
    public MapRenderer(AssetManager assets) {
        this.assets = assets;
        this.ground = new GroundLayer(assets);
    }

    /**
     * Pinta todo el mapa en el área dada, sin efectos.
     *
     * @param g      dónde pintar
     * @param map    el mapa que se dibuja
     * @param width  ancho del área
     * @param height alto del área
     * @param hover  parcela bajo el cursor, o {@code null}
     */
    public void render(Graphics2D g, MapView map, int width, int height, CellPos hover) {
        render(g, map, width, height, hover, null);
    }

    /**
     * Pinta todo el mapa en el área dada.
     *
     * @param g       dónde pintar
     * @param map     el mapa que se dibuja
     * @param width   ancho del área
     * @param height  alto del área
     * @param hover   parcela bajo el cursor, o {@code null}
     * @param effects la estación, el clima y las animaciones que se muestran, o {@code null} para el aspecto simple
     */
    public void render(Graphics2D g, MapView map, int width, int height, CellPos hover, VisualEffects effects) {
        render(g, map, MapGeometry.fit(map.cols(), map.rows(), width, height), width, height, hover, effects);
    }

    /**
     * Pinta todo el mapa con la colocación de parcelas dada; la tierra alrededor llena el resto.
     *
     * @param g        dónde pintar
     * @param map      el mapa que se dibuja
     * @param geometry dónde va cada parcela
     * @param width    ancho del área
     * @param height   alto del área
     * @param hover    parcela bajo el cursor, o {@code null}
     * @param effects  la estación, el clima y las animaciones que se muestran, o {@code null} para el aspecto simple
     */
    public void render(Graphics2D g, MapView map, MapGeometry geometry, int width, int height, CellPos hover,
                       VisualEffects effects) {
        VisualEffects fx = effects != null && effects.isEnabled() ? effects : null;
        boolean ambient = fx != null && fx.isAmbient();
        long time = fx == null ? 0 : fx.time();
        String season = fx == null ? "SPRING" : fx.season();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (ground.available(season)) {
            paintPixelArt(g, map, geometry, width, height, hover, fx, season, ambient, time);
        } else {
            paintVector(g, map, geometry, width, height, fx, season);
        }
        if (hover != null && hover.col() >= 0 && hover.row() >= 0 && hover.col() < map.cols() && hover.row() < map.rows()) {
            drawCursor(g, geometry.cellBounds(hover.col(), hover.row()), ambient, time);
        }
        if (fx != null) {
            drawAmbience(g, fx, geometry, width, height, ambient);
        }
    }

    /** Cuántas veces se pintó el suelo; una cámara en movimiento lo reutiliza (HU-50 · CA-5). */
    public int groundPaints() {
        return ground.paints();
    }

    /** Baldosas de suelo que se pintan más allá de un lado del valle, que muestra {@code outside} píxeles más allá. */
    private static int edge(int outside, int cell) {
        return Math.max(GROUND_EDGE, ceilDiv(Math.max(0, outside), cell) + GROUND_SPILL);
    }

        // ------------------------------------------------------------------ pixel art

    private void paintPixelArt(Graphics2D g, MapView map, MapGeometry geo, int width, int height, CellPos hover,
                               VisualEffects fx, String season, boolean ambient, long time) {
        int s = geo.cellSize();
                // El suelo de todo el valle más un borde de bosque, sea lo que sea que muestre la cámara: caminar solo lo
        // desliza. Unos límites que seguían a la cámara lo pintaban y escalaban otra vez en cada fila y hacían que la
        // caminata diera tirones.
        int col0 = -edge(geo.originX(), s);
        int row0 = -edge(geo.originY(), s);
        int col1 = map.cols() + edge(width - geo.originX() - map.cols() * s, s);
        int row1 = map.rows() + edge(height - geo.originY() - map.rows() * s, s);
        int frame = ambient ? (int) ((time / WATER_FRAME_MS) % GroundLayer.WATER_FRAMES) : 0;
        BufferedImage art = ground.image(map, season, frame, col0, row0, col1, row1);
        groundScaler.draw(g, art, geo.originX() + col0 * s, geo.originY() + row0 * s, (col1 - col0) * s, (row1 - row0) * s);

        Set<Long> hidden = new HashSet<>();
        for (PlotView plot : map.plots()) {
            if (plot.crop() != null) {
                hidden.add(key(plot.col(), plot.row()));
            }
        }
        if (hover != null) {
            hidden.add(key(hover.col(), hover.row()));
        }
        if (map.avatar() != null) {
            hidden.add(key(map.avatar().col(), map.avatar().row()));
        }
        for (NpcView npc : npcs(map)) {
            hidden.add(key(npc.col(), npc.row()));
        }
        List<Rectangle> shadows = new ArrayList<>();
        List<Item> items = new ArrayList<>();
        List<Runnable> above = new ArrayList<>();
        surroundings(g, map, geo, season, ambient, time, col0, row0, col1, row1, shadows, items);
        for (PlotView plot : map.plots()) {
            Rectangle cell = geo.cellBounds(plot.col(), plot.row());
            if ("OBSTACLE".equals(plot.terrain())) {
                obstacle(g, plot.col(), plot.row(), cell, season, ambient, time, hidden, shadows, items);
            }
            if (plot.crop() != null) {
                double depth = cell.y + cell.height - (plot.sheltered() ? 0.5 : 0.25);
                items.add(new Item(depth, items.size(), () -> drawCropArt(g, plot, cell, fx)));
            }
        }
        for (BuildingView building : map.buildings()) {
            building(g, building, geo, fx, ambient, time, hidden, shadows, items, above);
        }
        for (HouseView house : houses(map)) {
            house(g, house, geo, hidden, shadows, items);
        }
        if (map.avatar() != null) {
            avatar(g, map.avatar(), geo, fx, shadows, items);
        }
        for (NpcView npc : npcs(map)) {
            npc(g, npc, geo, ambient, time, shadows, items, above);
        }
        Composite saved = g.getComposite();
        g.setColor(SHADOW);
        for (Rectangle shadow : shadows) {
            g.fillOval(shadow.x, shadow.y, shadow.width, shadow.height);
        }
        g.setComposite(saved);
        items.sort(Comparator.comparingDouble(Item::depth).thenComparingInt(Item::order));
        for (Item item : items) {
            item.paint().run();
        }
        for (Runnable bubble : above) {
            bubble.run();
        }
    }

    /** Los bosques más allá de los bordes del valle, sobre las baldosas fuera del mapa. */
    private void surroundings(Graphics2D g, MapView map, MapGeometry geo, String season, boolean ambient, long time,
                              int col0, int row0, int col1, int row1, List<Rectangle> shadows, List<Item> items) {
        int s = geo.cellSize();
        for (int row = row0; row < row1; row++) {
            for (int col = col0; col < col1; col++) {
                int dx = col < 0 ? -col : Math.max(0, col - map.cols() + 1);
                int dy = row < 0 ? -row : Math.max(0, row - map.rows() + 1);
                int distance = Math.max(dx, dy);
                if (distance == 0) {
                    continue;
                }
                Rectangle cell = geo.cellBounds(col, row);
                int h = hash(col, row) % 100;
                if (h < 42) {
                    String kind = h % 3 == 0 ? "PINE" : "TREE";
                    tree(g, "terrain/" + kind + "_" + season + ".png", col, row, cell, ambient, time, false, shadows, items);
                } else if (h < 52) {
                    assets.sprite("terrain/BUSH_" + season + ".png").ifPresent(img -> {
                        shadows.add(new Rectangle(cell.x + s / 8, cell.y + s * 3 / 4, s * 3 / 4, s / 4));
                        items.add(new Item(cell.y + cell.height - 1, items.size(), () -> scaler.draw(g, img, cell.x, cell.y, s, s)));
                    });
                }
            }
        }
    }

    private void obstacle(Graphics2D g, int col, int row, Rectangle cell, String season, boolean ambient, long time,
                          Set<Long> hidden, List<Rectangle> shadows, List<Item> items) {
        int h = hash(col, row);
        if (h % 4 == 0) {
            Optional<BufferedImage> rock = assets.sprite("terrain/OBSTACLE.png");
            if (rock.isPresent()) {
                shadows.add(new Rectangle(cell.x + cell.width / 10, cell.y + cell.height * 3 / 4, cell.width * 4 / 5, cell.height / 4));
                items.add(new Item(cell.y + cell.height - 1, items.size(),
                        () -> scaler.draw(g, rock.get(), cell.x, cell.y, cell.width, cell.height)));
                return;
            }
        }
        String kind = h % 3 == 0 ? "PINE" : "TREE";
        boolean behind = hidden.contains(key(col, row - 1)) || hidden.contains(key(col - 1, row - 1))
                || hidden.contains(key(col + 1, row - 1));
        if (!tree(g, "terrain/" + kind + "_" + season + ".png", col, row, cell, ambient, time, behind, shadows, items)) {
            items.add(new Item(cell.y + cell.height - 1, items.size(), () -> drawObstacleVector(g, cell)));
        }
    }

    private boolean tree(Graphics2D g, String name, int col, int row, Rectangle cell, boolean ambient, long time,
                         boolean behind, List<Rectangle> shadows, List<Item> items) {
        Optional<BufferedImage> sprite = assets.sprite(name);
        if (sprite.isEmpty()) {
            return false;
        }
        int s = cell.width;
        shadows.add(new Rectangle(cell.x + s / 20, cell.y + s * 3 / 4, s * 9 / 10, s / 3));
        double sway = ambient ? Math.sin(time / 1100.0 + col * 0.9 + row * 0.5) * s / 28.0 : 0;
        items.add(new Item(cell.y + cell.height - 1, items.size(), () -> drawSplit(g, sprite.get(),
                cell.x - s / 2, cell.y - s, 2 * s, 2 * s, TREE_TRUNK_FROM, (int) Math.round(sway), behind ? BEHIND_ALPHA : 1f)));
        return true;
    }

    /**
     * Dibuja un sprite alto cuya parte superior (la copa) puede inclinarse hacia un lado mientras la parte inferior
     * queda quieta.
     */
    private void drawSplit(Graphics2D g, BufferedImage img, int x, int y, int w, int h, double split, int lean, float alpha) {
        Composite saved = g.getComposite();
        if (alpha < 1f) {
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        }
        if (lean == 0) {
            scaler.draw(g, img, x, y, w, h);
        } else {
            double device = PixelScaler.deviceScale(g);
            BufferedImage ready = scaler.scaled(img, (int) Math.round(w * device), (int) Math.round(h * device));
            int cut = (int) Math.round(h * split);
            int cutDevice = (int) Math.round(ready.getHeight() * split);
            g.drawImage(ready, x + lean, y, x + lean + w, y + cut, 0, 0, ready.getWidth(), cutDevice, null);
            g.drawImage(ready, x, y + cut, x + w, y + h, 0, cutDevice, ready.getWidth(), ready.getHeight(), null);
        }
        g.setComposite(saved);
    }

    private void building(Graphics2D g, BuildingView building, MapGeometry geo, VisualEffects fx, boolean ambient,
                          long time, Set<Long> hidden, List<Rectangle> shadows, List<Item> items, List<Runnable> above) {
        Rectangle foot = geo.cellBounds(building.col(), building.row())
                .union(geo.cellBounds(building.col() + building.width() - 1, building.row() + building.height() - 1));
        int s = geo.cellSize();
        int bottom = foot.y + foot.height;
        Optional<BufferedImage> sheet = assets.sprite("buildings/" + building.type() + ".png");
        if (sheet.isEmpty()) {
            items.add(new Item(bottom, items.size(), () -> drawBuilding(g, building, geo, fx)));
            return;
        }
        BufferedImage img = sheet.get();
        int drawH = img.getHeight() * s / T;
        int drawW = img.getWidth() * s / T;
        int top = bottom - drawH;
        boolean behind = false;
        for (int col = building.col(); col < building.col() + building.width(); col++) {
            behind |= hidden.contains(key(col, building.row() - 1));
        }
        float alpha = behind ? BEHIND_ALPHA : 1f;
        shadows.add(new Rectangle(foot.x - s / 6, bottom - s / 3, foot.width + s / 3, s / 2));
        items.add(new Item(bottom, items.size(), () -> {
            Composite saved = g.getComposite();
            if (alpha < 1f) {
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            }
            scaler.draw(g, img, foot.x, top, drawW, drawH);
            g.setComposite(saved);
        }));
        int slots = Math.max(1, building.capacity());
        for (AnimalView animal : building.animals()) {
            int frame = fx == null ? 0 : fx.animalFrame(animal.index());
            int lane = foot.width / slots;
            double phase = time / 1700.0 + animal.index() * 1.9;
            int roam = ambient ? (int) Math.round(Math.sin(phase) * Math.max(0, lane - s) / 2.0) : 0;
            boolean left = ambient && Math.cos(phase) < 0;
            int x = foot.x + animal.index() * lane + (lane - s) / 2 + roam;
            int y = bottom - s + s / 16;
            shadows.add(new Rectangle(x + s / 6, bottom - s / 6, s * 2 / 3, s / 5));
            items.add(new Item(bottom + 1, items.size(), () -> {
                Optional<BufferedImage> sprite = assets.sprite("animals/" + animal.type() + "_" + frame + ".png");
                if (sprite.isPresent()) {
                    scaler.draw(g, sprite.get(), x, y, left ? -s : s, s);
                } else {
                    drawAnimalVector(g, animal, x, y, s, frame);
                }
            }));
            String need = "SICK".equals(animal.condition()) ? "items/MEDICINE.png"
                    : "HUNGRY".equals(animal.condition()) ? "items/HAY.png" : null;
            if (need != null) {
                Color fallback = "SICK".equals(animal.condition()) ? Palette.ANIMAL_SICK : Palette.ANIMAL_HUNGRY;
                above.add(() -> bubble(g, x + s / 2, y + s / 8, Math.max(12, s / 2), need, fallback, 0));
            }
        }
        if (building.pendingProducts() > 0) {
            String product = "BARN".equals(building.type()) ? "items/MILK.png" : "items/EGG.png";
            int bob = ambient ? (int) Math.round(Math.sin(time / 300.0) * s / 12.0) : 0;
            above.add(() -> bubble(g, foot.x + foot.width - s / 2, top + s / 2, Math.max(14, s * 3 / 5), product,
                    Palette.PENDING, bob));
        }
    }

    /** Una casa del valle: su sprite se levanta sobre sus parcelas y su techo se eleva una parcela sobre ellas. */
    private void house(Graphics2D g, HouseView house, MapGeometry geo, Set<Long> hidden, List<Rectangle> shadows,
                       List<Item> items) {
        Rectangle foot = footprint(geo, house);
        int s = geo.cellSize();
        int bottom = foot.y + foot.height;
        Optional<BufferedImage> sprite = assets.sprite(houseSprite(house));
        boolean behind = false;
        for (int col = house.col(); col < house.col() + house.width(); col++) {
            behind |= hidden.contains(key(col, house.row() - 1));
        }
        float alpha = behind ? BEHIND_ALPHA : 1f;
        shadows.add(new Rectangle(foot.x - s / 6, bottom - s / 3, foot.width + s / 3, s / 2));
        items.add(new Item(bottom, items.size(), () -> {
            if (sprite.isEmpty()) {
                drawHouseVector(g, foot, s);
                return;
            }
            BufferedImage img = sprite.get();
            int drawW = img.getWidth() * s / T;
            int drawH = img.getHeight() * s / T;
            Composite saved = g.getComposite();
            if (alpha < 1f) {
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            }
            scaler.draw(g, img, foot.x, bottom - drawH, drawW, drawH);
            g.setComposite(saved);
        }));
    }

    /** El sprite de una casa del valle. */
    static String houseSprite(HouseView house) {
        return "village/" + house.id() + ".png";
    }

    private static Rectangle footprint(MapGeometry geo, HouseView house) {
        return geo.cellBounds(house.col(), house.row())
                .union(geo.cellBounds(house.col() + house.width() - 1, house.row() + house.height() - 1));
    }

    /** Una casa sin sprites: paredes claras con una puerta y un techo rojo sobre su parcela superior. */
    private static void drawHouseVector(Graphics2D g, Rectangle foot, int s) {
        int roofH = Math.max(2, foot.height / 2);
        g.setColor(WALL);
        g.fillRect(foot.x, foot.y + roofH, foot.width, foot.height - roofH);
        g.setColor(ROOF);
        g.fillPolygon(new int[] {foot.x - s / 8, foot.x + foot.width + s / 8, foot.x + foot.width - s / 3, foot.x + s / 3},
                new int[] {foot.y + roofH, foot.y + roofH, foot.y, foot.y}, 4);
        g.setColor(DOOR);
        int doorW = Math.max(2, s / 2);
        g.fillRect(foot.x + (foot.width - doorW) / 2, foot.y + foot.height - s * 3 / 4, doorW, s * 3 / 4);
    }

    /** Un globo de diálogo con un objeto dentro, para lo que necesita un animal o lo que espera ser recogido. */
    private void bubble(Graphics2D g, int cx, int bottom, int size, String icon, Color fallback, int bob) {
        int x = cx - size / 2;
        int y = bottom - size - size / 4 + bob;
        int arc = Math.max(4, size / 3);
        g.setColor(CURSOR_EDGE);
        g.fillRoundRect(x - 2, y - 2, size + 4, size + 4, arc + 2, arc + 2);
        g.fillPolygon(new int[] {cx - size / 6 - 2, cx + size / 6 + 2, cx}, new int[] {y + size, y + size, y + size + size / 4 + 2}, 3);
        g.setColor(BUBBLE);
        g.fillRoundRect(x, y, size, size, arc, arc);
        g.fillPolygon(new int[] {cx - size / 6, cx + size / 6, cx}, new int[] {y + size - 1, y + size - 1, y + size + size / 4}, 3);
        Optional<BufferedImage> sprite = assets.sprite(icon);
        int inset = Math.max(1, size / 10);
        if (sprite.isPresent()) {
            scaler.draw(g, sprite.get(), x + inset, y + inset, size - 2 * inset, size - 2 * inset);
        } else {
            g.setColor(fallback);
            g.fillOval(x + inset * 2, y + inset * 2, size - 4 * inset, size - 4 * inset);
        }
    }

    private void drawCropArt(Graphics2D g, PlotView plot, Rectangle c, VisualEffects fx) {
        float scale = fx == null ? 1f : fx.cropScale(plot.col(), plot.row());
        Optional<BufferedImage> sprite = assets.sprite("crops/" + plot.crop() + "_" + plot.stage() + ".png");
        if (sprite.isEmpty()) {
            drawCrop(g, plot, c, fx);
            return;
        }
        int size = Math.max(1, Math.round(c.width * scale));
        scaler.draw(g, sprite.get(), c.x + (c.width - size) / 2, c.y + c.height - size, size, size);
    }

    /** Corchetes de píxeles en las esquinas de la parcela bajo el cursor, sobre un velo claro; respiran lentamente. */
    private static void drawCursor(Graphics2D g, Rectangle cell, boolean ambient, long time) {
        int pulse = ambient ? (int) Math.round((Math.sin(time / 220.0) + 1) * cell.width / 48.0) : 0;
        int x = cell.x - pulse;
        int y = cell.y - pulse;
        int w = cell.width + 2 * pulse;
        int h = cell.height + 2 * pulse;
        g.setColor(CURSOR_FILL);
        g.fillRect(cell.x, cell.y, cell.width, cell.height);
        int len = Math.max(4, cell.width / 4);
        int t = Math.max(2, cell.width / 16);
        for (int pass = 0; pass < 2; pass++) {
            int o = pass == 0 ? 1 : 0;
            g.setColor(pass == 0 ? CURSOR_EDGE : CURSOR);
            int[][] corners = {{x, y, 1, 1}, {x + w, y, -1, 1}, {x, y + h, 1, -1}, {x + w, y + h, -1, -1}};
            for (int[] k : corners) {
                int cx = k[0];
                int cy = k[1];
                int hx = k[2] > 0 ? cx : cx - len;
                int vy = k[3] > 0 ? cy : cy - len;
                int tx = k[2] > 0 ? cx : cx - t;
                int ty = k[3] > 0 ? cy : cy - t;
                g.fillRect(hx - o, ty - o, len + 2 * o, t + 2 * o);
                g.fillRect(tx - o, vy - o, t + 2 * o, len + 2 * o);
            }
        }
    }

    private static long key(int col, int row) {
        return ((long) row << 32) ^ (col & 0xFFFFFFFFL);
    }

    private static int hash(int col, int row) {
        int h = col * 374_761_393 + row * 668_265_263;
        h = (h ^ (h >>> 13)) * 1_274_126_177;
        return (h ^ (h >>> 16)) & 0x7FFFFFFF;
    }

    private static int ceilDiv(int a, int b) {
        return a <= 0 ? 0 : (a + b - 1) / b;
    }

        // ------------------------------------------------------------------ respaldo vectorial

    private void paintVector(Graphics2D g, MapView map, MapGeometry geometry, int width, int height, VisualEffects fx,
                             String season) {
        g.setColor(Palette.BACKGROUND);
        g.fillRect(0, 0, width, height);
        Color grass = fx == null ? Palette.GRASS : SeasonPalette.grass(fx.season());
        for (PlotView plot : map.plots()) {
            Rectangle cell = geometry.cellBounds(plot.col(), plot.row());
            drawTerrain(g, plot, cell, grass, season);
            if (plot.crop() != null && !plot.sheltered()) {
                drawCrop(g, plot, cell, fx);
            }
        }
        for (BuildingView building : map.buildings()) {
            drawBuilding(g, building, geometry, fx);
        }
        for (HouseView house : houses(map)) {
            drawHouseVector(g, footprint(geometry, house), geometry.cellSize());
        }
        for (PlotView plot : map.plots()) {
            if (plot.crop() != null && plot.sheltered()) {
                drawCrop(g, plot, geometry.cellBounds(plot.col(), plot.row()), fx);
            }
        }
        for (NpcView npc : npcs(map)) {
            Rectangle cell = geometry.cellBounds(npc.col(), npc.row());
            drawPersonVector(g, cell, npcShirt(npc.id()), "SOUTH");
            if (!npc.errandDone()) {
                bubble(g, cell.x + cell.width / 2, cell.y - cell.height + cell.height / 8, Math.max(12, cell.width / 2),
                        errandIcon(npc), Palette.PENDING, 0);
            }
        }
        if (map.avatar() != null) {
            drawAvatarVector(g, avatarCell(map.avatar(), geometry, fx), map.avatar().facing());
        }
    }

        // ------------------------------------------------------------------ personas del valle

    private static List<NpcView> npcs(MapView map) {
        return map.npcs() == null ? List.of() : map.npcs();
    }

    private static List<HouseView> houses(MapView map) {
        return map.houses() == null ? List.of() : map.houses();
    }

    /** La sombra a los pies de una persona, dentro de su propia parcela. */
    private static Rectangle feet(Rectangle cell) {
        int s = cell.width;
        return new Rectangle(cell.x + s / 5, cell.y + s - s / 5, s * 3 / 5, s / 5);
    }

    /** Dibuja un sprite de persona de una parcela de ancho, parado sobre {@code cell} y tan alto como dice su arte. */
    private void standing(Graphics2D g, BufferedImage sprite, Rectangle cell, int lift) {
        int s = cell.width;
        int h = sprite.getHeight() * s / sprite.getWidth();
        scaler.draw(g, sprite, cell.x, cell.y + s - h - lift, s, h);
    }

    private static String errandIcon(NpcView npc) {
        return "items/" + npc.errandItem() + ".png";
    }

    private static Color npcShirt(String id) {
        return switch (id) {
            case "MARTA" -> NPC_MARTA;
            case "TOMAS" -> NPC_TOMAS;
            case "LUCIA" -> NPC_LUCIA;
            default -> NPC_INES;
        };
    }

    private void npc(Graphics2D g, NpcView npc, MapGeometry geo, boolean ambient, long time, List<Rectangle> shadows,
                     List<Item> items, List<Runnable> above) {
        Rectangle cell = geo.cellBounds(npc.col(), npc.row());
        int s = cell.width;
        shadows.add(feet(cell));
        Optional<BufferedImage> sprite = assets.sprite("characters/" + npc.id() + ".png");
        int breathe = ambient && (time / 600 + npc.col()) % 2 == 0 ? Math.max(1, s / 32) : 0;
        items.add(new Item(cell.y + cell.height - 0.1, items.size(), () -> {
            if (sprite.isPresent()) {
                standing(g, sprite.get(), cell, breathe);
            } else {
                drawPersonVector(g, cell, npcShirt(npc.id()), "SOUTH");
            }
        }));
        if (!npc.errandDone()) {
            int bob = ambient ? (int) Math.round(Math.sin(time / 350.0 + npc.col()) * s / 12.0) : 0;
            above.add(() -> bubble(g, cell.x + s / 2, cell.y - s + s / 8, Math.max(14, s / 2), errandIcon(npc),
                    Palette.PENDING, bob));
        }
    }

        // ------------------------------------------------------------------ avatar

    /** El sprite del avatar mirando hacia una dirección, en un cuadro de su caminata. */
    static String avatarSprite(String facing, int frame) {
        return "characters/AVATAR_" + facing.charAt(0) + "_" + frame + ".png";
    }

    /** Dónde se dibuja el avatar: su parcela, o en camino hacia ella mientras camina. */
    private static Rectangle avatarCell(AvatarView avatar, MapGeometry geo, VisualEffects fx) {
        Rectangle to = geo.cellBounds(avatar.col(), avatar.row());
        int[] from = fx == null ? null : fx.avatarFrom();
        if (from == null) {
            return to;
        }
        Rectangle start = geo.cellBounds(from[0], from[1]);
        double t = fx.avatarProgress();
        return new Rectangle((int) Math.round(start.x + (to.x - start.x) * t),
                (int) Math.round(start.y + (to.y - start.y) * t), to.width, to.height);
    }

    private void avatar(Graphics2D g, AvatarView avatar, MapGeometry geo, VisualEffects fx, List<Rectangle> shadows,
                        List<Item> items) {
        Rectangle cell = avatarCell(avatar, geo, fx);
        shadows.add(feet(cell));
        int frame = fx == null ? 0 : fx.avatarFrame();
        Optional<BufferedImage> sprite = assets.sprite(avatarSprite(avatar.facing(), frame));
        items.add(new Item(cell.y + cell.height - 0.1, items.size(), () -> {
            if (sprite.isPresent()) {
                standing(g, sprite.get(), cell, 0);
            } else {
                drawAvatarVector(g, cell, avatar.facing());
            }
        }));
    }

    /** El avatar sin sprites: un granjero con sombrero de paja, mirando hacia su dirección. */
    private static void drawAvatarVector(Graphics2D g, Rectangle c, String facing) {
        drawPersonVector(g, c, AVATAR_SHIRT, facing);
    }

    /**
     * Una persona dibujada con formas, de una parcela de ancho y dos de alto con los pies en {@code c}: pantalones, una
     * camisa del color dado, una cara, un sombrero de paja y ojos hacia su dirección.
     */
    private static void drawPersonVector(Graphics2D g, Rectangle c, Color shirt, String facing) {
        int s = c.width;
        int cx = c.x + s / 2;
        int top = c.y - s;
        g.setColor(PANTS);
        g.fillRect(cx - s / 5, top + s * 3 / 2, s * 2 / 5, s * 2 / 5);
        g.setColor(shirt);
        g.fillRoundRect(cx - s / 4, top + s * 19 / 20, s / 2, s * 13 / 20, s / 6, s / 6);
        g.setColor(AVATAR_SKIN);
        g.fillOval(cx - s / 4, top + s * 9 / 20, s / 2, s / 2);
        g.setColor(AVATAR_HAT);
        g.fillRect(cx - s / 3, top + s / 2, s * 2 / 3, Math.max(2, s / 12));
        g.fillOval(cx - s / 5, top + s * 3 / 10, s * 2 / 5, s / 4);
        if (!"NORTH".equals(facing)) {
            int shift = "EAST".equals(facing) ? s / 10 : ("WEST".equals(facing) ? -s / 10 : 0);
            int eye = Math.max(2, s / 14);
            int ey = top + s * 3 / 4;
            g.setColor(AVATAR_EYE);
            if (shift == 0) {
                g.fillRect(cx - s / 10 - eye / 2, ey, eye, eye);
                g.fillRect(cx + s / 10 - eye / 2, ey, eye, eye);
            } else {
                g.fillRect(cx + shift - eye / 2, ey, eye, eye);
            }
        }
    }

    private void drawAmbience(Graphics2D g, VisualEffects fx, MapGeometry geometry, int width, int height, boolean ambient) {
        Rectangle area = geometry.area();
        if (ambient) {
            AmbientPainter.paint(g, area, geometry.cellSize(), fx.time(), fx.season(), fx.weather());
        }
        Color tint = SeasonPalette.tint(fx.weather());
        if (tint != null) {
            g.setColor(tint);
            g.fillRect(ambient ? 0 : area.x, ambient ? 0 : area.y, ambient ? width : area.width, ambient ? height : area.height);
        }
        for (Particle particle : fx.particles()) {
            int x = area.x + (int) Math.round(particle.x() * area.width);
            int y = area.y + (int) Math.round(particle.y() * area.height);
            if (particle.kind() == Particle.Kind.RAIN) {
                g.setColor(RAIN);
                g.drawLine(x, y, x - 3, y + 10);
            } else {
                g.setColor(SNOWFLAKE);
                g.fillOval(x - 2, y - 2, 5, 5);
            }
        }
        drawIndicator(g, fx.weather(), area);
        if (ambient) {
            AmbientPainter.vignette(g, width, height);
        }
        float night = fx.nightAlpha();
        if (night > 0f) {
            g.setColor(new Color(NIGHT.getRed(), NIGHT.getGreen(), NIGHT.getBlue(), Math.round(night * NIGHT_MAX_ALPHA)));
            g.fillRect(0, 0, width, height);
        }
    }

    private void drawIndicator(Graphics2D g, String weather, Rectangle area) {
        int r = Math.max(8, area.height / 36);
        int cx = area.x + area.width - 2 * r;
        int cy = area.y + 2 * r;
        g.setStroke(new BasicStroke(Math.max(2f, r / 4f)));
        if ("DROUGHT".equals(weather)) {
            g.setColor(SUN);
            g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * i / 4;
                g.drawLine(cx + (int) Math.round(Math.cos(angle) * r * 1.3), cy + (int) Math.round(Math.sin(angle) * r * 1.3),
                        cx + (int) Math.round(Math.cos(angle) * r * 1.9), cy + (int) Math.round(Math.sin(angle) * r * 1.9));
            }
        } else if ("FROST".equals(weather)) {
            g.setColor(SNOWFLAKE);
            for (int i = 0; i < 3; i++) {
                double angle = Math.PI * i / 3;
                g.drawLine(cx - (int) Math.round(Math.cos(angle) * r * 1.6), cy - (int) Math.round(Math.sin(angle) * r * 1.6),
                        cx + (int) Math.round(Math.cos(angle) * r * 1.6), cy + (int) Math.round(Math.sin(angle) * r * 1.6));
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawBuilding(Graphics2D g, BuildingView building, MapGeometry geometry, VisualEffects fx) {
        Rectangle first = geometry.cellBounds(building.col(), building.row());
        Rectangle last = geometry.cellBounds(building.col() + building.width() - 1, building.row() + building.height() - 1);
        Rectangle area = first.union(last);
        int inset = Math.max(2, area.height / 16);
        Rectangle body = new Rectangle(area.x + inset, area.y + inset, area.width - 2 * inset, area.height - 2 * inset);
        Optional<BufferedImage> sheet = assets.sprite("buildings/" + building.type() + ".png");
        if (sheet.isPresent()) {
            g.drawImage(sheet.get(), area.x, area.y, area.width, area.height, null);
            drawAnimalSprites(g, building, area, fx);
            drawPending(g, building, area);
            return;
        }
        if ("GREENHOUSE".equals(building.type())) {
            g.setColor(Palette.GREENHOUSE_GLASS);
            g.fillRect(body.x, body.y, body.width, body.height);
            g.setColor(Palette.BUILDING_OUTLINE);
            for (int i = 1; i < building.width(); i++) {
                int x = body.x + body.width * i / building.width();
                g.drawLine(x, body.y, x, body.y + body.height);
            }
            for (int i = 1; i < building.height(); i++) {
                int y = body.y + body.height * i / building.height();
                g.drawLine(body.x, y, body.x + body.width, y);
            }
        } else {
            g.setColor(wallColor(building.type()));
            g.fillRect(body.x, body.y, body.width, body.height);
            int roofHeight = body.height / 3;
            g.setColor(Palette.ROOF);
            g.fillPolygon(new int[] {body.x - inset, body.x + body.width / 2, body.x + body.width + inset},
                    new int[] {body.y + roofHeight, body.y - inset, body.y + roofHeight}, 3);
        }
        g.setColor(Palette.BUILDING_OUTLINE);
        g.drawRect(body.x, body.y, body.width, body.height);
        drawAnimals(g, building, body, fx);
        drawPending(g, building, area);
    }

    private void drawPending(Graphics2D g, BuildingView building, Rectangle area) {
        if (building.pendingProducts() > 0) {
            int r = Math.max(4, area.width / 12);
            g.setColor(Palette.PENDING);
            g.fillOval(area.x + area.width - 2 * r, area.y, 2 * r, 2 * r);
            g.setColor(Palette.BUILDING_OUTLINE);
            g.drawOval(area.x + area.width - 2 * r, area.y, 2 * r, 2 * r);
        }
    }

    /**
     * Animales frente a un edificio dibujados con un sprite: un sprite del tamaño de una baldosa cada uno, con una
     * insignia si no están bien.
     */
    private void drawAnimalSprites(Graphics2D g, BuildingView building, Rectangle area, VisualEffects fx) {
        int slots = Math.max(1, building.capacity());
        int d = Math.max(8, Math.min(area.width / slots, area.height / 2));
        for (AnimalView animal : building.animals()) {
            int frame = fx == null ? 0 : fx.animalFrame(animal.index());
            Optional<BufferedImage> sprite = assets.sprite("animals/" + animal.type() + "_" + frame + ".png");
            int x = area.x + animal.index() * area.width / slots + (area.width / slots - d) / 2;
            int y = area.y + area.height - d - Math.max(1, area.height / 32);
            if (sprite.isPresent()) {
                g.drawImage(sprite.get(), x, y, d, d, null);
            } else {
                drawAnimalVector(g, animal, x, y, d, frame);
            }
            drawBadge(g, animal, x + d, y, d);
        }
    }

    private static void drawBadge(Graphics2D g, AnimalView animal, int right, int top, int d) {
        boolean sick = "SICK".equals(animal.condition());
        if (!sick && !"HUNGRY".equals(animal.condition())) {
            return;
        }
        int r = Math.max(3, d / 5);
        int x = right - 2 * r;
        g.setColor(sick ? Palette.ANIMAL_SICK : Palette.ANIMAL_HUNGRY);
        g.fillOval(x, top, 2 * r, 2 * r);
        g.setColor(Palette.BUILDING_OUTLINE);
        g.drawOval(x, top, 2 * r, 2 * r);
        if (sick) {
            g.drawLine(x + r, top + r / 2, x + r, top + 2 * r - r / 2);
            g.drawLine(x + r / 2, top + r, x + 2 * r - r / 2, top + r);
        }
    }

    private static Color wallColor(String type) {
        return switch (type) {
            case "BARN" -> Palette.BARN_WALL;
            case "WAREHOUSE" -> Palette.WAREHOUSE_WALL;
            default -> Palette.COOP_WALL;
        };
    }

    private void drawAnimals(Graphics2D g, BuildingView building, Rectangle body, VisualEffects fx) {
        int slots = Math.max(1, building.capacity());
        int d = Math.max(4, body.width / (slots + 1));
        for (AnimalView animal : building.animals()) {
            int frame = fx == null ? 0 : fx.animalFrame(animal.index());
            int x = body.x + body.width * (2 * animal.index() + 1) / (2 * slots) - d / 2;
            int y = body.y + body.height * 2 / 3 - d / 2 - bob(frame, d);
            Optional<BufferedImage> sprite = assets.sprite("animals/" + animal.type() + "_" + frame + ".png");
            if (sprite.isPresent()) {
                g.drawImage(sprite.get(), x, y, d, d, null);
                continue;
            }
            g.setColor(switch (animal.condition()) {
                case "SICK" -> Palette.ANIMAL_SICK;
                case "HUNGRY" -> Palette.ANIMAL_HUNGRY;
                default -> Palette.ANIMAL_HEALTHY;
            });
            paintVectorAnimal(g, animal, x, y, d);
        }
    }

    private static void drawAnimalVector(Graphics2D g, AnimalView animal, int x, int y, int d, int frame) {
        g.setColor(switch (animal.condition()) {
            case "SICK" -> Palette.ANIMAL_SICK;
            case "HUNGRY" -> Palette.ANIMAL_HUNGRY;
            default -> Palette.ANIMAL_HEALTHY;
        });
        paintVectorAnimal(g, animal, x, y - bob(frame, d), d);
    }

    private static void paintVectorAnimal(Graphics2D g, AnimalView animal, int x, int y, int d) {
        if ("COW".equals(animal.type())) {
            g.fillOval(x - d / 4, y, d + d / 2, d);
            g.setColor(Palette.BUILDING_OUTLINE);
            g.fillOval(x + d / 4, y + d / 4, d / 3, d / 3);
        } else {
            g.fillOval(x, y, d, d);
            g.setColor(Palette.BARN_WALL);
            g.fillOval(x + d / 3, y - d / 5, d / 3, d / 3);
        }
    }

    /** Cuántos píxeles sube el animal en cada cuadro de su saltito: abajo, arriba, a medio camino. */
    private static int bob(int frame, int d) {
        return switch (frame) {
            case 1 -> Math.max(1, d / 6);
            case 2 -> Math.max(1, d / 12);
            default -> 0;
        };
    }

    private void drawTerrain(Graphics2D g, PlotView plot, Rectangle c, Color grass, String season) {
        String name = switch (plot.terrain()) {
            case "SOIL" -> plot.watered() ? "terrain/SOIL_WATERED.png" : "terrain/SOIL.png";
            case "WATER" -> "terrain/WATER.png";
            case "OBSTACLE" -> "terrain/OBSTACLE.png";
            case "PATH" -> "terrain/PATH.png";
            default -> "terrain/GRASS_" + season + ".png";
        };
        Optional<BufferedImage> sprite = assets.sprite(name);
        if (sprite.isPresent()) {
            g.drawImage(sprite.get(), c.x, c.y, c.width, c.height, null);
            return;
        }
        switch (plot.terrain()) {
            case "SOIL" -> {
                g.setColor(plot.watered() ? Palette.SOIL_WATERED : Palette.SOIL);
                g.fillRect(c.x, c.y, c.width, c.height);
                g.setColor(Palette.FURROW);
                for (int i = 1; i <= 2; i++) {
                    int y = c.y + c.height * i / 3;
                    g.drawLine(c.x + c.width / 8, y, c.x + c.width * 7 / 8, y);
                }
            }
            case "WATER" -> {
                g.setColor(Palette.WATER);
                g.fillRect(c.x, c.y, c.width, c.height);
                g.setColor(Palette.WAVE);
                int y = c.y + c.height / 2;
                g.drawArc(c.x + c.width / 6, y - c.height / 10, c.width / 3, c.height / 5, 0, 180);
                g.drawArc(c.x + c.width / 2, y - c.height / 10, c.width / 3, c.height / 5, 0, 180);
            }
            case "OBSTACLE" -> drawObstacleVector(g, c);
            case "PATH" -> {
                g.setColor(PATH);
                g.fillRect(c.x, c.y, c.width, c.height);
            }
            default -> {
                g.setColor(grass);
                g.fillRect(c.x, c.y, c.width, c.height);
            }
        }
    }

    private static void drawObstacleVector(Graphics2D g, Rectangle c) {
        g.setColor(Palette.OBSTACLE);
        g.fillRect(c.x, c.y, c.width, c.height);
        g.setColor(Palette.TRUNK);
        g.fillRect(c.x + c.width * 9 / 20, c.y + c.height / 2, c.width / 10, c.height * 2 / 5);
        g.setColor(Palette.LEAF);
        g.fillOval(c.x + c.width / 5, c.y + c.height / 8, c.width * 3 / 5, c.height * 3 / 5);
    }

    private void drawCrop(Graphics2D g, PlotView plot, Rectangle c, VisualEffects fx) {
        float scale = fx == null ? 1f : fx.cropScale(plot.col(), plot.row());
        AffineTransform saved = g.getTransform();
        if (scale < 1f) {
            double cx = c.x + c.width / 2.0;
            double cy = c.y + c.height / 2.0;
            g.translate(cx, cy);
            g.scale(scale, scale);
            g.translate(-cx, -cy);
        }
        Optional<BufferedImage> sprite = assets.sprite("crops/" + plot.crop() + "_" + plot.stage() + ".png");
        if (sprite.isPresent()) {
            g.drawImage(sprite.get(), c.x, c.y, c.width, c.height, null);
        } else {
            drawCropVector(g, plot, c);
        }
        g.setTransform(saved);
    }

    private void drawCropVector(Graphics2D g, PlotView plot, Rectangle c) {
        int s = c.width;
        int cx = c.x + s / 2;
        int cy = c.y + s / 2;
        g.setStroke(new BasicStroke(Math.max(1f, s / 24f)));
        switch (plot.stage()) {
            case "SEED" -> {
                g.setColor(Palette.SEED);
                int r = Math.max(2, Math.round(s * 0.08f));
                g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            }
            case "SPROUT" -> {
                g.setColor(Palette.LEAF);
                g.drawLine(cx, cy + s / 6, cx - Math.round(s * 0.12f), cy - s / 20);
                g.drawLine(cx, cy + s / 6, cx + Math.round(s * 0.12f), cy - s / 20);
            }
            case "GROWING" -> drawPlant(g, cx, cy, s, false, plot);
            case "MATURE" -> drawPlant(g, cx, cy, s, true, plot);
            default -> {
                g.setColor(Palette.WITHERED);
                int d = Math.round(s * 0.18f);
                g.drawLine(cx - d, cy - d, cx + d, cy + d);
                g.drawLine(cx - d, cy + d, cx + d, cy - d);
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawPlant(Graphics2D g, int cx, int cy, int s, boolean withFruit, PlotView plot) {
        int top = cy - Math.round(s * STEM);
        g.setColor(Palette.LEAF);
        g.drawLine(cx, cy + Math.round(s * STEM), cx, top);
        int lw = Math.round(s * LEAF_W);
        int lh = Math.round(s * LEAF_H);
        g.fillOval(cx - lw, cy - lh, lw, lh);
        g.fillOval(cx, cy, lw, lh);
        g.fillOval(cx - lw, cy + lh, lw, lh);
        if (withFruit) {
            int r = Math.round(s * FRUIT);
            g.setColor(Palette.crop(plot.crop()));
            g.fillOval(cx - r, top - r, 2 * r, 2 * r);
        }
    }
}
