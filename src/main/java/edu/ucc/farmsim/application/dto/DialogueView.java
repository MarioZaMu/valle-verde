package edu.ucc.farmsim.application.dto;

/**
 * Lo que dice una persona del valle cuando el jugador le habla (RN-37, RN-38). Inmutable.
 *
 * <p>Stories: HU-46, HU-47.
 *
 * @param npcId      id de la persona
 * @param name       su nombre en español
 * @param role       lo que hace, en español
 * @param text       el consejo, que depende solo del estado de la partida
 * @param errand     la línea sobre su encargo: lo que pide, o su agradecimiento una vez hecho
 * @param errandDone si el encargo ya fue entregado (entonces no hay nada que entregar)
 */
public record DialogueView(String npcId, String name, String role, String text, String errand,
                           boolean errandDone) { }
