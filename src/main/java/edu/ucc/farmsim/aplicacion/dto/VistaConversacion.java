package edu.ucc.farmsim.aplicacion.dto;

/**
 * Lo que dice una persona del valle cuando el jugador le habla (RN-37, RN-38). Inmutable.
 *
 * <p>Stories: HU-46, HU-47.
 *
 * @param npcId      id de la persona
 * @param name       su nombre en español
 * @param rol       lo que hace, en español
 * @param texto       el consejo, que depende solo del estado de la partida
 * @param encargo     la línea sobre su encargo: lo que pide, o su agradecimiento una vez hecho
 * @param encargoHecho si el encargo ya fue entregado (entonces no hay nada que entregar)
 */
public record VistaConversacion(String npcId, String name, String rol, String texto, String encargo,
                           boolean encargoHecho) { }
