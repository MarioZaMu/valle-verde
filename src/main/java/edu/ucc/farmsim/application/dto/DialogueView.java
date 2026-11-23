package edu.ucc.farmsim.application.dto;

/**
 * What a person of the valley says when the player talks to it (RN-37, RN-38). Immutable.
 *
 * <p>Stories: HU-46, HU-47.
 *
 * @param npcId      id of the person
 * @param name       its name in Spanish
 * @param role       what it does, in Spanish
 * @param text       the advice, which depends only on the state of the game
 * @param errand     the line about its errand: what it asks for, or its thanks once it is done
 * @param errandDone whether the errand was already delivered (then there is nothing to deliver)
 */
public record DialogueView(String npcId, String name, String role, String text, String errand,
                           boolean errandDone) { }
