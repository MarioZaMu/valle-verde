package edu.ucc.farmsim.application.dto;

/**
 * One achievement as the achievements dialog shows it.
 *
 * <p>Stories: HU-26.
 *
 * @param code        identifier of RN-28, for example {@code LG-02}
 * @param name        Spanish name
 * @param description Spanish text of the condition
 * @param reward      reward in m
 * @param unlocked    whether the player has it
 * @param progress    how far the player is (capped at {@code target})
 * @param target      what the condition asks for
 */
public record AchievementView(String code, String name, String description, int reward, boolean unlocked,
                              int progress, int target) { }
