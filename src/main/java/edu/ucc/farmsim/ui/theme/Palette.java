package edu.ucc.farmsim.ui.theme;

import java.awt.Color;

/**
 * The colors of the game drawn with Java2D. Every terrain and crop stage has its own color or shape so the map
 * never depends on color alone (RNF-14).
 *
 * <p>Stories: HU-35, HU-39, HU-43.
 */
public final class Palette {

    /** Behind the map. */
    public static final Color BACKGROUND = new Color(0x2B3A2B);
    /** Grass. */
    public static final Color GRASS = new Color(0x6AAE4B);
    /** Soil, dry. */
    public static final Color SOIL = new Color(0x8B5A2B);
    /** Soil darkened by watering. */
    public static final Color SOIL_WATERED = new Color(0x5C3A1A);
    /** Furrow lines on soil. */
    public static final Color FURROW = new Color(0x70471F);
    /** Water. */
    public static final Color WATER = new Color(0x3A7BD5);
    /** Wave lines on water. */
    public static final Color WAVE = new Color(0xA9CBF2);
    /** Obstacle ground (rock and trees). */
    public static final Color OBSTACLE = new Color(0x2F5D2F);
    /** Tree trunk. */
    public static final Color TRUNK = new Color(0x5A3A1E);
    /** Tree crown and plant leaves. */
    public static final Color LEAF = new Color(0x3FA34D);
    /** A seed. */
    public static final Color SEED = new Color(0x3B2410);
    /** A withered plant. */
    public static final Color WITHERED = new Color(0xA68B5B);
    /** Grid line between plots. */
    public static final Color GRID = new Color(0, 0, 0, 40);
    /** Border of the plot under the cursor. */
    public static final Color HOVER = new Color(0xFFFFFF);

    /** Wall of a coop. */
    public static final Color COOP_WALL = new Color(0xD9A066);

    /** Wall of a barn. */
    public static final Color BARN_WALL = new Color(0xB5473B);

    /** Wall of a warehouse. */
    public static final Color WAREHOUSE_WALL = new Color(0x9A8F7A);

    /** Frame and glass of a greenhouse. */
    public static final Color GREENHOUSE_GLASS = new Color(0xBDE8E0);

    /** Roof of every building. */
    public static final Color ROOF = new Color(0x5B2E1F);

    /** Outline of a building. */
    public static final Color BUILDING_OUTLINE = new Color(0x2A1A10);

    /** A healthy animal. */
    public static final Color ANIMAL_HEALTHY = new Color(0xFFFDF2);

    /** A hungry animal. */
    public static final Color ANIMAL_HUNGRY = new Color(0xF2C14E);

    /** A sick animal. */
    public static final Color ANIMAL_SICK = new Color(0x7FA36B);

    /** Marker of products waiting to be collected. */
    public static final Color PENDING = new Color(0xFFD23F);

    /** Text on a notice (HU-39, HU-43). */
    public static final Color TOAST_TEXT = new Color(0xFFFFFF);

    /** Background of an error notice. */
    public static final Color TOAST_ERROR = new Color(0xA33A32);

    /** Background of a success notice. */
    public static final Color TOAST_SUCCESS = new Color(0x2E7D4F);

    /** Background of an achievement notice. */
    public static final Color TOAST_ACHIEVEMENT = new Color(0x8A6A12);

    /** Background of an information notice. */
    public static final Color TOAST_INFO = new Color(0x2F4F6F);

    /** Text of the top bar. */
    public static final Color HUD_TEXT = new Color(0xFFF4D6);

    /** Background of the top bar. */
    public static final Color HUD_BACKGROUND = new Color(0x3B2F1E);

    /** Text of dialogs. */
    public static final Color DIALOG_TEXT = new Color(0x2B2118);

    /** Background of dialogs. */
    public static final Color DIALOG_BACKGROUND = new Color(0xFFF8E7);

    private Palette() {
        // constants holder
    }

    /** Fruit color of each crop, by crop id; unknown crops use a neutral color. */
    public static Color crop(String cropId) {
        return switch (cropId) {
            case "CARROT" -> new Color(0xF28C28);
            case "POTATO" -> new Color(0xC9A66B);
            case "TOMATO" -> new Color(0xD0312D);
            case "CORN" -> new Color(0xF2D43D);
            case "PUMPKIN" -> new Color(0xE8751A);
            default -> Color.GRAY;
        };
    }
}
