package edu.ucc.farmsim.application.dto;

/**
 * One row of the construction dialog: a building, its price and whether it can be built.
 *
 * <p>Stories: HU-18.
 *
 * @param type        building id, for example {@code COOP}
 * @param name        Spanish name
 * @param width       columns it covers
 * @param height      rows it covers
 * @param cost        price in m
 * @param built       how many the player already has
 * @param max         the most the player can have
 * @param unlocked    whether the player has the achievement it requires
 * @param requirement Spanish name of the achievement that unlocks it, or {@code null} if none is needed
 */
public record BuildingOfferView(String type, String name, int width, int height, int cost, int built, int max,
                                boolean unlocked, String requirement) { }
