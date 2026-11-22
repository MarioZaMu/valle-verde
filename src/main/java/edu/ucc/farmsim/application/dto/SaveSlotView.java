package edu.ucc.farmsim.application.dto;

/**
 * One row of the slots list (RN-31): what the slot holds.
 *
 * <p>Stories: HU-29, HU-32.
 *
 * @param slot    slot number, from 1
 * @param status  {@code EMPTY}, {@code OK} or {@code DAMAGED}
 * @param year    year of the saved game, 0 if the slot is not usable
 * @param season  season id of the saved game, or {@code null}
 * @param day     day of the season of the saved game, 0 if the slot is not usable
 * @param money   money of the saved game
 * @param savedAt local date and time of the save as {@code yyyy-MM-dd HH:mm}, or {@code null}
 */
public record SaveSlotView(int slot, String status, int year, String season, int day, int money, String savedAt) { }
