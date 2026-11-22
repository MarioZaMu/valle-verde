package edu.ucc.farmsim.ui.theme;

import java.awt.Color;

/**
 * The colors of the game drawn with Java2D. Every terrain and crop stage has its own color or shape so the map
 * never depends on color alone (RNF-14).
 *
 * <p>Stories: HU-35.
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
