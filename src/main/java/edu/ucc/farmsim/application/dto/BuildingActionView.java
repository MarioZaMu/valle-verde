package edu.ucc.farmsim.application.dto;

/**
 * The result of an action on a building (feed, collect, cure): the building as it is now, how many animals or
 * units the action reached, and an optional notice for the player when something was left undone.
 *
 * <p>Stories: HU-20, HU-22, HU-23.
 *
 * @param building the building after the action
 * @param amount   animals fed, units collected or animals cured
 * @param notice   Spanish text to show the player, or {@code null}
 */
public record BuildingActionView(BuildingView building, int amount, String notice) { }
